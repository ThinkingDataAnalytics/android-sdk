package cn.thinkingdata.android;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import cn.thinkingdata.android.utils.HttpService;
import cn.thinkingdata.android.utils.RemoteService;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.HashMap;
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
    private static final String KEY_DATA_STRING = "dataString";

    private final SendMessageWorker mSendMessageWorker;
    private final SaveMessageWorker mSaveMessageWorker;
    private final SystemInformation mSystemInformation;
    private final DatabaseAdapter mDbAdapter;
    private final TDConfig mConfig;

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
        Context appContext = context.getApplicationContext();
        mConfig = getConfig(appContext);
        mSystemInformation = SystemInformation.getInstance(appContext);
        mDbAdapter = getDbAdapter(appContext);
        mDbAdapter.cleanupEvents(System.currentTimeMillis() - mConfig.getDataExpiration(), DatabaseAdapter.Table.EVENTS);
        mSendMessageWorker = new SendMessageWorker();
        mSaveMessageWorker = new SaveMessageWorker();
    }

    // for auto tests.
    protected DatabaseAdapter getDbAdapter(Context context) {
        return DatabaseAdapter.getInstance(context);
    }

    // for auto tests.
    protected TDConfig getConfig(Context context) {
        return TDConfig.getInstance(context);
    }


    /**
     * 保存数据到本地数据库
     * @param data JSON 数据，包括事件数据和用户属性数据
     * @param token APP ID
     */
    void saveClickData(final JSONObject data, final String token) {
        mSaveMessageWorker.saveToDatabase(data, token);
    }

    /**
     * 立即上报到服务器，不会缓存和重试
     * @param data JSON 数据，包括事件数据和用户属性数据
     * @param token APP ID
     */
    void postClickData(final JSONObject data, final String token) {
        mSendMessageWorker.postToServer(data, token);
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

        void saveToDatabase(final JSONObject data, String token) {
            final Message msg = Message.obtain();
            msg.what = ENQUEUE_EVENTS;
            msg.obj = token;
            Bundle bundle = new Bundle();
            bundle.putString(KEY_DATA_STRING, data.toString());
            msg.setData(bundle);
            if (null != mHandler) {
                mHandler.sendMessage(msg);
            }
        }

        void triggerFlush(String token) {
            if (mHandler.hasMessages(ENQUEUE_EVENTS, token)) {
                Message msg = Message.obtain();
                msg.what = TRIGGER_FLUSH;
                msg.obj = token;
                mHandler.sendMessage(msg);
            } else {
                mSendMessageWorker.postToServer(token);
            }
        }

        // 清空关于 token 的数据：包括未处理的消息和本地缓存
        void emptyQueue(String token) {
            final Message msg = Message.obtain();
            msg.what = EMPTY_QUEUE;
            msg.obj = token;
            if (null != mHandler) {
                mHandler.sendMessageAtFrontOfQueue(msg);
            }
        }

        private void checkSendStrategy(final String token, final int count) {
            if (count > mConfig.getFlushBulkSize()) {
                mSendMessageWorker.postToServer(token);
            } else {
                final int interval = mConfig.getFlushInterval();
                mSendMessageWorker.posterToServerDelayed(token, interval);
            }
        }


        private class AnalyticsSaveMessageHandler extends Handler {

            AnalyticsSaveMessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ENQUEUE_EVENTS) {
                    try {
                        int ret;
                        String token = (String) msg.obj;
                        String dataString = msg.getData().getString(KEY_DATA_STRING);
                        if (null == dataString) return;

                        JSONObject data = new JSONObject(dataString);
                        try {
                            data.put(TDConstants.DATA_ID, UUID.randomUUID().toString());
                        } catch (JSONException e) {
                            // ignore
                        }
                        synchronized (mDbAdapter) {
                            ret = mDbAdapter.addJSON(data, DatabaseAdapter.Table.EVENTS, token);
                        }
                        if (ret < 0) {
                            TDLog.w(TAG, "Save data to database failed.");
                        } else {
                            TDLog.i(TAG, "Data enqueued(" + token + "):\n" + data.toString(4));
                        }
                        checkSendStrategy(token, ret);
                    } catch (Exception e) {
                        TDLog.w(TAG, "handleData error: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (msg.what == EMPTY_QUEUE) {
                    String token = (String) msg.obj;
                    mSendMessageWorker.emptyQueue(token);
                    synchronized (mHandler) {
                        mHandler.removeMessages(TRIGGER_FLUSH, token);
                        mHandler.removeMessages(ENQUEUE_EVENTS, token);
                    }
                    synchronized (mDbAdapter) {
                        mDbAdapter.cleanupEvents(DatabaseAdapter.Table.EVENTS, (String) msg.obj);
                    }
                } else if (msg.what == TRIGGER_FLUSH) {
                    mSendMessageWorker.postToServer((String) msg.obj);
                }
            }
        }

        private final Handler mHandler;
        private static final int ENQUEUE_EVENTS = 0; // push given JSON message to events DB
        private static final int EMPTY_QUEUE = 1; // empty events.
        private static final int TRIGGER_FLUSH = 2; // Trigger a flush.
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
        void postToServer(final JSONObject data, String token) {
            if (null == data) return;
            Message msg = Message.obtain();
            msg.what = SEND_TO_SERVER;
            msg.obj = token;
            Bundle bundle = new Bundle();
            bundle.putString(KEY_DATA_STRING, data.toString());
            msg.setData(bundle);
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
                       mHandler.sendMessageDelayed(msg, delay);
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
                String token = (String) msg.obj;
                switch (msg.what) {
                    case FLUSH_QUEUE:
                        synchronized (mHandlerLock) {
                            Message pmsg = Message.obtain();
                            pmsg.what = FLUSH_QUEUE_PROCESSING;
                            pmsg.obj = token;
                            mHandler.sendMessage(pmsg);
                            removeMessages(FLUSH_QUEUE, token);
                        }

                        try {
                            sendData(token);
                        } catch (final RuntimeException e) {
                            TDLog.w(TAG, "Send data to server failed due to unexpected exception: " + e.getMessage());
                            e.printStackTrace();
                        }

                        synchronized (mHandlerLock) {
                            removeMessages(FLUSH_QUEUE_PROCESSING, token);
                            final int interval = mConfig.getFlushInterval();
                            posterToServerDelayed(token, interval);
                        }
                        break;
                    case FLUSH_QUEUE_OLD:
                        try {
                            sendData("", (String) msg.obj);
                        } catch (final RuntimeException e) {
                            TDLog.w(TAG, "Send old data failed due to unexpected exception: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;

                    case FLUSH_QUEUE_PROCESSING:
                        break;
                    case EMPTY_FLUSH_QUEUE:
                        synchronized (mHandlerLock) {
                            removeMessages(FLUSH_QUEUE, token);
                        }
                        break;
                    case SEND_TO_SERVER:
                        try {
                            String dataString = msg.getData().getString(KEY_DATA_STRING);
                            if (null == dataString) return;

                            JSONObject data = new JSONObject(dataString);
                            sendData(token, data);
                        } catch (Exception e) {
                           e.printStackTrace();
                        }
                        break;

                }
            }
        }

        private void sendData(String token, JSONObject data) throws IOException, RemoteService.ServiceUnavailableException, JSONException {
            if (TextUtils.isEmpty(token)) {
                return;
            }

            JSONArray dataArray = new JSONArray();
            dataArray.put(data);

            JSONObject dataObj = new JSONObject();
            dataObj.put(KEY_DATA, dataArray);
            dataObj.put(KEY_AUTOMATIC_DATA, mDeviceInfo);
            dataObj.put(KEY_APP_ID, token);

            String dataString = dataObj.toString();
            String response = mPoster.performRequest(mConfig.getServerUrl(), dataString);
            JSONObject responseJson = new JSONObject(response);
            String ret = responseJson.getString("code");
            TDLog.i(TAG, "ret code: " + ret + ", upload message:\n" + dataObj.toString(4));
        }

        private void sendData(String token) {
            sendData(token, token);
        }

        private void sendData(String fromToken, String sendToken) {
            if (TextUtils.isEmpty(sendToken)) {
                return;
            }

            try {
                if (!mSystemInformation.isOnline()) {
                    return;
                }

                String networkType = mSystemInformation.getNetworkType();
                if (!mConfig.isShouldFlush(networkType)) {
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
                        dataObj.put(KEY_APP_ID, sendToken);
                    } catch (JSONException e) {
                        TDLog.w(TAG, "Invalid data: " + dataObj.toString());
                        throw e;
                    }

                    deleteEvents = true;
                    String dataString = dataObj.toString();
                    String response = mPoster.performRequest(mConfig.getServerUrl(), dataString);
                    JSONObject responseJson = new JSONObject(response);
                    String ret = responseJson.getString("code");
                    TDLog.i(TAG, "ret code: " + ret + ", upload message:\n" + dataObj.toString(4));
                } catch (final RemoteService.ServiceUnavailableException e) {
                    deleteEvents = false;
                    errorMessage = "Cannot post message to " + mConfig.getServerUrl();
                } catch (MalformedInputException e) {
                    errorMessage = "Cannot interpret " + mConfig.getServerUrl() + " as a URL. The data will be deleted.";
                } catch (final IOException e) {
                    deleteEvents = false;
                    errorMessage = "Cannot post message to " + mConfig.getServerUrl();
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
        private final RemoteService mPoster;
        private final JSONObject mDeviceInfo;

        private static final String KEY_APP_ID = "#app_id";
        private static final String KEY_DATA = "data";
        private static final String KEY_AUTOMATIC_DATA = "automaticData";
    }
}
