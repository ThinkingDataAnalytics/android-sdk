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
    private final Worker mWorker;
    private final Context mContext;
    private final DatabaseManager mDbAdapter;
    private static final String TAG = "ThinkingAnalyticsSDK.DataHandle";
    private static final Map<Context, DataHandle> sInstances =
            new HashMap<Context, DataHandle>();

    private static final int ENQUEUE_EVENTS = 0; // push given JSON message to events DB
    private static final int FLUSH_QUEUE = 1; // submit events to thinkingdata server.

    DataHandle(final Context context, final String packageName) {
        mContext = context;
        mDbAdapter = new DatabaseManager(mContext, packageName/*dbName*/);
        mWorker = new Worker();
    }

    public static DataHandle getInstance(final Context messageContext, final String packageName) {
        synchronized (sInstances) {
            final Context appContext = messageContext.getApplicationContext();
            final DataHandle ret;
            if (!sInstances.containsKey(appContext)) {
                ret = new DataHandle(appContext, packageName);
                sInstances.put(appContext, ret);
            } else {
                ret = sInstances.get(appContext);
            }
            return ret;
        }
    }

    public void saveClickData(final JSONObject eventJson) {
        final Message m = Message.obtain();
        m.what = ENQUEUE_EVENTS;
        m.obj = eventJson;
        mWorker.runMessage(m);
    }

    private void checkSendStrategy(int count) {
        final Message msg = Message.obtain();
        msg.what = FLUSH_QUEUE;
        if (count > ThinkingAnalyticsSDK.sharedInstance(mContext).getFlushBulkSize()) {
            mWorker.runMessage(msg);
        } else {
            final int interval = ThinkingAnalyticsSDK.sharedInstance(mContext).getFlushInterval();
            mWorker.runMessageOnce(msg, interval);
        }
    }

    public void flush() {
        final Message msg = Message.obtain();
        msg.what = FLUSH_QUEUE;
        mWorker.runMessage(msg);
    }

    private class Worker {
        public Worker() {
            final HandlerThread workerThread =
                    new HandlerThread("thinkingdata.sdk.sendmessage",
                            Thread.MIN_PRIORITY);
            workerThread.start();
            mHandler = new AnalyticsMessageHandler(workerThread.getLooper());
        }

        public void runMessage(Message msg) {
            synchronized (mHandlerLock) {
                if (mHandler == null) {

                } else {
                    mHandler.sendMessage(msg);
                }
            }
        }

        public void runMessageOnce(Message msg, long delay) {
            synchronized (mHandlerLock) {
                if (mHandler == null) {
                    // We died under suspicious circumstances. Don't try to send any more events.
                } else {
                    if (!mHandler.hasMessages(msg.what)) {
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
                    case ENQUEUE_EVENTS:
                        try {
                            int ret = mDbAdapter.addJSON((JSONObject)msg.obj, DatabaseManager.Table.EVENTS);
                            if (ret < 0) {
                                TDLog.d(TAG,"failed to save data");
                            }
                            checkSendStrategy(ret);
                        } catch (Exception e) {
                            TDLog.d(TAG, "handleData error:" + e);
                            e.printStackTrace();
                        }
                        break;
                    case FLUSH_QUEUE:
                        try {
                            sendData();
                        } catch (final RuntimeException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }

        private final Object mHandlerLock = new Object();
        private Handler mHandler;
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

    private void sendData() {
        try {
            if (!TDUtil.isNetworkAvailable(mContext)) {
                return;
            }

            String networkType = TDUtil.networkType(mContext);
            if (!ThinkingAnalyticsSDK.sharedInstance(mContext).isShouldFlush(networkType)) {
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
                eventsData = mDbAdapter.generateDataString(DatabaseManager.Table.EVENTS, 50);
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

            JSONObject sendProperties = new JSONObject(ThinkingAnalyticsSDK.sharedInstance(mContext).getDeviceInfo());
            JSONObject dataObj = new JSONObject();
            try {
                dataObj.put("data", myJsonArray);
                dataObj.put("automaticData", sendProperties);
                dataObj.put("#app_id", ThinkingAnalyticsSDK.sharedInstance(mContext).getAppid());
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

                    final URL url = new URL(ThinkingAnalyticsSDK.sharedInstance(mContext).getServerUrl());
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
                    count = mDbAdapter.cleanupEvents(lastId, DatabaseManager.Table.EVENTS);
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
}
