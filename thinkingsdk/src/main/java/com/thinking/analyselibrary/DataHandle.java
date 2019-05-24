package com.thinking.analyselibrary;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.thinking.analyselibrary.utils.Base64Coder;
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DataHandle {
    private final TDConfig mConfig;
    private final SendMessageWorker mSendMessageWorker;
    private final SaveMessageWorker mSaveMessageWorker;
    private final Context mContext;
    private final DatabaseAdapter mDbAdapter;
    private static final String TAG = "ThinkingAnalyticsSDK.DataHandle";
    private static final Map<Context, DataHandle> sInstances =
            new HashMap<>();
    private static boolean mUncaughtExceptionStatus = false;
    synchronized void setUncaughtExceptionStatus(boolean uncaughtExceptionStatus) {
        mUncaughtExceptionStatus = uncaughtExceptionStatus;
    }

    synchronized boolean getUncaughtExceptionStatus() {
        return mUncaughtExceptionStatus;
    }

    DataHandle(final Context context, final JSONObject deviceInfo) {
        mContext = context;
        mConfig = TDConfig.getInstance(context);
        mDbAdapter = DatabaseAdapter.getInstance(context);
        mDbAdapter.cleanupEvents(System.currentTimeMillis() - mConfig.getDataExpiration(), DatabaseAdapter.Table.EVENTS);
        mSendMessageWorker = new SendMessageWorker(deviceInfo);
        mSaveMessageWorker = new SaveMessageWorker();
    }

    public static DataHandle getInstance(final Context messageContext, final JSONObject deviceInfo) {
        synchronized (sInstances) {
            final Context appContext = messageContext.getApplicationContext();
            final DataHandle ret;
            if (!sInstances.containsKey(appContext)) {
                ret = new DataHandle(appContext, deviceInfo);
                sInstances.put(appContext, ret);
            } else {
                ret = sInstances.get(appContext);
            }
            return ret;
        }
    }

    static class DataDescription {
        private final JSONObject data;
        private final String token;
        public DataDescription(JSONObject data, String token) {
            this.data = data;
            this.token = token;
        }

        public String getToken() {return token;}
        public JSONObject getData() {return data;}
    }

    public void saveClickData(final JSONObject data, final String token) {
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

    public void flush() {
        mSendMessageWorker.postToServer(null);
    }

    // 指定 APP ID，上报数据到服务器。在完成上报之前，新的数据会暂缓存储
    // 此方法仅用于更换APP ID的特殊情况
    void flush(String token) {
        // TODO 加锁阻止新的事件上报
        mSendMessageWorker.postToServer(token);
    }

    private class SaveMessageWorker {
        public SaveMessageWorker() {
            final HandlerThread workerThread =
                    new HandlerThread("thinkingdata.sdk.savemessage",
                            Thread.MIN_PRIORITY);
            workerThread.start();
            mHandler = new AnalyticsSaveMessageHandler(workerThread.getLooper());
        }

        public void runSaveJob(final DataDescription dataDescription) {
            final Message msg = Message.obtain();
            msg.what = ENQUEUE_EVENTS;
            msg.obj = dataDescription;
            if (null != mHandler) {
                mHandler.sendMessage(msg);
            }
        }

        private class AnalyticsSaveMessageHandler extends Handler {

            public AnalyticsSaveMessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ENQUEUE_EVENTS:
                        try {
                            int ret;
                            DataDescription dataDescription = (DataDescription) msg.obj;
                            String token = dataDescription.getToken();
                            synchronized (mDbAdapter) {
                                ret = mDbAdapter.addJSON(dataDescription.getData(), DatabaseAdapter.Table.EVENTS,
                                        token);
                            }
                            if (ret < 0) {
                                TDLog.d(TAG,"failed to save data");
                            }
                            checkSendStrategy(token, ret);
                        } catch (Exception e) {
                            TDLog.d(TAG, "handleData error:" + e);
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }
        private Handler mHandler;
        private static final int ENQUEUE_EVENTS = 0; // push given JSON message to events DB
    }


    private class SendMessageWorker {
        public SendMessageWorker(final JSONObject deviceInfo) {
            final HandlerThread workerThread =
                    new HandlerThread("thinkingdata.sdk.sendmessage",
                            Thread.MIN_PRIORITY);
            workerThread.start();
            mHandler = new AnalyticsMessageHandler(workerThread.getLooper());
            mDeviceInfo = deviceInfo;
        }

        public void postToServer(String token) {
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

        public void posterToServerDelayed(final String token, final long delay) {
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

            public AnalyticsMessageHandler(Looper looper) {
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
                            e.printStackTrace();
                        }

                        // 异常情况，异常处理函数有可能在等待
                        if (getUncaughtExceptionStatus()) {
                            synchronized (ExceptionHandler.class) {
                                ExceptionHandler.class.notify();
                            }
                        }
                        synchronized (mHandlerLock) {
                            removeMessages(FLUSH_QUEUE_PROCESSING, token);
                            final int interval = mConfig.getFlushInterval();
                            posterToServerDelayed(token, interval);
                        }
                        break;
                    case FLUSH_QUEUE_PROCESSING:
                        TDLog.d(TAG, "test");
                        break;
                }
            }
        }

        private void sendData(String token) {
            try {
                if (!TDUtil.isNetworkAvailable(mContext)) {
                    return;
                }

                String networkType = TDUtil.networkType(mContext);
                if (!mConfig.isShouldFlush(networkType)) {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            int count = 100;
            while (count > 0) {
                boolean deleteEvents = false;
                InputStream in = null;
                OutputStream out = null;
                BufferedOutputStream bout = null;
                HttpURLConnection connection = null;
                String[] eventsData;
                synchronized (mDbAdapter) {
                    eventsData = mDbAdapter.generateDataString(DatabaseAdapter.Table.EVENTS, token, 50);
                }
                if (eventsData == null) {
                    return;
                }

                final String lastId = eventsData[0];
                final String clickData = eventsData[1];

                JSONArray myJsonArray = null;
                try {
                    myJsonArray = new JSONArray(clickData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JSONObject dataObj = new JSONObject();
                try {
                    dataObj.put("data", myJsonArray);
                    dataObj.put("automaticData", mDeviceInfo);
                    dataObj.put("#app_id", token);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String dataString = dataObj.toString();
                String errorMessage = null;

                try {
                    String data = null;
                    try {
                        data = encodeData(dataString);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        final URL url = new URL(mConfig.getServerUrl());
                        connection = (HttpURLConnection) url.openConnection();
                        String query = data;

                        connection.setFixedLengthStreamingMode(query.getBytes("UTF-8").length);
                        connection.setDoOutput(true);
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "text/plain");
                        out = connection.getOutputStream();
                        bout = new BufferedOutputStream(out);
                        bout.write(query.getBytes("UTF-8"));

                        bout.flush();
                        bout.close();
                        bout = null;
                        out.close();
                        out = null;

                        int responseCode = connection.getResponseCode();
                        TDLog.d(TAG, "ret_code:" + responseCode);
                        if (responseCode == 200) {
                            in = connection.getInputStream();
                            BufferedReader br = new BufferedReader(new InputStreamReader(in));
                            String str = null;
                            StringBuffer buffer = new StringBuffer();
                            while ((str = br.readLine()) != null) {
                                buffer.append(str);
                            }
                            in.close();
                            br.close();
                            JSONObject rjson = new JSONObject(buffer.toString());
                            String result = rjson.getString("code");

                            TDLog.d(TAG, "url ret:" + result);
                            if (result.equals("0")) {
                                deleteEvents = true;
                                TDLog.i(TAG, "upload message:" + dataObj);
                            } else {
                                deleteEvents = false;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                catch (Exception e) {
                    deleteEvents = false;
                    errorMessage = "Exception: " + e.getMessage();
                } finally {
                    if (!TextUtils.isEmpty(errorMessage)) {
                        TDLog.d(TAG, errorMessage);
                    }

                    if (deleteEvents) {
                        synchronized (mDbAdapter) {
                            count = mDbAdapter.cleanupEvents(lastId, DatabaseAdapter.Table.EVENTS, token);
                        }
                        TDLog.i(TAG, String.format(Locale.CHINA, "Events flushed. [left = %d]", count));
                    } else {
                        count = 0;
                    }
                    if (null != bout)
                        try {
                            bout.close();
                        } catch (final IOException e) {
                            TDLog.d(TAG, errorMessage);
                        }
                    if (null != out)
                        try {
                            out.close();
                        } catch (final IOException e) {
                            TDLog.d(TAG, errorMessage);
                        }
                    if (null != in)
                        try {
                            in.close();
                        } catch (final IOException e) {
                            TDLog.d(TAG, errorMessage);
                        }
                    if (null != connection)
                        connection.disconnect();
                }
            }
        }

        private String encodeData(final String rawMessage) throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes().length);
            GZIPOutputStream gos = new GZIPOutputStream(os);
            gos.write(rawMessage.getBytes());
            gos.close();
            byte[] compressed = os.toByteArray();
            os.close();
            return new String(Base64Coder.encode(compressed));
        }

        private final Object mHandlerLock = new Object();
        private Handler mHandler;
        private static final int FLUSH_QUEUE = 0; // submit events to thinkingdata server.
        private static final int FLUSH_QUEUE_PROCESSING = 1; // ignore redundent messages.
        private final JSONObject mDeviceInfo;
    }
}
