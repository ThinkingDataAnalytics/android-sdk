package cn.thinkingdata.android;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import cn.thinkingdata.android.utils.HttpService;
import cn.thinkingdata.android.utils.RemoteService;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;
import cn.thinkingdata.android.utils.TDUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * DataHandle 负责处理用户数据（事件、用户属性设置）的缓存和上报.
 *
 * 其工作依赖两个内部类 SendMessageWorker 和 SaveMessageWorker.
 */
public class DataHandle {

    private static final String TAG = "ThinkingAnalytics.DataHandle";
    static final String THREAD_NAME_SAVE_WORKER = "thinkingData.sdk.saveMessageWorker";
    static final String THREAD_NAME_SEND_WORKER = "thinkingData.sdk.sendMessageWorker";

    private final SendMessageWorker mSendMessageWorker;
    private final SaveMessageWorker mSaveMessageWorker;
    private final SystemInformation mSystemInformation;
    private final DatabaseAdapter mDbAdapter;
    private final Context mContext;

    private static final Map<Context, DataHandle> sInstances = new HashMap<>();

    /**
     * 获取给定 Context 的单例实例.
     * @param messageContext context
     * @return DataHandle 实例
     */
    static DataHandle getInstance(final Context messageContext) {
        synchronized (sInstances) {
            final Context appContext = messageContext.getApplicationContext();
            final DataHandle ret;
            if (!sInstances.containsKey(appContext)) {
                ret = new DataHandle(appContext);
                sInstances.put(appContext, ret);
            } else {
                ret = sInstances.get(appContext);
            }
            return ret;
        }
    }

    DataHandle(final Context context) {
        mContext = context.getApplicationContext();
        TDContextConfig config = TDContextConfig.getInstance(mContext);
        mSystemInformation = SystemInformation.getInstance(mContext);
        mDbAdapter = getDbAdapter(mContext);
        mDbAdapter.cleanupEvents(System.currentTimeMillis() - config.getDataExpiration(), DatabaseAdapter.Table.EVENTS);
        mSendMessageWorker = new SendMessageWorker();
        mSaveMessageWorker = new SaveMessageWorker();
    }

    // for auto tests.
    protected DatabaseAdapter getDbAdapter(Context context) {
        return DatabaseAdapter.getInstance(context);
    }

    // for auto tests.
    protected TDConfig getConfig(String token) {
        return TDConfig.getInstance(mContext, token);
    }

    /**
     * 保存数据到本地数据库
     */
    void saveClickData(final DataDescription dataDescription) {
        mSaveMessageWorker.saveToDatabase(dataDescription);
    }

    /**
     * 立即上报到服务器，不会缓存和重试
     */
    void postClickData(final DataDescription dataDescription) {
        mSendMessageWorker.postToServer(dataDescription);
    }

    /**
     * Debug 模式上报数据，逐条上报
     */
    void postToDebug(final DataDescription dataDescription) {
        mSendMessageWorker.postToDebug(dataDescription);
    }

    /**
     * 清空当前项目的队列，尝试上报到服务器. 如果缓存队列中有当前 token 的数据，会等待缓存数据入库后发起上报.
     * @param token APP ID
     */
    void flush(String token) {
        mSaveMessageWorker.triggerFlush(token);
    }

    /**
     * 谨慎调用此接口. 仅仅用于老版本兼容，将指定 APP ID 的本地缓存数据上报到服务器
     * @param token 项目 ID
     */
    void flushOldData(String token) {
        mSendMessageWorker.postOldDataToServer(token);
    }

    /**
     * 清空关于给定 token 的所有队列数据: 数据缓存、数据上报.
     * @param token 项目 ID
     */
    void emptyMessageQueue(String token) {
        mSaveMessageWorker.emptyQueue(token);
    }

    /**
     * 数据缓存队列, 主要处理缓存数据到本地数据库.
     */
    private class SaveMessageWorker {
        SaveMessageWorker() {
            final HandlerThread workerThread =
                    new HandlerThread(THREAD_NAME_SAVE_WORKER,
                            Thread.MIN_PRIORITY);
            workerThread.start();
            mHandler = new AnalyticsSaveMessageHandler(workerThread.getLooper());
        }

        void saveToDatabase(final DataDescription dataDescription) {
            final Message msg = Message.obtain();
            msg.what = ENQUEUE_EVENTS;
            msg.obj = dataDescription;
            if (null != mHandler) {
                mHandler.sendMessage(msg);
            }
        }

        void triggerFlush(String token) {
            Message msg = Message.obtain();
            msg.what = TRIGGER_FLUSH;
            msg.obj = token;
            mHandler.sendMessage(msg);
        }

