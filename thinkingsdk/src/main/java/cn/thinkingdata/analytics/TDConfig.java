/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import android.content.Context;
import android.text.TextUtils;

import cn.thinkingdata.analytics.persistence.ConfigStoragePlugin;
import cn.thinkingdata.analytics.persistence.LocalStorageType;
import cn.thinkingdata.analytics.tasks.TrackTaskManager;
import cn.thinkingdata.analytics.utils.CalibratedTimeManager;
import cn.thinkingdata.analytics.utils.DNSServiceManager;
import cn.thinkingdata.analytics.utils.TDUtils;
import cn.thinkingdata.analytics.encrypt.TDSecreteKey;
import cn.thinkingdata.core.utils.TDLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
 */
public class TDConfig {

    public static final String VERSION = BuildConfig.VERSION_NAME;
    private static final String SECRET_KEY_MODE_GCM = "gcm";
    //Compatible with older versions before 1.2.0. Since 1.3.0, app ids will be stored in the local cache. By default, legacy data is reported to the first initialized instance.
    private volatile boolean mTrackOldData = true;
    private static final Map<Context, Map<String, TDConfig>> sInstances = new HashMap<>();
    private final Set<String> mDisabledEvents = new HashSet<>();
    private final ReadWriteLock mDisabledEventsLock = new ReentrantReadWriteLock();
    private ConfigStoragePlugin mConfigStoragePlugin;
    public DNSServiceManager mDnsServiceManager;
    private volatile ModeEnum mMode = ModeEnum.NORMAL;
    private volatile String name;
    private int mNetworkType = NetworkType.TYPE_ALL;
    private String mServerUrl;
    private String mDebugUrl;
    private String mConfigUrl;
    private boolean mEnableMutiprocess;
    private TDSecreteKey secreteKey = null;
    private volatile int mGcmSupport = ConfigStoragePlugin.GCM_UNKNOWN;
    public final String mToken;
    public final Context mContext;
    boolean mEnableEncrypt = false;
    public boolean mEnableDNS = false;
    public boolean disableRConfig = false;
    private SSLSocketFactory mSSLSocketFactory;
    private TimeZone mDefaultTimeZone;
    public boolean mEnableAutoPush = false;
    public boolean enableAutoCalibrated = false;
    /**
     * Backup server URL list. Users can directly add URLs to this list.
     * URLs should be the base URL without "/sync" suffix (e.g., "https://example.com").
     * The SDK will automatically append "/sync" when making requests.
     */
    public List<String> backupUrlList = new ArrayList<>();
    private int mCurrentUrlIndex = 0;
    /**
     * Pre-parsed server URLs (including /sync suffix).
     */
    private final List<String> mAllServerUrls = new ArrayList<>();

    private List<TDDNSService> mDNSServiceList;
    private static final String TAG = "ThinkingAnalytics.TDConfig";

