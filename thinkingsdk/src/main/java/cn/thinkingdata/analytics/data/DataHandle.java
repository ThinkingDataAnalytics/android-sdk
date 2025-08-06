/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.data;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

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
import java.util.concurrent.ConcurrentHashMap;

import cn.thinkingdata.analytics.TDConfig;
import cn.thinkingdata.analytics.TDPresetProperties;
import cn.thinkingdata.analytics.ThinkingAnalyticsSDK;
import cn.thinkingdata.analytics.encrypt.TDEncryptUtils;
import cn.thinkingdata.analytics.persistence.ConfigStoragePlugin;
import cn.thinkingdata.analytics.utils.HttpService;
import cn.thinkingdata.analytics.utils.RemoteService;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.analytics.utils.TDDebugException;
import cn.thinkingdata.analytics.utils.TDUtils;
import cn.thinkingdata.core.preset.TDPresetUtils;
import cn.thinkingdata.core.receiver.TDAnalyticsObservable;
import cn.thinkingdata.core.utils.TDLog;

/**
 * DataHandle handles the caching and reporting of user data (events and user property Settings).
 * Its work relies on two inner classes, SendMessageWorker and SaveMessageWorker.
 */
public class DataHandle {

    private static final String TAG = "ThinkingAnalytics.DataHandle";
    static final String THREAD_NAME_SAVE_WORKER = "thinkingData.sdk.saveMessageWorker";
    static final String THREAD_NAME_SEND_WORKER = "thinkingData.sdk.sendMessageWorker";

    private final SendMessageWorker mSendMessageWorker;
    private final SaveMessageWorker mSaveMessageWorker;
    private final DatabaseAdapter mDbAdapter;
    private final Context mContext;

    private static final Map<Context, DataHandle> sInstances = new HashMap<>();

    private final Map<String, Boolean> trackPauseMap = new ConcurrentHashMap<>();

    public static DataHandle getInstance(final Context messageContext) {
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
        //TDContextConfig config = TDContextConfig.getInstance(mContext);
        mDbAdapter = getDbAdapter(mContext);
        mSendMessageWorker = new SendMessageWorker();
        mSaveMessageWorker = new SaveMessageWorker();
        mSendMessageWorker.cleanupEvents();
    }

    // for auto tests.
    protected DatabaseAdapter getDbAdapter(Context context) {
        return DatabaseAdapter.getInstance(context);
    }

    // for auto tests.
    protected TDConfig getConfig(String token) {
        return TDConfig.getInstance(mContext, token);
    }

    public void handleTrackPauseToken(String token, boolean isEnable) {
        synchronized (trackPauseMap) {
            if (isEnable) {
                trackPauseMap.put(token, true);
            } else {
                trackPauseMap.remove(token);
            }
        }
    }

    public void saveClickData(final DataDescription dataDescription) {
        mSaveMessageWorker.saveToDatabase(dataDescription);
    }

    /**
     * Immediately reported to the server without caching and retry.
     */
    public void postClickData(final DataDescription dataDescription) {
        if (dataDescription.mIsSaveOnly) return;
        mSendMessageWorker.postToServer(dataDescription);
    }

    /**
     * Debug mode Reports data one by one.
     */
    public void postToDebug(final DataDescription dataDescription) {
        if (dataDescription.mIsSaveOnly) return;
        mSendMessageWorker.postToDebug(dataDescription);
    }

    /**
     * Clear the queue for the current item and attempt to report it to the server. If the cache queue contains the data of the current token, the cache queue is reported after the cached data is imported to the database.
     *
     * @param token APP ID
     */
    public void flush(String token) {
        mSaveMessageWorker.triggerFlush(token);
    }

    /**
     * Invoke this interface with caution. Report the local cache data of the specified APP ID to the server only for compatibility with older versions
     *
     * @param token App ID
     */
    public void flushOldData(String token) {
        mSendMessageWorker.postOldDataToServer(token);
    }