        // 清空关于 token 的数据：包括未处理的消息和本地缓存
        void emptyQueue(String token) {
            final Message msg = Message.obtain();
            msg.what = EMPTY_QUEUE;
            msg.obj = token;
            if (null != mHandler) {
                mHandler.sendMessageAtFrontOfQueue(msg);
            }

            final Message msg1 = Message.obtain();
            msg1.what = EMPTY_QUEUE_END;
            msg1.obj = token;
            if (null != mHandler) {
                mHandler.sendMessage(msg1);
            }
        }

        private void checkSendStrategy(final String token, final int count) {
            if (count >= getFlushBulkSize(token)) {
                mSendMessageWorker.postToServer(token);
            } else {
                mSendMessageWorker.posterToServerDelayed(token, getFlushInterval(token));
            }
        }


        private class AnalyticsSaveMessageHandler extends Handler {

            AnalyticsSaveMessageHandler(Looper looper) {
                super(looper);
            }

            // 清空队列的时候保存待清空的项目 APP ID
            private final List<String> removingTokens = new ArrayList<>();

            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ENQUEUE_EVENTS) {
                    try {
                        int ret;
                        DataDescription dataDescription = (DataDescription) msg.obj;
                        if (null == dataDescription) return;
                        String token = dataDescription.mToken;
                        if (removingTokens.contains(token)) {
                            return;
                        }

                        JSONObject data = dataDescription.get();
                        try {
                            data.put(TDConstants.DATA_ID, UUID.randomUUID().toString());
                        } catch (JSONException e) {
                            // ignore
                        }
                        synchronized (mDbAdapter) {
                            ret = mDbAdapter.addJSON(data, DatabaseAdapter.Table.EVENTS, token);
                        }
                        if (ret < 0) {
                            TDLog.w(TAG, "Saving data to database failed.");
                        } else {
                            TDLog.i(TAG, "Data enqueued(" + TDUtils.getSuffix(token, 4) + "):\n" + data.toString(4));
                        }
                        checkSendStrategy(token, ret);
                    } catch (Exception e) {
                        TDLog.w(TAG, "Exception occurred while saving data to database: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (msg.what == EMPTY_QUEUE) {
                    String token = (String) msg.obj;
                    if (null == token) return;
                    // 发送队列停止上报该项目数据
                    mSendMessageWorker.emptyQueue(token);
                    synchronized (mHandler) {
                        mHandler.removeMessages(TRIGGER_FLUSH, token);
                        removingTokens.add(token);
                    }
                    synchronized (mDbAdapter) {
                        mDbAdapter.cleanupEvents(DatabaseAdapter.Table.EVENTS, (String) msg.obj);
                    }
                } else if (msg.what == TRIGGER_FLUSH) {
                    mSendMessageWorker.postToServer((String) msg.obj);
                } else if (msg.what == EMPTY_QUEUE_END) {
                    String token = (String) msg.obj;
                    removingTokens.remove(token);
                }
            }
        }

        private final Handler mHandler;
        private static final int ENQUEUE_EVENTS = 0; // push given JSON message to events DB
        private static final int EMPTY_QUEUE = 1; // empty events.
        private static final int TRIGGER_FLUSH = 2; // Trigger a flush.
        private static final int EMPTY_QUEUE_END = 3; // message that remove token from removingTokens.
    }

    protected int getFlushBulkSize(String token) {
        TDConfig config = getConfig(token);
        return null == config ? TDConfig.DEFAULT_FLUSH_BULK_SIZE : config.getFlushBulkSize();
    }

    protected int getFlushInterval(String token) {
        TDConfig config = getConfig(token);
        return null == config ? TDConfig.DEFAULT_FLUSH_INTERVAL : config.getFlushInterval();
    }

    protected RemoteService getPoster() {
        return new HttpService();
    }

    /**
     * 数据上报队列, 主要处理网络请求.
     */
    private class SendMessageWorker {

        SendMessageWorker() {
            final HandlerThread workerThread =
                    new HandlerThread(THREAD_NAME_SEND_WORKER,
                            Thread.MIN_PRIORITY);
            workerThread.start();
            mHandler = new AnalyticsMessageHandler(workerThread.getLooper());
            mPoster = getPoster();
            mDeviceInfo = new JSONObject(mSystemInformation.getDeviceInfo());
        }

        // 将 token 为空的数据发送到指定的 token 项目中; 只应在项目初始化时调用一次
        void postOldDataToServer(String token) {
            if (!TextUtils.isEmpty(token)) {
                Message msg = Message.obtain();
                msg.what = FLUSH_QUEUE_OLD;
                msg.obj = token;
                mHandler.sendMessage(msg);
            }
        }

