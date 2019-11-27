package cn.thinkingdata.android;

import android.content.Context;
import android.content.SharedPreferences;

import cn.thinkingdata.android.persistence.StorageFlushBulkSize;
import cn.thinkingdata.android.persistence.StorageFlushInterval;
import cn.thinkingdata.android.utils.TDLog;

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
import java.util.concurrent.Future;

public class TDConfig {
    public static final String VERSION = BuildConfig.TDSDK_VERSION;

    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static final String PREFERENCE_NAME_PREFIX = "cn.thinkingdata.android.config";

    private static final Map<Context, Map<String, TDConfig>> sInstances = new HashMap<>();

    // This method should be called after the instance was initialed.
    static TDConfig getInstance(Context context, String token) {
        return getInstance(context, token, "");
    }

    public static TDConfig getInstance(Context context, String token, String url) {
        Context appContext = context.getApplicationContext();

        synchronized (sInstances) {
            Map<String, TDConfig> instances = sInstances.get(appContext);
            if (null == instances) {
                instances = new HashMap<>();
                sInstances.put(appContext, instances);
            }

            TDConfig instance = instances.get(token);
            if (null == instance) {
                instance = new TDConfig(appContext, token, url + "/sync");
                instances.put(token, instance);
                instance.getRemoteConfig(url + "/config?appid=" + token);
            }
            return instance;
        }
    }

    TDConfig(Context context, String token, String serverUrl) {
        if (null == serverUrl) {
            TDLog.w(TAG, "The server url is null, it cannot be used to post data");
        }

        Future<SharedPreferences> storedSharedPrefs = sPrefsLoader.loadPreferences(
                context, PREFERENCE_NAME_PREFIX + "_" + token);

        mServerUrl = serverUrl;
        mContextConfig = TDContextConfig.getInstance(context);

        mFlushInterval = new StorageFlushInterval(storedSharedPrefs);
        mFlushBulkSize = new StorageFlushBulkSize(storedSharedPrefs);
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

                            int newUploadInterval = mFlushInterval.get();
                            int newUploadSize = mFlushBulkSize.get();
                            try {
                                JSONObject data = rjson.getJSONObject("data");
                                // FIXME
                                TDLog.i(TAG, "config url is: " + configureUrl);
                                TDLog.i(TAG, data.toString(4));
                                newUploadInterval = data.getInt("sync_interval") * 1000;
                                newUploadSize = data.getInt("sync_batch_size");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                            TDLog.d(TAG, "newUploadInterval is " + newUploadInterval + ", newUploadSize is " + newUploadSize);

                            if (mFlushBulkSize.get() != newUploadSize) {
                                mFlushBulkSize.put(newUploadSize);
                            }

                            if (mFlushInterval.get() != newUploadInterval) {
                                mFlushInterval.put(newUploadInterval);
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

    /**
     * Flush interval, 单位毫秒
     * @return
     */
    public int getFlushInterval() {
        return mFlushInterval.get();
    }

    public int getFlushBulkSize() {
        return mFlushBulkSize.get();
    }


    public boolean getAutoTrackConfig() {
        return mContextConfig.getAutoTrackConfig();
    }

    public String getMainProcessName() {
        return mContextConfig.getMainProcessName();
    }

    private final StorageFlushInterval mFlushInterval;
    private final StorageFlushBulkSize mFlushBulkSize;
    private final String mServerUrl;

    private static final String TAG = "ThinkingAnalytics.TDConfig";

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

    // 同一个 Context 下所有实例共享的配置
    private final TDContextConfig mContextConfig;
}