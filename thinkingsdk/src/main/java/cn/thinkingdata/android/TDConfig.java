/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import android.content.Context;
import android.text.TextUtils;
import cn.thinkingdata.android.encrypt.TDSecreteKey;
import cn.thinkingdata.android.persistence.ConfigStoragePlugin;
import cn.thinkingdata.android.persistence.LocalStorageType;
import cn.thinkingdata.android.utils.CalibratedTimeManager;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;
import cn.thinkingdata.android.utils.TDUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * sdk config.
 * */
public class TDConfig {
    public static final String VERSION = BuildConfig.VERSION_NAME;

    private static final Map<Context, Map<String, TDConfig>> sInstances = new HashMap<>();

    private final Set<String> mDisabledEvents = new HashSet<>();
    private final ReadWriteLock mDisabledEventsLock = new ReentrantReadWriteLock();

    private final ConfigStoragePlugin mConfigStoragePlugin;

    /**
     * Set the current instance name.
     *
     * @param name instance name
     */
    private void setName(String name) {
        this.name = name;
    }

    /**
     * Get the instance name.
     *
     * @return String instance name
     * */
    public String getName() {
        return name;
    }

    /**
     * Running mode. The default mode is NORMAL.
     */
    public enum ModeEnum {
        /*  In normal mode, data is cached and reported according to certain cache policies */
        NORMAL,
        /* Debug mode: Data is reported one by one. When a problem occurs, the user is alerted in the form of logs and exceptions */
        DEBUG,
        /* Debug Only mode: verifies data and does not store data in the database */
        DEBUG_ONLY
    }

    /**
     * Whether the event has been disabled. It can be set after TA version 2.7.
     *
     * @param eventName event name
     * @return true event is disabled
     */
    boolean isDisabledEvent(String eventName) {
        mDisabledEventsLock.readLock().lock();
        try {
            return mDisabledEvents.contains(eventName);
        } finally {
            mDisabledEventsLock.readLock().unlock();
        }
    }

    /**
     * Specifies whether data reporting by multiple processes is supported. By default, data reporting by multiple processes is disabled
     * Data reporting by multiple processes has certain performance loss, and cross-process communication is a relatively slow process.
     *
     * @param isSupportMultiProcess multiple processes is supported
     */
    public TDConfig setMutiprocess(boolean isSupportMultiProcess) {
        mEnableMutiprocess = isSupportMultiProcess;
        return this;
    }

    public boolean isEnableMutiprocess() {
        return  mEnableMutiprocess;
    }

    private volatile ModeEnum mMode = ModeEnum.NORMAL;
    private volatile boolean mAllowedDebug;
    private volatile String name;

    void setAllowDebug() {
        mAllowedDebug = true;
    }

    /**
     * for test.
     * */
    Map<String, TDConfig> getTDConfigMap() {
        return sInstances.get(mContext);
    }

    /**
     * Set the SDK running mode.
     *
     * @param mode running mode
     */
    public TDConfig setMode(ModeEnum mode) {
        this.mMode = mode;
        return this;
    }

    /**
     *  Obtain the current running mode of the SDK.
     *
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
     * Obtain the TDConfig instance. This example can be used to initialize ThinkingAnalyticsSDK. Each SDK instance corresponds to one TDConfig instance.
     * @param context app context
     * @param token APP ID
     * @param url  The URL of the data receiving end must be the complete URL with the protocol; otherwise, an exception will be thrown
     * @return TDConfig instance
     */
    public static TDConfig getInstance(Context context, String token, String url) {
        return getInstance(context, token, url, token);
    }