    public TDConfig(Context context, final String token, final URL url) {
        mContext = context;
        mToken = token;
        mEnableMutiprocess = false;
        if (null == url) return;
        final String host = url.getHost();
        String sUrl = url.getProtocol()
                + "://" + host
                + (url.getPort() > 0 ? ":" + url.getPort() : "");
        mServerUrl = sUrl + "/sync";
        mDebugUrl = sUrl + "/data_debug";
        mConfigUrl = sUrl + "/config?appid=" + token;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mDnsServiceManager = new DNSServiceManager();
                mConfigStoragePlugin = new ConfigStoragePlugin(mContext, token);
                if (!disableRConfig) {
                    getRemoteConfig();
                }
            }
        });

    }

    // Internal use only. This method should be called after the instance was initialed.
    public static TDConfig getInstance(Context context, String token) {
        try {
            return getInstance(context, token, "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Obtain the TDConfig instance. This example can be used to initialize ThinkingAnalyticsSDK. Each SDK instance corresponds to one TDConfig instance.
     *
     * @param context app context
     * @param token   APP ID
     * @param url     The URL of the data receiving end must be the complete URL with the protocol; otherwise, an exception will be thrown
     * @return TDConfig instance
     */
    public static TDConfig getInstance(Context context, String token, String url) {
        return getInstance(context, token, url, token);
    }

    /**
     * Obtain the TDConfig instance. This example can be used to initialize ThinkingAnalyticsSDK. Each SDK instance corresponds to one TDConfig instance.
     *
     * @param context app context
     * @param token   APP ID
     * @param url     The URL of the data receiving end must be the complete URL with the protocol; otherwise, an exception will be thrown
     * @param name    instance name
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
                    serverUrl = null;
                }
                instance = new TDConfig(appContext, token, serverUrl);
                instance.setName(name);
                instances.put(name, instance);
            }
            return instance;
        }
    }

    private void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
     * @return TDConfig
     */
    public TDConfig setMutiprocess(boolean isSupportMultiProcess) {
        mEnableMutiprocess = isSupportMultiProcess;
        return this;
    }

    /**
     * Whether to allow multi-process
     *
     * @return allow multi-process
     */
    public boolean isEnableMutiprocess() {
        return mEnableMutiprocess;
    }


    /**
     * Set the SDK running mode.
     *
     * @param mode running mode
     * @return TDConfig
     */
    public TDConfig setMode(ModeEnum mode) {
        this.mMode = mode;
        return this;
    }

    /**
     * Set the SDK running mode.
     *
     * @param mode running mode
     * @return TDConfig
     */
    public TDConfig setMode(TDMode mode) {
        switch (mode) {
            case DEBUG:
                this.mMode = ModeEnum.DEBUG;
                break;
            case DEBUG_ONLY:
                this.mMode = ModeEnum.DEBUG_ONLY;
                break;
            case NORMAL:
                this.mMode = ModeEnum.NORMAL;
                break;
        }
        return this;
    }

    /**
     * Obtain the current running mode of the SDK.
     *
     * @return ModeEnum
     */
    public ModeEnum getMode() {
        return mMode;
    }

    public synchronized boolean isShouldFlush(String networkType) {
        return (TDUtils.convertToNetworkType(networkType) & mNetworkType) != 0;
    }

    public String getServerUrl() {
        return mServerUrl;
    }

    /**
     * Get the current server URL being used (primary or backup).
     * The URL list is built dynamically: [serverUrl, backupUrl1, backupUrl2, ...]
     *
     * @return current server URL with "/sync" suffix
     */
    public String getCurrentServerUrl() {
        if (mCurrentUrlIndex < 0 || mCurrentUrlIndex >= mAllServerUrls.size()) {
            return mServerUrl;
        }
        return mAllServerUrls.get(mCurrentUrlIndex);
    }

    /**
     * Initialize all server URLs in advance to avoid repeated string concatenation.
     * This method should be called in the async task during construction.
     *
     */
    void initAllServerUrls(List<String> backupUrls) {
        // Add primary URL
        mAllServerUrls.add(mServerUrl);
        for (String urlStr : backupUrls) {
            if (!TextUtils.isEmpty(urlStr) && mAllServerUrls.size() < 6) {
                try {
                    URL backupUrl = new URL(urlStr);
                    String baseUrl = backupUrl.getProtocol() + "://" + backupUrl.getHost()
                            + (backupUrl.getPort() > 0 ? ":" + backupUrl.getPort() : "");
                    String syncUrl = baseUrl + "/sync";
                    mAllServerUrls.add(syncUrl);
                } catch (MalformedURLException e) {
                    TDLog.e(TAG, "Invalid backup URL: " + urlStr);
                }
            }
        }
        // Initialize DNS service with all URLs
        if (mDnsServiceManager != null && mEnableDNS) {
            mDnsServiceManager.enableDNSService(mAllServerUrls,mDNSServiceList);
        }
    }

    /**
     * Switch to the next URL in the list (for retry logic).
     *
     */
    public void switchToNextUrl() {
        mCurrentUrlIndex++;
        if (mCurrentUrlIndex > mAllServerUrls.size()) {
            mCurrentUrlIndex = 0;
        }
    }

    /**
     * Get total number of available URLs (primary + backups).
     *
     * @return total URL count
     */
    public List<String> getAllServerUrlList() {
        return mAllServerUrls;
    }

    public String getDebugUrl() {
        return mDebugUrl;
    }

    boolean isDebug() {
        return ModeEnum.DEBUG.equals(mMode);
    }

    public boolean isDebugOnly() {
        return ModeEnum.DEBUG_ONLY.equals(mMode);
    }

    public boolean isNormal() {
        return ModeEnum.NORMAL.equals(mMode);
    }

    public int getFlushInterval() {
        if (mConfigStoragePlugin == null) {
            return ConfigStoragePlugin.DEFAULT_FLUSH_INTERVAL;
        }
        return mConfigStoragePlugin.get(LocalStorageType.FLUSH_INTERVAL, ConfigStoragePlugin.DEFAULT_FLUSH_INTERVAL);
    }

    public int getFlushBulkSize() {
        if (mConfigStoragePlugin == null) {
            return ConfigStoragePlugin.DEFAULT_FLUSH_BULK_SIZE;
        }
        return mConfigStoragePlugin.get(LocalStorageType.FLUSH_SIZE, ConfigStoragePlugin.DEFAULT_FLUSH_BULK_SIZE);
    }


    synchronized void setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType type) {
        switch (type) {
            case NETWORKTYPE_WIFI:
                mNetworkType = NetworkType.TYPE_WIFI;
                break;
            case NETWORKTYPE_DEFAULT:
            case NETWORKTYPE_ALL:
                mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_5G | NetworkType.TYPE_WIFI | NetworkType.TYPE_2G;
                break;
            default:
                break;
        }
    }


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
        if (mDefaultTimeZone == null) {
            mDefaultTimeZone = TimeZone.getDefault();
        }
        return mDefaultTimeZone;
    }

    public TDSecreteKey getSecreteKey() {
        return secreteKey;
    }

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

    public TDConfig enableEncrypt(int version, String publicKey) {
        this.mEnableEncrypt = true;
        if (secreteKey == null) {
            secreteKey = new TDSecreteKey();
            secreteKey.version = version;
            secreteKey.publicKey = publicKey;
            secreteKey.asymmetricEncryption = "RSA";
            secreteKey.symmetricEncryption = "AES";
        }
        return this;
    }

    /**
     * Whether the receiver is known to support AES-GCM. Report data is only encrypted when this
     * returns true, so that a new SDK talking to an old receiver falls back to plaintext instead
     * of uploading data the receiver cannot decrypt.
     *
     * <p>The flag comes from the remote config field {@code secret_key.mode} and is persisted, so
     * from the second launch on it is available before the config request returns. On the very
     * first launch it stays unknown until the config response arrives.
     *
     * @return true if AES-GCM is supported by the receiver
     */
    public boolean isEncryptGcmSupported() {
        // Without remote config there is no way to know what the receiver supports.
        if (disableRConfig) {
            return false;
        }
        if (mGcmSupport == ConfigStoragePlugin.GCM_UNKNOWN && mConfigStoragePlugin != null) {
            mGcmSupport = mConfigStoragePlugin.get(LocalStorageType.ENCRYPT_SUPPORT_GCM, ConfigStoragePlugin.GCM_UNKNOWN);
        }
        return mGcmSupport == ConfigStoragePlugin.GCM_SUPPORTED;
    }

    /**
     * Whether the remote config declares AES-GCM support through {@code secret_key.mode}.
     *
     * @param data the {@code data} object of the config response
     * @return true if the receiver supports AES-GCM
     */
    static boolean parseGcmSupported(JSONObject data) {
        if (data == null) {
            return false;
        }
        JSONObject secretJson = data.optJSONObject("secret_key");
        if (secretJson == null) {
            return false;
        }
        return SECRET_KEY_MODE_GCM.equalsIgnoreCase(secretJson.optString("mode"));
    }

    /**
     * Whether to warn that encryption was requested but the receiver cannot decrypt AES-GCM
     * (typically an old AE version whose config {@code secret_key} has no {@code mode} field).
     *
     * @param enableEncrypt whether the client called {@link #enableEncrypt}
     * @param gcmSupported  whether the remote config declares AES-GCM support
     * @return true if an error should be logged for the customer
     */
    static boolean shouldLogEncryptWithoutGcm(boolean enableEncrypt, boolean gcmSupported) {
        return enableEncrypt && !gcmSupported;
    }

    private void updateGcmSupport(boolean supported) {
        int newState = supported ? ConfigStoragePlugin.GCM_SUPPORTED : ConfigStoragePlugin.GCM_UNSUPPORTED;
        if (mGcmSupport == newState) {
            return;
        }
        mGcmSupport = newState;
        if (mConfigStoragePlugin != null) {
            mConfigStoragePlugin.save(LocalStorageType.ENCRYPT_SUPPORT_GCM, newState);
        }
    }

    private void logEncryptWithoutGcmIfNeeded(boolean gcmSupported) {
        if (!shouldLogEncryptWithoutGcm(mEnableEncrypt, gcmSupported)) {
            return;
        }
        TDLog.e(TAG, "[ThinkingData] Error: Encryption is enabled but the receiver does not support "
                + "AES-GCM (config secret_key.mode is missing). Please contact technical support "
                + "to upgrade to the latest AE system version.");
    }

    public TDConfig enableDNSService(final List<TDDNSService> lists) {
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mEnableDNS = true;
                mDNSServiceList = new ArrayList<>(lists);
            }
        });
        return this;
    }

    public TDConfig enableAutoPush() {
        this.mEnableAutoPush = true;
        return this;
    }

    public synchronized TDConfig setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        if (null != sslSocketFactory) {
            mSSLSocketFactory = sslSocketFactory;
            if (!disableRConfig) {
                getRemoteConfig();
            }
        }
        return this;
    }

    public synchronized SSLSocketFactory getSSLSocketFactory() {
        return mSSLSocketFactory;
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
                    connection = ( HttpURLConnection ) url.openConnection();
                    final SSLSocketFactory socketFactory = getSSLSocketFactory();
                    if (null != socketFactory && connection instanceof HttpsURLConnection) {
                        (( HttpsURLConnection ) connection).setSSLSocketFactory(socketFactory);
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
                            int newUploadInterval = mConfigStoragePlugin.get(LocalStorageType.FLUSH_INTERVAL, ConfigStoragePlugin.DEFAULT_FLUSH_INTERVAL);
                            int newUploadSize = mConfigStoragePlugin.get(LocalStorageType.FLUSH_SIZE, ConfigStoragePlugin.DEFAULT_FLUSH_BULK_SIZE);
                            try {
                                JSONObject data = rjson.getJSONObject("data");
                                // Decided before anything else may throw: an old receiver without
                                // "mode" must switch encryption off rather than leave it unknown.
                                boolean gcmSupported = parseGcmSupported(data);
                                updateGcmSupport(gcmSupported);
                                logEncryptWithoutGcmIfNeeded(gcmSupported);
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
                                if (enableAutoCalibrated) {
                                    long timestamp = data.optLong("server_timestamp");
                                    if (timestamp != 0) {
                                        long t2 = System.currentTimeMillis();
                                        CalibratedTimeManager.calibrateTime(timestamp + (t2 - t1) / 2);
                                    }
                                }

                                TDLog.i(TAG, "[ThinkingData] Info: Get remote config success (" + TDUtils.getSuffix(mToken, 4)
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

                            int localFlushBulkSize = mConfigStoragePlugin.get(LocalStorageType.FLUSH_SIZE, ConfigStoragePlugin.DEFAULT_FLUSH_BULK_SIZE);
                            if (localFlushBulkSize != newUploadSize) {
                                mConfigStoragePlugin.save(LocalStorageType.FLUSH_SIZE, newUploadSize);
                            }
                            int localFlushInterval = mConfigStoragePlugin.get(LocalStorageType.FLUSH_INTERVAL, ConfigStoragePlugin.DEFAULT_FLUSH_INTERVAL);
                            if (localFlushInterval != newUploadInterval) {
                                mConfigStoragePlugin.save(LocalStorageType.FLUSH_INTERVAL, newUploadInterval);
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


    /**
     * Running mode. The default mode is NORMAL.
     */
    public enum ModeEnum {
        /**
         * In normal mode, data is cached and reported according to certain cache policies
         */
        NORMAL,
        /**
         * Debug mode: Data is reported one by one. When a problem occurs, the user is alerted in the form of logs and exceptions
         */
        DEBUG,
        /**
         * Debug Only mode: verifies data and does not store data in the database
         */
        DEBUG_ONLY
    }

    /**
     * Running mode. The default mode is NORMAL.
     */
    public enum TDMode {
        /**
         * In normal mode, data is cached and reported according to certain cache policies
         */
        NORMAL,
        /**
         * Debug mode: Data is reported one by one. When a problem occurs, the user is alerted in the form of logs and exceptions
         */
        DEBUG,
        /**
         * Debug Only mode: verifies data and does not store data in the database
         */
        DEBUG_ONLY
    }

    public enum TDDNSService {
        CLOUD_FLARE,
        CLOUD_ALI,
        CLOUD_GOOGLE
    }

    public final class NetworkType {
        public static final int TYPE_2G = 1; //2G
        public static final int TYPE_3G = 1 << 1; //3G
        public static final int TYPE_4G = 1 << 2; //4G
        public static final int TYPE_WIFI = 1 << 3; //WIFI
        public static final int TYPE_5G = 1 << 4; // 5G
        public static final int TYPE_ALL = 0xFF; //ALL
    }

}