    /**
     * Clear all queue data for a given token: data cache and data report.
     *
     * @param token App ID
     */
    public void emptyMessageQueue(String token) {
        mSaveMessageWorker.emptyQueue(token);
    }

    /**
     * Data cache queue, which deals with caching data to the local database.
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

            private final List<String> removingTokens = new ArrayList<>();

            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ENQUEUE_EVENTS) {
                    try {
                        int ret;
                        DataDescription dataDescription = ( DataDescription ) msg.obj;
                        if (null == dataDescription) {
                            return;
                        }
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
                        TDConfig config = getConfig(token);
                        if (config != null) {
                            TDAnalyticsObservable.getInstance().onDataEnqueued(config.mToken, data);
                        }
                        if (ret < 0) {
                            TDLog.w(TAG, "Saving data to database failed.");
                        } else {
                            if(TDLog.mEnableLog) {
                                TDLog.i(TAG, "[ThinkingData] Info: Enqueue data("
                                        + TDUtils.getSuffix(token, 4)
                                        + "):\n" + data.toString(4));
                            }
                        }
                        if (!dataDescription.mIsSaveOnly) {
                            checkSendStrategy(token, ret);
                        }
                    } catch (Exception e) {
                        TDLog.w(TAG, "Exception occurred while saving data to database: "
                                + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (msg.what == EMPTY_QUEUE) {
                    String token = ( String ) msg.obj;
                    if (null == token) {
                        return;
                    }
                    mSendMessageWorker.emptyQueue(token);
                    synchronized (mHandler) {
                        mHandler.removeMessages(TRIGGER_FLUSH, token);
                        removingTokens.add(token);
                    }
                    synchronized (mDbAdapter) {
                        mDbAdapter.cleanupEvents(DatabaseAdapter.Table.EVENTS, ( String ) msg.obj);
                    }
                } else if (msg.what == TRIGGER_FLUSH) {
                    mSendMessageWorker.postToServer(( String ) msg.obj);
                } else if (msg.what == EMPTY_QUEUE_END) {
                    String token = ( String ) msg.obj;
                    removingTokens.remove(token);
                }
            }
        }

        private final Handler mHandler;
        private static final int ENQUEUE_EVENTS = 0; // push given JSON message to events DB
        private static final int EMPTY_QUEUE = 1; // empty events.
        private static final int TRIGGER_FLUSH = 2; // Trigger a flush.
        private static final int EMPTY_QUEUE_END = 3;
        // message that remove token from removingTokens.
    }

    protected int getFlushBulkSize(String token) {
        TDConfig config = getConfig(token);
        //return null == config ? TDConfig.DEFAULT_FLUSH_BULK_SIZE : config.getFlushBulkSize();
        return null == config ? ConfigStoragePlugin.DEFAULT_FLUSH_BULK_SIZE : config.getFlushBulkSize();
    }

    protected int getFlushInterval(String token) {
        TDConfig config = getConfig(token);
        //return null == config ? TDConfig.DEFAULT_FLUSH_INTERVAL : config.getFlushInterval();
        return null == config ? ConfigStoragePlugin.DEFAULT_FLUSH_INTERVAL : config.getFlushInterval();
    }

    protected RemoteService getPoster() {
        return new HttpService();
    }

    /**
     * Data is queued to process network requests.
     */
    private class SendMessageWorker {

        SendMessageWorker() {
            final HandlerThread workerThread =
                    new HandlerThread(THREAD_NAME_SEND_WORKER,
                            Thread.MIN_PRIORITY);
            workerThread.start();
            mHandler = new AnalyticsMessageHandler(workerThread.getLooper());
            mPoster = getPoster();
        }

        void cleanupEvents() {
            Message msg = Message.obtain();
            msg.what = CLEAN_EVENT;
            mHandler.sendMessage(msg);
        }

        //Sends null token data to the specified token item. It should only be called once when the project is initialized
        void postOldDataToServer(String token) {
            if (!TextUtils.isEmpty(token)) {
                Message msg = Message.obtain();
                msg.what = FLUSH_QUEUE_OLD;
                msg.obj = token;
                mHandler.sendMessage(msg);
            }
        }

