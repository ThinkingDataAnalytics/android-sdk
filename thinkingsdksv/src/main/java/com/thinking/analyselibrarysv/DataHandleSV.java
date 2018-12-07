package com.thinking.analyselibrarysv;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DataHandleSV {
    private final SendMessage mWorker;
    private final Context mContext;
    private final DatabaseManagerSV mDbAdapter;
    private static final String TAG = "ThinkingAnalyticsSDKSV";
    private static final Map<Context, DataHandleSV> sInstances =
            new HashMap<Context, DataHandleSV>();

    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"
            + ".SSS", Locale.CHINA);

    DataHandleSV(final Context context, final String packageName) {
        mContext = context;
        mDbAdapter = new DatabaseManagerSV(mContext, packageName/*dbName*/);
        mWorker = new SendMessage();
    }

    public static DataHandleSV getInstance(final Context messageContext, final String packageName) {
        synchronized (sInstances) {
            final Context appContext = messageContext.getApplicationContext();
            final DataHandleSV ret;
            if (!sInstances.containsKey(appContext)) {
                ret = new DataHandleSV(appContext, packageName);
                sInstances.put(appContext, ret);
            }else {
                ret = sInstances.get(appContext);
            }
            return ret;
        }
    }

    public void saveClickData(final JSONObject eventJson) {
        try {
            synchronized (mDbAdapter) {
                int ret = mDbAdapter.addJSON(eventJson, DatabaseManagerSV.Table.EVENTSSV);
                if (ret < 0) {
                    TDLogSV.d(TAG,"failed save data");
                }

                final Message m = Message.obtain();

                if (ret > ThinkingAnalyticsSDKSV.sharedInstance(mContext)
                        .getFlushBulkSize()) {
                    mWorker.runMessage(m);
                }
                else {
                    final int interval = ThinkingAnalyticsSDKSV.sharedInstance(mContext).getFlushInterval();
                    mWorker.runMessageOnce(m, interval);
                }

            }
        } catch (Exception e) {
            TDLogSV.d(TAG, "handleData error:" + e);
            e.printStackTrace();
        }
    }

    private class SendMessage {
        public SendMessage() {
            final HandlerThread workerThread =
                    new HandlerThread("thinkingdata.sv.sdk.sendmessage",
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
                try {
                    sendData();
                } catch (final RuntimeException e) {
                    e.printStackTrace();
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
        return new String(Base64CoderSV.encode(compressed));
    }

    public static void mergeJSONObject(final JSONObject source, JSONObject dest)
            throws JSONException {
        Iterator<String> superPropertiesIterator = source.keys();
        while (superPropertiesIterator.hasNext()) {
            String key = superPropertiesIterator.next();
            Object value = source.get(key);
            if (value instanceof Date) {
                synchronized (mDateFormat) {
                    dest.put(key, mDateFormat.format((Date) value));
                }
            } else {
                dest.put(key, value);
            }
        }
    }

    public void sendData() {
        try {
            if (!TDUtilSV.isNetworkAvailable(mContext)) {
                return;
            }

            String networkType = TDUtilSV.networkType(mContext);
            if (!ThinkingAnalyticsSDKSV.sharedInstance(mContext).isShouldFlush(networkType)) {
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
                eventsData = mDbAdapter.generateDataString(DatabaseManagerSV.Table.EVENTSSV, 50);
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

            JSONObject sendProperties = new JSONObject(ThinkingAnalyticsSDKSV.sharedInstance(mContext).getDeviceInfo());
            JSONObject dataObj = new JSONObject();
            try {
                dataObj.put("data", myJsonArray);
                dataObj.put("automaticData", sendProperties);
                dataObj.put("#app_id", ThinkingAnalyticsSDKSV.sharedInstance(mContext).getAppid());
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

                    final URL url = new URL(ThinkingAnalyticsSDKSV.sharedInstance(mContext).getServerUrl());
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
                    TDLogSV.i(TAG, "ret_code:" + responseCode);
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

                        TDLogSV.i(TAG, "url ret:" + result);
                        if (result.equals("0")) {
                            deleteEvents = true;
                            TDLogSV.i(TAG, "upload message:" + dataObj);
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
                    TDLogSV.d(TAG, errorMessage);
                }

                if (deleteEvents) {
                    count = mDbAdapter.cleanupEvents(lastId, DatabaseManagerSV.Table.EVENTSSV);
                    TDLogSV.i(TAG, String.format(Locale.CHINA, "Events flushed. [left = %d]", count));
                } else {
                    count = 0;
                }
                if (null != bout)
                    try {
                        bout.close();
                    } catch (final IOException e) {
                        TDLogSV.d(TAG, errorMessage);
                    }
                if (null != out)
                    try {
                        out.close();
                    } catch (final IOException e) {
                        TDLogSV.d(TAG, errorMessage);
                    }
                if (null != in)
                    try {
                        in.close();
                    } catch (final IOException e) {
                        TDLogSV.d(TAG, errorMessage);
                    }
                if (null != connection)
                    connection.disconnect();
            }
        }
    }

    public void flush() {
        final Message m = Message.obtain();
        mWorker.runMessage(m);
    }
}