        // 读取本地缓存中此 token 的数据并发送到网络
        void postToServer(String token) {
            synchronized (mHandlerLock) {
                if (mHandler == null) {
                    // We died under suspicious circumstances. Don't try to send any more events.
                } else {
                    if (!mHandler.hasMessages(FLUSH_QUEUE_PROCESSING, token)) {
                        Message msg = Message.obtain();
                        msg.what = FLUSH_QUEUE;
                        msg.obj = token;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }

        // 立即发送数据, 没有重试
        void postToServer(DataDescription dataDescription) {
            if (null == dataDescription) return;
            Message msg = Message.obtain();
            msg.what = SEND_TO_SERVER;
            msg.obj = dataDescription;
            mHandler.sendMessage(msg);
        }

        void postToDebug(DataDescription dataDescription) {
            if (null == dataDescription) return;
            Message msg = Message.obtain();
            msg.what = SEND_TO_DEBUG;
            msg.obj = dataDescription;
            mHandler.sendMessage(msg);
        }

        void emptyQueue(String token) {
            if (!TextUtils.isEmpty(token)) {
                Message msg = Message.obtain();
                msg.what = EMPTY_FLUSH_QUEUE;
                msg.obj = token;
                mHandler.sendMessageAtFrontOfQueue(msg);
            }
        }

        void posterToServerDelayed(final String token, final long delay) {
           synchronized (mHandlerLock) {
               if (mHandler == null) {
                   // We died under suspicious circumstances. Don't try to send any more events.
               } else {
                   if (!mHandler.hasMessages(FLUSH_QUEUE, token) && !mHandler.hasMessages(FLUSH_QUEUE_PROCESSING, token)) {
                       Message msg = Message.obtain();
                       msg.what = FLUSH_QUEUE;
                       msg.obj = token;
                       try {
                           mHandler.sendMessageDelayed(msg, delay);
                       } catch (IllegalStateException e) {
                           TDLog.w(TAG, "The app might be quiting: " + e.getMessage());
                       }
                   }
               }
           }
        }

        private class AnalyticsMessageHandler extends Handler {

            AnalyticsMessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case FLUSH_QUEUE: {
                        String token = (String) msg.obj;
                        final TDConfig config = getConfig(token);
                        if (null == config) {
                            TDLog.w(TAG, "Could found config object for token. Canceling...");
                            return;
                        }
                        synchronized (mHandlerLock) {
                            Message pmsg = Message.obtain();
                            pmsg.what = FLUSH_QUEUE_PROCESSING;
                            pmsg.obj = token;
                            mHandler.sendMessage(pmsg);
                            removeMessages(FLUSH_QUEUE, token);
                        }

                        try {
                            sendData(config);
                        } catch (final RuntimeException e) {
                            TDLog.w(TAG, "Sending data to server failed due to unexpected exception: " + e.getMessage());
                            e.printStackTrace();
                        }

                        synchronized (mHandlerLock) {
                            removeMessages(FLUSH_QUEUE_PROCESSING, token);
                            posterToServerDelayed(token, getFlushInterval(token));
                        }
                        break;
                    }
                    case FLUSH_QUEUE_OLD: {
                        final TDConfig config = getConfig((String) msg.obj);
                        if (null == config) {
                            TDLog.w(TAG, "Could found config object for token. Canceling...");
                            return;
                        }
                        try {
                            sendData("", config);
                        } catch (final RuntimeException e) {
                            TDLog.w(TAG, "Sending old data failed due to unexpected exception: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    }

                    case FLUSH_QUEUE_PROCESSING:
                        break;
                    case EMPTY_FLUSH_QUEUE: {
                        String token = (String) msg.obj;
                        if (null == token) return;
                        synchronized (mHandlerLock) {
                            removeMessages(FLUSH_QUEUE, msg.obj);
                        }
                        break;
                    }
                    case SEND_TO_SERVER:
                        try {
                            DataDescription dataDescription = (DataDescription) msg.obj;
                            if (null == dataDescription) return;

                            JSONObject data = dataDescription.get();
                            sendData(getConfig(dataDescription.mToken), data);
                        } catch (Exception e) {
                            TDLog.e(TAG, "Exception occurred while sending message to Server: " + e.getMessage());
                        }
                        break;
                    case SEND_TO_DEBUG: {
                        try {
                            DataDescription dataDescription = (DataDescription) msg.obj;
                            if (null == dataDescription) return;
                            TDConfig config = getConfig(dataDescription.mToken);
                            if (config.isNormal()) {
                                saveClickData(dataDescription);
                            } else {
                                try {
                                    JSONObject data = dataDescription.get();
                                    if (dataDescription.mType == TDConstants.DataType.TRACK) {
                                        JSONObject originalProperties = data.getJSONObject(TDConstants.KEY_PROPERTIES);
                                        JSONObject finalObject = new JSONObject();
                                        TDUtils.mergeJSONObject(mDeviceInfo, finalObject, config.getDefaultTimeZone());
                                        TDUtils.mergeJSONObject(originalProperties, finalObject, config.getDefaultTimeZone());
                                        data.put(TDConstants.KEY_PROPERTIES, finalObject);
                                        sendDebugData(config, data);
                                    } else {
                                        sendDebugData(config, data);
                                    }
                                } catch (Exception e) {
                                    TDLog.e(TAG, "Exception occurred while sending message to Server: " + e.getMessage());
                                    if (config.shouldThrowException()) {
                                        throw new TDDebugException(e);
                                    } else if (!config.isDebugOnly()) {
                                        saveClickData(dataDescription);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }

        // 发送单条数据到 Debug 模式
        private void sendDebugData(TDConfig config, JSONObject data) throws IOException, RemoteService.ServiceUnavailableException, JSONException {
            StringBuilder sb = new StringBuilder();
            sb.append("appid=");
            sb.append(config.mToken);
            sb.append("&deviceId=");
            sb.append(mDeviceInfo.getString(TDConstants.KEY_DEVICE_ID));
            sb.append("&source=client&data=");
            sb.append(URLEncoder.encode(data.toString()));
            if (config.isDebugOnly()) {
                sb.append("&dryRun=1");
            }

            String tokenSuffix = TDUtils.getSuffix(config.mToken, 4);
            TDLog.d(TAG, "uploading message(" + tokenSuffix + "):\n" + data.toString(4));

            String response = mPoster.performRequest(config.getDebugUrl(), sb.toString(), true, config.getSSLSocketFactory());

            JSONObject respObj = new JSONObject(response);

            int errorLevel = respObj.getInt("errorLevel");
            // 服务端设置回退到 normal 模式
            if (errorLevel == -1) {
                if (config.isDebugOnly()) {
                    // Just discard the data
                    TDLog.w(TAG, "The data will be discarded due to this device is not allowed to debug for: " + tokenSuffix);
                    return;
                }
                config.setMode(TDConfig.ModeEnum.NORMAL);
                throw new TDDebugException("Fallback to normal mode due to the device is not allowed to debug for: " + tokenSuffix);
            }

            // 提示用户 Debug 模式成功开启
            Boolean toastHasShown = mToastShown.get(config.mToken);
            if (toastHasShown == null || !toastHasShown) {
                Toast.makeText(mContext, "Debug Mode enabled for: " + tokenSuffix, Toast.LENGTH_LONG).show();
                mToastShown.put(config.mToken, true);
                config.setAllowDebug();
            }

            if (errorLevel != 0) {
                if (respObj.has("errorProperties")) {
                    JSONArray errProperties = respObj.getJSONArray("errorProperties");
                    TDLog.d(TAG, " Error Properties: \n" + errProperties.toString(4));
                }

                if (respObj.has("errorReasons")) {
                    JSONArray errReasons = respObj.getJSONArray("errorReasons");
                    TDLog.d(TAG, "Error Reasons: \n" + errReasons.toString(4));
                }

                if (config.shouldThrowException()) {
                    if (1 == errorLevel) {
                        throw new TDDebugException("Invalid properties. Please refer to the logcat log for detail info.");
                    } else if (2 == errorLevel) {
                        throw new TDDebugException("Invalid data format. Please refer to the logcat log for detail info.");
                    } else {
                        throw new TDDebugException("Unknown error level: " + errorLevel);
                    }
                }
            } else {
                TDLog.d(TAG, "Upload debug data successfully for " + tokenSuffix);
            }
        }

        // 发送单条数据到接收端
        private void sendData(TDConfig config, JSONObject data) throws IOException, RemoteService.ServiceUnavailableException, JSONException {
            if (TextUtils.isEmpty(config.mToken)) {
                return;
            }

            JSONArray dataArray = new JSONArray();
            dataArray.put(data);

            JSONObject dataObj = new JSONObject();
            dataObj.put(KEY_DATA, dataArray);
            dataObj.put(KEY_AUTOMATIC_DATA, mDeviceInfo);
            dataObj.put(KEY_APP_ID, config.mToken);

            String dataString = dataObj.toString();
            String response = mPoster.performRequest(config.getServerUrl(), dataString, false, config.getSSLSocketFactory());
            JSONObject responseJson = new JSONObject(response);
            String ret = responseJson.getString("code");
            TDLog.i(TAG, "ret code: " + ret + ", upload message:\n" + dataObj.toString(4));
        }

        private void sendData(TDConfig config) {
            sendData(config.mToken, config);
        }

        private void sendData(String fromToken, TDConfig config) {
            if (config == null) {
                TDLog.w(TAG, "Could found config object for sendToken. Canceling...");
                return;
            }

            if (TextUtils.isEmpty(config.mToken)) {
                return;
            }

            try {
                if (!mSystemInformation.isOnline()) {
                    return;
                }

                String networkType = mSystemInformation.getNetworkType();
                if (!config.isShouldFlush(networkType)) {
                    return;
                }
            } catch (Exception e) {
                // An exception occurred in network status checking, ignore this exception to continue sending data.
                e.printStackTrace();
            }

            int count;
            do {
                boolean deleteEvents = false;
                String[] eventsData;
                synchronized (mDbAdapter) {
                    eventsData = mDbAdapter.generateDataString(DatabaseAdapter.Table.EVENTS, fromToken, 50);
                }
                if (eventsData == null) {
                    return;
                }

                final String lastId = eventsData[0];
                final String clickData = eventsData[1];

                String errorMessage = null;
                try {
                    JSONArray myJsonArray;
                    try {
                        myJsonArray = new JSONArray(clickData);
                    } catch (JSONException e) {
                        TDLog.w(TAG, "The data is invalid: " + clickData);
                        throw e;
                    }

                    JSONObject dataObj = new JSONObject();
                    try {
                        dataObj.put(KEY_DATA, myJsonArray);
                        dataObj.put(KEY_AUTOMATIC_DATA, mDeviceInfo);
                        dataObj.put(KEY_APP_ID, config.mToken);
                    } catch (JSONException e) {
                        TDLog.w(TAG, "Invalid data: " + dataObj.toString());
                        throw e;
                    }

                    deleteEvents = true;
                    String dataString = dataObj.toString();
                    String response = mPoster.performRequest(config.getServerUrl(), dataString, false, config.getSSLSocketFactory());
                    JSONObject responseJson = new JSONObject(response);
                    String ret = responseJson.getString("code");
                    TDLog.i(TAG, "ret code: " + ret + ", upload message:\n" + dataObj.toString(4));
                } catch (final RemoteService.ServiceUnavailableException e) {
                    deleteEvents = false;
                    errorMessage = "Cannot post message to [" + config.getServerUrl() + "] due to " + e.getMessage();
                } catch (MalformedInputException e) {
                    errorMessage = "Cannot interpret " + config.getServerUrl() + " as a URL. The data will be deleted.";
                } catch (final IOException e) {
                    deleteEvents = false;
                    errorMessage = "Cannot post message to [" + config.getServerUrl() + "] due to " + e.getMessage();
                } catch (final JSONException e) {
                    deleteEvents = true;
                    errorMessage = "Cannot post message due to JSONException, the data will be deleted";
                } finally {
                    if (!TextUtils.isEmpty(errorMessage)) {
                        TDLog.d(TAG, errorMessage);
                    }

                    if (deleteEvents) {
                        synchronized (mDbAdapter) {
                            count = mDbAdapter.cleanupEvents(lastId, DatabaseAdapter.Table.EVENTS, fromToken);
                        }
                        TDLog.i(TAG, String.format(Locale.CHINA, "Events flushed. [left = %d]", count));
                    } else {
                        count = 0;
                    }
                }
            } while (count > 0);
        }

        private final Object mHandlerLock = new Object();
        private Handler mHandler;
        private static final int FLUSH_QUEUE = 0; // submit events to thinking data server.
        private static final int FLUSH_QUEUE_PROCESSING = 1; // ignore redundant messages.
        private static final int FLUSH_QUEUE_OLD = 2; // send old data if exists.
        private static final int EMPTY_FLUSH_QUEUE = 3; // empty the flush queue.
        private static final int SEND_TO_SERVER = 4; // send the data to server immediately.
        private static final int SEND_TO_DEBUG = 5; // send the data to debug receiver.
        private final RemoteService mPoster;
        private final JSONObject mDeviceInfo;
        private Map<String, Boolean> mToastShown = new HashMap<>();

        private static final String KEY_APP_ID = "#app_id";
        private static final String KEY_DATA = "data";
        private static final String KEY_AUTOMATIC_DATA = "automaticData";
    }
}
