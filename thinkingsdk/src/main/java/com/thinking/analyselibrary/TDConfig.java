package com.thinking.analyselibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.thinking.analyselibrary.utils.TDLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TDConfig {
    public static final String VERSION = BuildConfig.TDSDK_VERSION;

    private final static Map<Context, TDConfig> sInstanceMap = new HashMap<>();

    static TDConfig getInstance(Context context) {
        return sInstanceMap.get(context);

    }

    static TDConfig getInstance(Context context, String url, String token) {
        TDConfig instance;
        synchronized (sInstanceMap) {
            instance = sInstanceMap.get(context);
            if (null == instance) {
                instance = new TDConfig(context, url + "/sync");
                sInstanceMap.put(context, instance);
                instance.getRemoteConfig(url + "/config?appid=" + token);
            }
        }
        return instance;
    }

    TDConfig(Context context, String serverUrl) {
        // 获取数据上传的触发条件，默认为间隔15秒，或者数据达到20条
        mContext = context;
        mFlushInterval = getUploadInterval();
        mFlushBulkSize = getUploadSize();
        mServerUrl = serverUrl;
    }


    synchronized boolean isShouldFlush(String networkType) {
        return (convertToNetworkType(networkType) & mNetworkType) != 0;
    }

    private int convertToNetworkType(String networkType) {
        if ("NULL".equals(networkType)) {
            return NetworkType.TYPE_ALL;
        } else if ("WIFI".equals(networkType)) {
            return NetworkType.TYPE_WIFI;
        } else if ("2G".equals(networkType)) {
            return NetworkType.TYPE_2G;
        } else if ("3G".equals(networkType)) {
            return NetworkType.TYPE_3G;
        } else if ("4G".equals(networkType)) {
            return NetworkType.TYPE_4G;
        } else if ("5G".equals(networkType)) {
            return NetworkType.TYPE_5G;
        }
        return NetworkType.TYPE_ALL;
    }


    private void getRemoteConfig(final String configureUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                InputStream in = null;

                try {
                    URL url = new URL(configureUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    if (200 == connection.getResponseCode()) {
                        in = connection.getInputStream();

                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        StringBuffer buffer = new StringBuffer();
                        String line;
                        while((line = br.readLine())!=null) {
                            buffer.append(line);
                        }
                        JSONObject rjson = new JSONObject(buffer.toString());

                        if (rjson.getString("code").equals("0")) {

                            int newUploadInterval = mFlushInterval;
                            int newUploadSize = mFlushBulkSize;
                            try {
                                JSONObject data = rjson.getJSONObject("data");
                                newUploadInterval = data.getInt("sync_interval") * 1000;
                                newUploadSize = data.getInt("sync_batch_size");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                            TDLog.d(TAG, "newUploadInterval is " + newUploadInterval + ", newUploadSize is " + newUploadSize);

                            if (mFlushBulkSize != newUploadSize || mFlushInterval != newUploadInterval) {
                                setUploadInterval(newUploadInterval);
                                setUploadSize(newUploadSize);

                                synchronized (lock) {
                                    mFlushInterval = newUploadInterval;
                                    mFlushBulkSize = newUploadSize;
                                }
                            }
                        }

                        in.close();
                        br.close();
                    } else {
                        TDLog.d(TAG, "getConfig faild, responseCode is " + connection.getResponseCode());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (null != in) {
                        try {
                            in.close();
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (null != connection) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    public String getServerUrl() {
        return mServerUrl;
    }

    public int getFlushInterval() {
        synchronized (lock) {
            return mFlushInterval;
        }
    }

    public int getFlushBulkSize() {
        synchronized (lock) {
            return mFlushBulkSize;
        }
    }

    public int getMinimumDatabaseLimit() {
        return mMinimumDatabaseLimit;
    }

    // Throw away records that are older than this in milliseconds. Should be below the server side age limit for events.
    public long getDataExpiration() {
        return mDataExpiration;
    }

    public void setMinimumDatabaseLimit(int limit) {
        mMinimumDatabaseLimit = limit;

    }

    public void setDataExpiration(int hours) {
        mDataExpiration = 1000 * 60 * 60 * hours;
    }

    private long mDataExpiration = 1000 * 60 * 60 * 24 * 15; // 5 days default

    private int mFlushInterval;
    private int mFlushBulkSize;
    private final String mServerUrl;
    private Context mContext;
    private final Object lock = new Object();
    private int mMinimumDatabaseLimit = 32 * 1024 * 1024;  // 32 M default


    private static final String TAG = "ThinkingAnalyticsSDK.TDConfig";

    /* 默认触发上传时间间隔，单位毫秒 */
    final static private int DEFAULT_UPLOAD_INTERVAL = 15000;
    /* 默认触发上传数据条数 */
    final static private int DEFAULT_DATA_UPLOAD_SIZE = 20;

    private static final String PREF_DATA_UPLOADINTERVAL = "thinkingdata_uploadinterval";
    private static final String PREF_DATA_UPLOADSIZE = "thinkingdata_uploadsize";

    synchronized void setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType type) {
        switch (type) {
            case NETWORKTYPE_DEFAULT:
                mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_5G | NetworkType.TYPE_WIFI;
                break;
            case NETWORKTYPE_WIFI:
                mNetworkType = NetworkType.TYPE_WIFI;
                break;
            case NETWORKTYPE_ALL:
                mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_5G | NetworkType.TYPE_WIFI | NetworkType.TYPE_2G;
                break;
        }
    }

    private final class NetworkType {
        public static final int TYPE_2G = 1; //2G
        public static final int TYPE_3G = 1 << 1; //3G
        public static final int TYPE_4G = 1 << 2; //4G
        public static final int TYPE_WIFI = 1 << 3; //WIFI
        public static final int TYPE_5G = 1 << 4; // 5G
        public static final int TYPE_ALL = 0xFF; //ALL
    }
    private int mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_5G | NetworkType.TYPE_WIFI;

    synchronized private void setUploadInterval(final int newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.edit().putInt(PREF_DATA_UPLOADINTERVAL, newValue).apply();
    }

    synchronized private int getUploadInterval() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getInt(PREF_DATA_UPLOADINTERVAL, DEFAULT_UPLOAD_INTERVAL);
    }

    synchronized private void setUploadSize(final int newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.edit().putInt(PREF_DATA_UPLOADSIZE, newValue).apply();
    }

    synchronized private int getUploadSize() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getInt(PREF_DATA_UPLOADSIZE, DEFAULT_DATA_UPLOAD_SIZE);
    }
}