        // Read the data of the token in the local cache and send it to the network
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

        // Send data immediately, no retry
        void postToServer(DataDescription dataDescription) {
            if (null == dataDescription) {
                return;
            }
            Message msg = Message.obtain();
            msg.what = SEND_TO_SERVER;
            msg.obj = dataDescription;
            if (!dataDescription.mIsSaveOnly) {
                mHandler.sendMessage(msg);
            }
        }

        void postToDebug(DataDescription dataDescription) {
            if (null == dataDescription) {
                return;
            }
            Message msg = Message.obtain();
            msg.what = SEND_TO_DEBUG;
            msg.obj = dataDescription;
            if (!dataDescription.mIsSaveOnly) {
                mHandler.sendMessage(msg);
            }
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
                if (mHandler != null) {

                    if (!mHandler.hasMessages(FLUSH_QUEUE, token)
                            && !mHandler.hasMessages(FLUSH_QUEUE_PROCESSING, token)) {
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
                        String token = ( String ) msg.obj;
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
                            TDLog.w(TAG, "Sending data to server failed "
                                    + "due to unexpected exception: " + e.getMessage());
                            e.printStackTrace();
                        }

                        synchronized (mHandlerLock) {
                            removeMessages(FLUSH_QUEUE_PROCESSING, token);
                            posterToServerDelayed(token, getFlushInterval(token));
                        }
                        break;
                    }
                    case FLUSH_QUEUE_OLD: {
                        final TDConfig config = getConfig(( String ) msg.obj);
                        if (null == config) {
                            TDLog.w(TAG, "Could found config object for token. Canceling...");
                            return;
                        }
                        try {
                            sendData("", config);
                        } catch (final RuntimeException e) {
                            TDLog.w(TAG, "Sending old data failed due to unexpected exception: "
                                    + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    }

                    case FLUSH_QUEUE_PROCESSING:
                        break;
                    case EMPTY_FLUSH_QUEUE: {
                        String token = ( String ) msg.obj;
                        if (null == token) {
                            return;
                        }
                        synchronized (mHandlerLock) {
                            removeMessages(FLUSH_QUEUE, msg.obj);
                        }
                        break;
                    }
                    case SEND_TO_SERVER:
                        try {
                            DataDescription dataDescription = ( DataDescription ) msg.obj;
                            if (null == dataDescription) {
                                return;
                            }
                            JSONObject data = dataDescription.get();
                            sendData(getConfig(dataDescription.mToken), data);
                        } catch (Exception e) {
                            TDLog.e(TAG,
                                    "Exception occurred while sending message to Server: "
                                            + e.getMessage());
                        }
                        break;
                    case SEND_TO_DEBUG: {
                        try {
                            DataDescription dataDescription = ( DataDescription ) msg.obj;
                            if (null == dataDescription) {
                                return;
                            }
                            TDConfig config = getConfig(dataDescription.mToken);
                            if (config.isNormal() && dataDescription.isTrackDebugType != 2) {
                                saveClickData(dataDescription);
                            } else {
                                try {
                                    JSONObject data = dataDescription.get();
                                    TDAnalyticsObservable.getInstance().onDataEnqueued(config.mToken, data);
                                    sendDebugData(config, data, dataDescription.isTrackDebugType);
                                    /*if (dataDescription.mType.isTrack()) {
                                        //JSONObject originalProperties
                                        //= data.getJSONObject(TDConstants.KEY_PROPERTIES);
                                        //JSONObject finalObject = new JSONObject();
                                        //TDUtils.mergeJSONObject(mDeviceInfo,
                                        //finalObject, config.getDefaultTimeZone());
                                        //TDUtils.mergeJSONObject(originalProperties,
                                        //finalObject, config.getDefaultTimeZone());
                                        //data.put(TDConstants.KEY_PROPERTIES, finalObject);
                                        sendDebugData(config, data);
                                    } else {
                                        sendDebugData(config, data);
                                    }*/
                                } catch (Exception e) {
                                    TDLog.e(TAG,
                                            "Exception occurred while sending message to Server: "
                                                    + e.getMessage());
                                    if (!config.isDebugOnly()) {
                                        saveClickData(dataDescription);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case CLEAN_EVENT: {
                        TDContextConfig config = TDContextConfig.getInstance(mContext);
                        synchronized (mDbAdapter) {
                            mDbAdapter.cleanupEvents(
                                    System.currentTimeMillis() - config.getDataExpiration(),
                                    DatabaseAdapter.Table.EVENTS);
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        private void sendDebugData(TDConfig config, JSONObject data, int isTrackDebugType)
                throws IOException, RemoteService.ServiceUnavailableException, JSONException {
            StringBuilder sb = new StringBuilder();
            sb.append("appid=");
            sb.append(config.mToken);
            JSONObject properties = data.optJSONObject(TDConstants.KEY_PROPERTIES);
            if (properties != null) {
                String deviceId = "";
                TDPresetProperties presetProperties
                        = ThinkingAnalyticsSDK.sharedInstance(config).getPresetProperties();
                if (presetProperties != null
                        && !TDPresetProperties.disableList.contains(TDPresetUtils.KEY_DEVICE_ID)) {
                    deviceId = presetProperties.deviceId;
                }
                if (TextUtils.isEmpty(deviceId)
                        && !TDPresetProperties.disableList.contains(TDPresetUtils.KEY_DEVICE_ID)) {
                    deviceId = SystemInformation
                            .getInstance(config.mContext).getDeviceId();
                }

                if (!TextUtils.isEmpty(deviceId)) {
                    sb.append("&deviceId=");
                    sb.append(deviceId);
                }
            }

            sb.append("&source=client&data=");
            sb.append(URLEncoder.encode(data.toString()));
            if (config.isDebugOnly() || isTrackDebugType == 2) {
                sb.append("&dryRun=1");
            }
            String tokenSuffix = TDUtils.getSuffix(config.getName(), 4);
            TDLog.i(TAG, "uploading message(" + tokenSuffix + "):\n" + data.toString(4));
            String response = mPoster.performRequest(config, sb.toString(), createExtraHeaders("1"));
            JSONObject respObj = new JSONObject(response);
            int errorLevel = respObj.getInt("errorLevel");
            TDLog.i(TAG, "[ThinkingData] Info: errorLevel=" + errorLevel);
            if (errorLevel == -1) {
                if (config.isDebugOnly()) {
                    // Just discard the data
                    TDLog.w(TAG, "The data will be discarded due to this device "
                            + "is not allowed to debug for: " + tokenSuffix);
                    return;
                }
                config.setMode(TDConfig.TDMode.NORMAL);
                throw new TDDebugException(
                        "Fallback to normal mode due to the device is not allowed to debug for: "
                                + tokenSuffix);
            }

            Boolean toastHasShown = mToastShown.get(config.getName());
            if (toastHasShown == null || !toastHasShown) {
//                Toast.makeText(mContext, "Debug Mode enabled for: "
//                        + tokenSuffix, Toast.LENGTH_LONG).show();
                mToastShown.put(config.getName(), true);
            }

            if (errorLevel != 0) {
                try {
                    if (respObj.has("errorProperties")) {
                        JSONArray errProperties = respObj.getJSONArray("errorProperties");
                        TDLog.d(TAG, " Error Properties: \n" + errProperties.toString(4));
                    }

                    if (respObj.has("errorReasons")) {
                        JSONArray errReasons = respObj.getJSONArray("errorReasons");
                        TDLog.d(TAG, "Error Reasons: \n" + errReasons.toString(4));
                    }
                } catch (Exception e) {

                }
//                if (config.shouldThrowException()) {
//                    if (1 == errorLevel) {
//                        throw new TDDebugException("Invalid properties. "
//                                + "Please refer to the logcat log for detail info.");
//                    } else if (2 == errorLevel) {
//                        throw new TDDebugException("Invalid data format. "
//                                + "Please refer to the logcat log for detail info.");
//                    } else {
//                        throw new TDDebugException("Unknown error level: " + errorLevel);
//                    }
//                }
            } else {
                TDLog.d(TAG, "Upload debug data successfully for " + tokenSuffix);
            }
        }

        private void sendData(TDConfig config, JSONObject data)
                throws IOException, RemoteService.ServiceUnavailableException, JSONException {
            if (TextUtils.isEmpty(config.mToken)) {
                return;
            }

            JSONArray dataArray = new JSONArray();
            dataArray.put(data);

            JSONObject dataObj = new JSONObject();
            dataObj.put(KEY_DATA, dataArray);
            dataObj.put(KEY_APP_ID, config.mToken);
            dataObj.put(KEY_FLUSH_TIME, System.currentTimeMillis());

            String dataString = dataObj.toString();


            String response = mPoster.performRequest(config, dataString, createExtraHeaders("1"));
            JSONObject responseJson = new JSONObject(response);
            String ret = responseJson.getString("code");
            TDLog.i(TAG, "ret code: " + ret + ", upload message:\n" + dataObj.toString(4));
        }

        private void sendData(TDConfig config) {
            sendData(config.getName(), config);
        }

        private void sendData(String fromToken, TDConfig config) {
            if (config == null) {
                TDLog.w(TAG, "Could found config object for sendToken. Canceling...");
                return;
            }

            if (TextUtils.isEmpty(config.mToken)) {
                return;
            }

            Boolean isTrackingPause;
            synchronized (trackPauseMap) {
                isTrackingPause = trackPauseMap.get(fromToken);
            }
            if (null != isTrackingPause && isTrackingPause) {
                return;
            }

            try {
                if (!SystemInformation.getInstance(mContext).isOnline()) {
                    return;
                }

                String networkType = SystemInformation.getInstance(mContext).getCurrentNetworkType();
                if (!config.isShouldFlush(networkType)) {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            int count;
            do {
                boolean deleteEvents = false;
                String[] eventsData;
                synchronized (mDbAdapter) {
                    eventsData = mDbAdapter
                            .generateDataString(DatabaseAdapter.Table.EVENTS, fromToken, 50);
                }
                if (eventsData == null) {
                    return;
                }

                final String lastId = eventsData[0];
                final String clickData = eventsData[1];

                String errorMessage = null;
                String postData = "";
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
                        dataObj.put(KEY_APP_ID, config.mToken);
                        dataObj.put(KEY_FLUSH_TIME, System.currentTimeMillis());
                    } catch (JSONException e) {
                        TDLog.w(TAG, "Invalid data: " + dataObj.toString());
                        throw e;
                    }

                    deleteEvents = true;
                    String dataString = dataObj.toString();
                    postData = dataString;
                    String response = mPoster.performRequest(config, dataString, createExtraHeaders(myJsonArray));

                    JSONObject responseJson = new JSONObject(response);
                    String ret = responseJson.getString("code");
                    if (!TextUtils.equals(ret, "0")) {
                        handleSDKError(config.getName(), TDConstants.SDK_ERROR_NET, "server code is:" + ret, postData);
                    }
                    if (TDLog.mEnableLog) {
                        TDLog.i(TAG, "[ThinkingData] Debug: Send event, Request = " + dataObj.toString(4));
                        TDLog.i(TAG, "[ThinkingData] Debug: Send event, Response =" + responseJson.toString(4));
                    }
                } catch (final RemoteService.ServiceUnavailableException e) {
                    deleteEvents = false;
                    errorMessage = "Cannot post message to ["
                            + config.getServerUrl() + "] due to " + e.getMessage();
                } catch (MalformedInputException e) {
                    errorMessage = "Cannot interpret "
                            + config.getServerUrl() + " as a URL. The data will be deleted.";
                } catch (final IOException e) {
                    deleteEvents = false;
                    errorMessage = "Cannot post message to ["
                            + config.getServerUrl() + "] due to " + e.getMessage();
                } catch (final JSONException e) {
                    deleteEvents = true;
                    errorMessage
                            = "Cannot post message due to JSONException, the data will be deleted";
                } finally {

                    if (!TextUtils.isEmpty(errorMessage)) {
                        TDLog.e(TAG, errorMessage);
                        handleSDKError(config.getName(),TDConstants.SDK_ERROR_NET,errorMessage,postData);
                    }

                    if (deleteEvents) {
                        synchronized (mDbAdapter) {
                            count = mDbAdapter
                                    .cleanupEvents(lastId, DatabaseAdapter.Table.EVENTS, fromToken);
                        }
                        TDLog.i(TAG, String.format(Locale.CHINA,
                                "Events flushed. [left = %d]", count));
                    } else {
                        count = 0;
                    }
                }
            } while (count > 0);
        }

        private void handleSDKError(String appId, int code, String errorMsg, String ext) {
            if (null != mContext && !TextUtils.isEmpty(appId)) {
                ThinkingAnalyticsSDK.ThinkingSDKErrorCallback callback = ThinkingAnalyticsSDK.sharedInstance(mContext, appId).getSDKErrorCallback();
                if (null != callback) {
                    callback.onSDKErrorCallback(code, errorMsg, ext);
                }
            }
        }

        private Map<String, String> createExtraHeaders(String count) {
            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put(INTEGRATION_TYPE, SystemInformation.getLibName());
            extraHeaders.put(INTEGRATION_VERSION, SystemInformation.getLibVersion());
            extraHeaders.put(INTEGRATION_COUNT, count);
            extraHeaders.put(INTEGRATION_EXTRA, "Android");
            return extraHeaders;
        }

        private Map<String, String> createExtraHeaders(JSONArray array) {
            Map<String, String> extraHeaders = new HashMap<>();
            extraHeaders.put(INTEGRATION_TYPE, SystemInformation.getLibName());
            extraHeaders.put(INTEGRATION_VERSION, SystemInformation.getLibVersion());
            extraHeaders.put(INTEGRATION_COUNT, String.valueOf(array.length()));
            extraHeaders.put(INTEGRATION_EXTRA, "Android");
            extraHeaders
                    .put(INTEGRATION_ENCRYPT, TDEncryptUtils.hasEncryptedData(array) ? "1" : "0");
            return extraHeaders;
        }

        private final Object mHandlerLock = new Object();
        private final Handler mHandler;
        private static final int FLUSH_QUEUE = 0; // submit events to thinking data server.
        private static final int FLUSH_QUEUE_PROCESSING = 1; // ignore redundant messages.
        private static final int FLUSH_QUEUE_OLD = 2; // send old data if exists.
        private static final int EMPTY_FLUSH_QUEUE = 3; // empty the flush queue.
        private static final int SEND_TO_SERVER = 4; // send the data to server immediately.
        private static final int SEND_TO_DEBUG = 5; // send the data to debug receiver.
        private static final int CLEAN_EVENT = 6; // clean event before time
        private final RemoteService mPoster;
        private final Map<String, Boolean> mToastShown = new HashMap<>();

        private static final String KEY_APP_ID = "#app_id";
        private static final String KEY_DATA = "data";
        private static final String KEY_FLUSH_TIME = "#flush_time";
        private static final String KEY_AUTOMATIC_DATA = "automaticData";

        private static final String INTEGRATION_TYPE = "TA-Integration-Type";
        private static final String INTEGRATION_VERSION = "TA-Integration-Version";
        private static final String INTEGRATION_COUNT = "TA-Integration-Count";
        private static final String INTEGRATION_EXTRA = "TA-Integration-Extra";
        private static final String INTEGRATION_ENCRYPT = "TA-Datas-Type";
    }
}