    /**
     * Obtain the TDConfig instance. This example can be used to initialize ThinkingAnalyticsSDK. Each SDK instance corresponds to one TDConfig instance.
     * @param context app context
     * @param token APP ID
     * @param url The URL of the data receiving end must be the complete URL with the protocol; otherwise, an exception will be thrown
     * @param name instance name
     * @return TDConfig instance
     */
    public static TDConfig getInstance(Context context, String token, String url, String name) {
        Context appContext = context.getApplicationContext();
        synchronized (sInstances) {
            Map<String, TDConfig> instances = sInstances.get(appContext);
            if (null == instances) {
                instances = new HashMap<>();
                sInstances.put(appContext, instances);
            }

            token = token.replace(" ", "");
            name = name.replace(" ", "");
            TDConfig instance = instances.get(name);
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
                instance.setName(name);
                instances.put(name, instance);
                instance.getRemoteConfig();
            }
            return instance;
        }
    }

    private TDConfig(Context context, String token, String serverUrl) {
        mContext = context.getApplicationContext();
        mContextConfig = TDContextConfig.getInstance(mContext);

        mToken = token;
        mServerUrl = serverUrl + "/sync";
        mDebugUrl = serverUrl + "/data_debug";
        mConfigUrl = serverUrl + "/config?appid=" + token;

        mConfigStoragePlugin = new ConfigStoragePlugin(mContext,token);
        mEnableMutiprocess = false;
    }

    synchronized boolean isShouldFlush(String networkType) {
        return (TDUtils.convertToNetworkType(networkType) & mNetworkType) != 0;
    }

    private void getRemoteConfig() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long t1 = System.currentTimeMillis();
                HttpURLConnection connection = null;
                InputStream in = null;

                try {
                    URL url = new URL(mConfigUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    final SSLSocketFactory socketFactory = getSSLSocketFactory();
                    if (null != socketFactory && connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setSSLSocketFactory(socketFactory);
                    }
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(20000);
                    connection.setRequestMethod("GET");

                    if (200 == connection.getResponseCode()) {
                        in = connection.getInputStream();

                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        StringBuffer buffer = new StringBuffer();
                        String line;
                        while ((line = br.readLine()) != null) {
                            buffer.append(line);
                        }
                        JSONObject rjson = new JSONObject(buffer.toString());

                        if (rjson.getString("code").equals("0")) {
                            int newUploadInterval = mConfigStoragePlugin.get(LocalStorageType.FLUSH_INTERVAL);
                            int newUploadSize = mConfigStoragePlugin.get(LocalStorageType.FLUSH_SIZE);
                            try {
                                JSONObject data = rjson.getJSONObject("data");
                                newUploadInterval = data.getInt("sync_interval") * 1000;
                                newUploadSize = data.getInt("sync_batch_size");
                                if (data.has("secret_key")) {
                                    JSONObject secretJson = data.getJSONObject("secret_key");
                                    if (secretJson.has("key") && secretJson.has("version") && secretJson.has("symmetric") && secretJson.has("asymmetric")) {
                                        String key = secretJson.getString("key");
                                        int version = secretJson.getInt("version");
                                        String symmetric = secretJson.getString("symmetric");
                                        String asymmetric = secretJson.getString("asymmetric");
                                        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(symmetric) && !TextUtils.isEmpty(asymmetric)) {
                                            secreteKey = new TDSecreteKey(key, version, symmetric, asymmetric);
                                        }
                                    }
                                }

//                                if (data.has("server_timestamp") && !TDPresetProperties.disableList.contains(TDConstants.KEY_CALIBRATION_TYPE)) {
//                                    long timestamp = data.optLong("server_timestamp");
//                                    if (timestamp != 0) {
//                                        long t2 = System.currentTimeMillis();
//                                        CalibratedTimeManager.calibrateTime(timestamp + (t2 - t1) / 2);
//                                    }
//                                }

                                TDLog.d(TAG, "Fetched remote config for (" + TDUtils.getSuffix(mToken,  4)
                                        + "):\n" + data.toString(4));

                                if (data.has("disable_event_list")) {
                                    mDisabledEventsLock.writeLock().lock();
                                    try {
                                        JSONArray disabledEventList = data.getJSONArray("disable_event_list");
                                        for (int i = 0; i < disabledEventList.length(); i++) {
                                            mDisabledEvents.add(disabledEventList.getString(i));
                                        }
                                    } finally {
                                        mDisabledEventsLock.writeLock().unlock();
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            int localFlushBulkSize = mConfigStoragePlugin.get(LocalStorageType.FLUSH_SIZE);
                            if (localFlushBulkSize != newUploadSize) {
                                mConfigStoragePlugin.save(LocalStorageType.FLUSH_SIZE,newUploadSize);
                            }
                            int localFlushInterval = mConfigStoragePlugin.get(LocalStorageType.FLUSH_INTERVAL);
                            if (localFlushInterval != newUploadInterval) {
                                mConfigStoragePlugin.save(LocalStorageType.FLUSH_INTERVAL,newUploadInterval);
                            }
                        }

                        in.close();
                        br.close();
                    } else {
                        TDLog.d(TAG, "Getting remote config failed, responseCode is " + connection.getResponseCode());
                    }

                } catch (IOException e) {
                    TDLog.d(TAG, "Getting remote config failed due to: " + e.getMessage());
                } catch (JSONException e) {
                    TDLog.d(TAG, "Getting remote config failed due to: " + e.getMessage());
                } catch (Exception e) {
                    TDLog.d(TAG, "Getting remote config failed due to: " + e.getMessage());
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
        return false;
    }

    /**
     * Flush interval
     *
     * @return interval
     */
    int getFlushInterval() {
        return mConfigStoragePlugin.get(LocalStorageType.FLUSH_INTERVAL);
    }

    int getFlushBulkSize() {
        return mConfigStoragePlugin.get(LocalStorageType.FLUSH_SIZE);
    }

    /**
     * Get the server key configuration.
     *
     * @return key configuration
     */
    public TDSecreteKey getSecreteKey() {
        return secreteKey;
    }

    String getMainProcessName() {
        return mContextConfig.getMainProcessName();
    }


    synchronized void setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType type) {
        switch (type) {
            case NETWORKTYPE_WIFI:
                mNetworkType = NetworkType.TYPE_WIFI;
                break;
            case  NETWORKTYPE_DEFAULT:
            case NETWORKTYPE_ALL:
                mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_5G | NetworkType.TYPE_WIFI | NetworkType.TYPE_2G;
                break;
            default:
                break;
        }
    }

    public final class NetworkType {
        public static final int TYPE_2G = 1; //2G
        public static final int TYPE_3G = 1 << 1; //3G
        public static final int TYPE_4G = 1 << 2; //4G
        public static final int TYPE_WIFI = 1 << 3; //WIFI
        public static final int TYPE_5G = 1 << 4; // 5G
        public static final int TYPE_ALL = 0xFF; //ALL
    }

    private int mNetworkType = NetworkType.TYPE_ALL;

    /**
     * Set whether to track older version data.
     *
     * @param trackOldData Tracking or not
     */
    public TDConfig setTrackOldData(boolean trackOldData) {
        mTrackOldData = trackOldData;
        return this;
    }

    public boolean trackOldData() {
        return mTrackOldData;
    }

    public synchronized TDConfig setDefaultTimeZone(TimeZone timeZone) {
        mDefaultTimeZone = timeZone;
        return this;
    }

    public synchronized TimeZone getDefaultTimeZone() {
        return mDefaultTimeZone == null ? TimeZone.getDefault() : mDefaultTimeZone;
    }

    /**
     * Whether to enable encryption.
     *
     * @param enableEncrypt boolean
     */
    public TDConfig enableEncrypt(boolean enableEncrypt) {
        this.mEnableEncrypt = enableEncrypt;
        return this;
    }

    public TDConfig setSecretKey(TDSecreteKey key) {
        if (secreteKey == null) {
            //Only one assignment is allowed
            secreteKey = key;
        }
        return this;
    }

    /**
     * Set the self - visa. The self - visa is valid for all network requests in the instance.
     *
     * @param sslSocketFactory Self-signed certificate
     */
    public synchronized TDConfig setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        if (null != sslSocketFactory) {
            mSSLSocketFactory = sslSocketFactory;
            getRemoteConfig();
        }
        return this;
    }

    /**
     * Returns the current autoform Settings
     *
     * @return SSLSocketFactory
     */
    public synchronized SSLSocketFactory getSSLSocketFactory() {
        return mSSLSocketFactory;
    }

    //Compatible with older versions before 1.2.0. Since 1.3.0, app ids will be stored in the local cache. By default, legacy data is reported to the first initialized instance.
    private volatile boolean mTrackOldData = true;

    private final TDContextConfig mContextConfig;

    private final String mServerUrl;
    private final String mDebugUrl;
    private final String mConfigUrl;
    private boolean mEnableMutiprocess;
    private TDSecreteKey secreteKey = null;
    final String mToken;
    final Context mContext;

    boolean mEnableEncrypt = false;

    private SSLSocketFactory mSSLSocketFactory;

    private TimeZone mDefaultTimeZone;

    private static final String TAG = "ThinkingAnalytics.TDConfig";
}