package com.thinking.analyselibrary;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.thinking.analyselibrary.utils.HttpService;
import com.thinking.analyselibrary.utils.RemoteService;
import com.thinking.analyselibrary.utils.TDConstants;
import com.thinking.analyselibrary.utils.TDLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DataHandle {
    private final TDConfig mConfig;
    static final String THREAD_NAME_SAVE_WORKER = "thinkingdata.sdk.savemessage";
    static final String THREAD_NAME_SEND_WORKER = "thinkingdata.sdk.sendmessage";
    private final SendMessageWorker mSendMessageWorker;
    private final SaveMessageWorker mSaveMessageWorker;
    private final DatabaseAdapter mDbAdapter;
    private final SystemInformation mSystemInformation;
    private static final String TAG = "ThinkingAnalytics.DataHandle";
    private static final Map<Context, DataHandle> sInstances =
            new HashMap<>();

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

    protected DatabaseAdapter getDbAdapter(Context context) {
        return DatabaseAdapter.getInstance(context);
    }

    protected TDConfig getConfig(Context context) {
        return TDConfig.getInstance(context);
    }

    static class DataDescription {
        private final JSONObject data;
        private final String token;
        DataDescription(JSONObject data, String token) {
            this.data = data;
            this.token = token;
        }

        public String getToken() {return token;}
        public JSONObject getData() {return data;}
    }

    void saveClickData(final JSONObject data, final String token) {
        mSaveMessageWorker.runSaveJob(new DataDescription(data, token));
    }

    private void checkSendStrategy(final String token, final int count) {
        if (count > mConfig.getFlushBulkSize()) {
            mSendMessageWorker.postToServer(token);
        } else {
            final int interval = mConfig.getFlushInterval();
            mSendMessageWorker.posterToServerDelayed(token, interval);
        }
    }

    void flush(String token) {
        mSendMessageWorker.postToServer(token);
    }

    void flushOldData(String token) {
        mSendMessageWorker.postOldDataToServer(token);
    }


    private class SaveMessageWorker {
        SaveMessageWorker() {
            final HandlerThread workerThread =
                    new HandlerThread(THREAD_NAME_SAVE_WORKER,
                            Thread.MIN_PRIORITY);
            workerThread.start();
            mHandler = new AnalyticsSaveMessageHandler(workerThread.getLooper());
        }

        void runSaveJob(final DataDescription dataDescription) {
            final Message msg = Message.obtain();
            msg.what = ENQUEUE_EVENTS;
            msg.obj = dataDescription;
            if (null != mHandler) {
                mHandler.sendMessage(msg);
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
                        DataDescription dataDescription = (DataDescription) msg.obj;
                        String token = dataDescription.getToken();
                        JSONObject data = dataDescription.getData();
                        try {
                            data.put(TDConstants.DATA_ID, UUID.randomUUID().toString());
                        } catch (JSONException e) {
                            // ignore
                        }
                        synchronized (mDbAdapter) {
                            ret = mDbAdapter.addJSON(data, DatabaseAdapter.Table.EVENTS, token);
                        }
                        if (ret < 0) {
                            TDLog.w(TAG, "Failed to save data.");
                        } else {
                            TDLog.i(TAG, "Data enqueued(" + token + "):\n" + data.toString(4));
                        }
                        checkSendStrategy(token, ret);
                    } catch (Exception e) {
                        TDLog.w(TAG, "handleData error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        private Handler mHandler;
        private static final int ENQUEUE_EVENTS = 0; // push given JSON message to events DB
    }

    protected RemoteService getPoster() {
        return new HttpService();
    }

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
                switch (msg.what) {
                    case FLUSH_QUEUE:
                        String token = (String) msg.obj;

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
                }
            }
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
        private static final int FLUSH_QUEUE = 0; // submit events to thinkingdata server.
        private static final int FLUSH_QUEUE_PROCESSING = 1; // ignore redundent messages.
        private static final int FLUSH_QUEUE_OLD = 2; // send old data if exists.
        private final RemoteService mPoster;
        private final JSONObject mDeviceInfo;

        private static final String KEY_APP_ID = "#app_id";
        private static final String KEY_DATA = "data";
        private static final String KEY_AUTOMATIC_DATA = "automaticData";
    }
}
