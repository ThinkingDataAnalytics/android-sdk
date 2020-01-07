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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class TDConfig {
    public static final String VERSION = BuildConfig.TDSDK_VERSION;

    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static final String PREFERENCE_NAME_PREFIX = "cn.thinkingdata.android.config";

    static final int DEFAULT_FLUSH_INTERVAL = 15000; // 默认每 15 秒发起一次上报
    static final int DEFAULT_FLUSH_BULK_SIZE = 20; // 默认每次上报请求最多包含 20 条数据

    private static final Map<Context, Map<String, TDConfig>> sInstances = new HashMap<>();

    /**
     * 实例运行模式, 默认为 NORMAL 模式.
     */
    public enum ModeEnum {
        /* 正常模式，数据会存入缓存，并依据一定的缓存策略上报 */
        NORMAL,
        /* Debug 模式，数据逐条上报。当出现问题时会以日志和异常的方式提示用户 */
        DEBUG,
        /* Debug Only 模式，只对数据做校验，不会入库 */
        DEBUG_ONLY
    }

    private volatile ModeEnum mMode = ModeEnum.NORMAL;
    private volatile boolean mAllowedDebug;
    void setAllowDebug() {
        mAllowedDebug = true;
    }

    // for Unity
    public void setModeInt(int mode) {
        if (mode < 0 || mode > 2) {
            TDLog.d(TAG, "Invalid mode value");
            return;
        }

        mMode = ModeEnum.values()[mode];
    }

    // for Unity
    public int getModeInt() {
        return mMode.ordinal();
    }

    /**
     * 设置 SDK 运行模式
     * @param mode 运行模式
     */
    public void setMode(ModeEnum mode) {
        this.mMode = mode;
    }

    /**
     *  获取 SDK 当前运行模式
     * @return ModeEnum
     */
    public ModeEnum getMode() {
        return mMode;
    }

    // Internal use only. This method should be called after the instance was initialed.
    static TDConfig getInstance(Context context, String token) {
        try {
            return getInstance(context, token, "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取 TDConfig 实例. 该实例可以用于初始化 ThinkingAnalyticsSDK. 每个 SDK 实例对应一个 TDConfig 实例.
     * @param context app context
     * @param token APP ID, 创建项目时会给出.
     * @param url 数据接收端 URL, 必须是带协议的完整 URL，否则会抛异常
     * @return TDConfig 实例
     */
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
                URL serverUrl;

                try {
                    serverUrl = new URL(url);
                } catch (MalformedURLException e) {
                    TDLog.e(TAG, "Invalid server URL: " + url);
                    throw new IllegalArgumentException(e);
                }

                instance = new TDConfig(appContext, token, serverUrl.getProtocol()
                        + "://" + serverUrl.getHost()
                        + (serverUrl.getPort() > 0 ? ":" + serverUrl.getPort() : ""));
                instances.put(token, instance);
                instance.getRemoteConfig();
            }
            return instance;
        }
    }

    private TDConfig(Context context, String token, String serverUrl) {
        mContext = context.getApplicationContext();

        Future<SharedPreferences> storedSharedPrefs = sPrefsLoader.loadPreferences(
                mContext, PREFERENCE_NAME_PREFIX + "_" + token);
        mContextConfig = TDContextConfig.getInstance(mContext);

        mToken = token;
        mServerUrl = serverUrl + "/sync";
        mDebugUrl = serverUrl + "/data_debug";
        mConfigUrl = serverUrl + "/config?appid=" + token;

        mFlushInterval = new StorageFlushInterval(storedSharedPrefs, DEFAULT_FLUSH_INTERVAL);
        mFlushBulkSize = new StorageFlushBulkSize(storedSharedPrefs, DEFAULT_FLUSH_BULK_SIZE);
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

    private void getRemoteConfig() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                InputStream in = null;

                try {
                    URL url = new URL(mConfigUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    final SSLSocketFactory socketFactory = getSSLSocketFactory();
                    if (null != socketFactory && connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setSSLSocketFactory(socketFactory);
                    }
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
                                newUploadInterval = data.getInt("sync_interval") * 1000;
                                newUploadSize = data.getInt("sync_batch_size");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                            TDLog.d(TAG, "Fetched remote config for (" + mToken.substring(mToken.length() - 4)
                                    + ") newUploadInterval is " + newUploadInterval + ", newUploadSize is " + newUploadSize);

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
                        TDLog.d(TAG, "getConfig failed, responseCode is " + connection.getResponseCode());
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

    String getServerUrl() {
        return mServerUrl;
    }

    String getDebugUrl() {
        return mDebugUrl;
    }

    boolean isDebug() {
        return ModeEnum.DEBUG.equals(mMode);
    }

    boolean isDebugOnly() {
        return ModeEnum.DEBUG_ONLY.equals(mMode);
    }

    boolean isNormal() {
        return ModeEnum.NORMAL.equals(mMode);
    }

    boolean shouldThrowException() {
        return mAllowedDebug && (isDebug() || isDebugOnly());
    }

    /**
     * Flush interval, 单位毫秒
     * @return 上报间隔
     */
    int getFlushInterval() {
        return mFlushInterval.get();
    }

    int getFlushBulkSize() {
        return mFlushBulkSize.get();
    }

    String getMainProcessName() {
        return mContextConfig.getMainProcessName();
    }


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

    /**
     * 设置是否追踪老版本数据
     * @param trackOldData
     */
    public void setTrackOldData(boolean trackOldData) {
        mTrackOldData = trackOldData;
    }

    public boolean trackOldData() {
        return mTrackOldData;
    }

    public synchronized void setDefaultTimeZone(TimeZone timeZone) {
        mDefaultTimeZone = timeZone;
    }

    public synchronized TimeZone getDefaultTimeZone() {
        return mDefaultTimeZone == null ? TimeZone.getDefault() : mDefaultTimeZone;
    }

    /**
     * 设置自签证书. 自签证书对实例所有网络请求有效.
     * @param sslSocketFactory
     */
    public synchronized void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        if (null != sslSocketFactory) {
            mSSLSocketFactory = sslSocketFactory;
            getRemoteConfig();
        }
    }

    /**
     * 返回当前自签证书设置.
     * @return SSLSocketFactory
     */
    public synchronized SSLSocketFactory getSSLSocketFactory() {
        return mSSLSocketFactory;
    }

    // 兼容 1.2.0 之前老版本. 1.3.0 开始会在本地缓存中存放 app ID. 默认情况下会将之前遗留数据上报到第一个初始化的实例中.
    private volatile boolean mTrackOldData = true;

    // 同一个 Context 下所有实例共享的配置
    private final TDContextConfig mContextConfig;

    private final StorageFlushInterval mFlushInterval;
    private final StorageFlushBulkSize mFlushBulkSize;
    private final String mServerUrl;
    private final String mDebugUrl;
    private final String mConfigUrl;
    final String mToken;
    final Context mContext;

    private SSLSocketFactory mSSLSocketFactory;

    private TimeZone mDefaultTimeZone;

    private static final String TAG = "ThinkingAnalytics.TDConfig";
}