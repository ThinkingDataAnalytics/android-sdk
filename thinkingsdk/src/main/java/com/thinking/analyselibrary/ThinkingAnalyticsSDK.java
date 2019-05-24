package com.thinking.analyselibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.PropertyUtils;
import com.thinking.analyselibrary.utils.SettingsUtils;
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ThinkingAnalyticsSDK implements IThinkingAnalyticsAPI {

    public static final String VERSION = BuildConfig.TDSDK_VERSION;;
    private static final String TAG = "ThinkingAnalyticsSDK";

    private static final int MESSAGE_GET_CONFIG = 1; // 获取服务器配置

    /**
     * 获取默认SDK实例，适合在只有一个实例的情况下使用
     * @return 第一个可用的SDK实例
     */
    public static ThinkingAnalyticsSDK sharedInstance() {
        synchronized (sInstanceMap) {
            if (sInstanceMap.size() > 0) {
                Iterator<ThinkingAnalyticsSDK> iterator = sInstanceMap.values().iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
            }
            return null;
        }
    }

    public static ThinkingAnalyticsSDK sharedInstance(Context context) {
        if (null == context) {
            return null;
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();
            ThinkingAnalyticsSDK instance = sInstanceMap.get(appContext);

            if (null == instance) {
                TDLog.d(TAG,"Please call method ThinkingAnalyticsSDK.sharedInstance(" +
                        "Context context,String appKey, String serverURL, String url) first ");
            }
            return instance;
        }
    }

    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url) {
        if (null == context) {
            TDLog.d(TAG, "param context is null");
            return null;
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            ThinkingAnalyticsSDK instance = sInstanceMap.get(appContext);
            if (null == instance) {
                instance = new ThinkingAnalyticsSDK(appContext,
                        appId,
                        url+"/sync",
                        url+"/config");
                sInstanceMap.put(appContext, instance);
            }

            return instance;
        }
    }

    /**
     * 初始化SDK
     * @param context APP context
     * @param appId 项目的APP_ID
     * @param serverURL 数据上报地址
     * @param configureURL 配置服务器地址
     */
    ThinkingAnalyticsSDK(Context context, String appId, String serverURL, String configureURL) {
        mContext = context;
        final String packageName = context.getApplicationContext().getPackageName();
        mAppKey = appId;
        mServerUrl = serverURL;

        final String prefsName = "com.thinkingdata.analyse";
        final SharedPreferencesFuture sPrefsLoader = new SharedPreferencesFuture();
        Future<SharedPreferences> storedPreferences =
                sPrefsLoader.loadPreferences(context, prefsName);

        // 获取数据上传的触发条件，默认为间隔15秒，或者数据达到20条
        mFlushInterval = SettingsUtils.getUploadInterval(mContext);
        mFlushBulkSize = SettingsUtils.getUploadSize(mContext);

        // 获取保存在本地的用户ID和公共属性
        mLoginId = new StorageLoginID(storedPreferences);
        mIdentifyId = new StorageIdentifyId(storedPreferences);
        mRandomID = new StorageRandomID(storedPreferences);
        mSuperProperties = new StorageSuperProperties(storedPreferences);

        mVersionName = TDUtil.getVersionName(mContext);

        mMessages = DataHandle.getInstance(mContext, packageName);
        final Map<String, Object> deviceInfo = TDUtil.getDeviceInfo(mContext);
        mDeviceInfo = Collections.unmodifiableMap(deviceInfo);

        mTrackTimer = new HashMap<>();

        mAutoTrackIgnoredActivities = new ArrayList<>();

        try {
            final ApplicationInfo appInfo = context.getApplicationContext().getPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA);

            Bundle configBundle = appInfo.metaData;
            if (null == configBundle) {
                configBundle = new Bundle();
            }
            mAutoTrack = configBundle.getBoolean("com.thinkingdata.analytics.android.AutoTrack",
                    false);
            mMainProcessName = configBundle.getString("com.thinkingdata.analytics.android.MainProcessName");
            mEnableTracklog = configBundle.getBoolean("com.thinkingdata.analytics.android.EnableTrackLogging",
                    false);
            TDLog.setEnableLog(mEnableTracklog);
            mAutoTrackEventTypeList = new ArrayList<>();


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                final Application app = (Application) context.getApplicationContext();
                app.registerActivityLifecycleCallbacks(new ThinkingDataActivityLifecycleCallbacks(this, mMainProcessName));
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // 获取服务器配置的数据上传触发条件
        getConfig(configureURL);

        TDLog.i(TAG, "Thank you very much for using Thinking Data. We will do our best to provide you with the best service.");
        TDLog.i(TAG, String.format("Thinking Data SDK version:%s", VERSION));
    }

    private void getConfig(final String configureUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                InputStream in = null;

                try {
                    URL url = new URL(configureUrl + "?appid=" + mAppKey);
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

                            if (mFlushBulkSize != newUploadSize || mFlushBulkSize != newUploadSize) {
                                SettingsUtils.setUploadInterval(mContext,newUploadInterval);
                                SettingsUtils.setUploadSize(mContext,newUploadSize);

                                synchronized (this) {
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

    void trackFromH5(String event) {
        try {
            JSONArray data = new JSONObject(event).getJSONArray("data");
            JSONObject eventObject = data.getJSONObject(0);

            String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
            SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern, Locale.CHINA);
            Date time = sDateFormat.parse(eventObject.getString("#time"));

            String event_type = eventObject.getString("#type");
            String event_name = null;
            if (event_type.equals("track")) {
                event_name =eventObject.getString("#event_name");
            }

            JSONObject properties = eventObject.getJSONObject("properties");
            for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
                String key = (String) iterator.next();
                if (key.equals("#account_id") || key.equals("#distinct_id") || mDeviceInfo.containsKey(key)) {
                    iterator.remove();
                }
            }

            clickEvent(event_type, event_name, properties, time, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final class NetworkType {
        public static final int TYPE_2G = 1; //2G
        public static final int TYPE_3G = 1 << 1; //3G
        public static final int TYPE_4G = 1 << 2; //4G
        public static final int TYPE_WIFI = 1 << 3; //WIFI
        public static final int TYPE_ALL = 0xFF; //ALL
    }

    public enum ThinkingdataNetworkType {
        NETWORKTYPE_DEFAULT,
        NETWORKTYPE_WIFI,
        NETWORKTYPE_ALL
    }

    @Override
    public void setNetworkType(ThinkingdataNetworkType type) {
        switch (type) {
            case NETWORKTYPE_DEFAULT:
                mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI;
                break;
            case NETWORKTYPE_WIFI:
                mNetworkType = NetworkType.TYPE_WIFI;
                break;
            case NETWORKTYPE_ALL:
                mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI | NetworkType.TYPE_2G;
                break;
        }
    }

    public boolean isShouldFlush(String networkType) {
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
        }
        return NetworkType.TYPE_ALL;
    }

    protected void autoTrack(String eventName, JSONObject properties) {
        clickEvent("track", eventName, properties, new Date(), true);
    }

    @Override
    public void track(String eventName, JSONObject properties) {
        clickEvent("track", eventName, properties);
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time) {
        clickEvent("track", eventName, properties, time, false);
    }

    @Override
    public void track(String eventName) {
        TDLog.d("TAG", "track " + eventName);
        clickEvent("track", eventName, null);
    }

    private void clickEvent(String eventType, String eventName, JSONObject properties) {
        clickEvent(eventType, eventName, properties, new Date(), false);
    }

    private void clickEvent(String eventType, String eventName, JSONObject properties, Date time, boolean isAutoTrack) {
        if(eventType != null && eventType.equals("track")) {
            if(!PropertyUtils.checkString(eventName)) {
                TDLog.d(TAG, "property name[" + eventName + "] is not valid");
                return;
            }
        }

        if (!isAutoTrack && !PropertyUtils.checkProperty(properties)) {
            TDLog.d(TAG, "Properties checking failed: " + properties.toString());
            return;
        }

        try {
            String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
            SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern, Locale.CHINA);
            String timeString = (null != time) ? sDateFormat.format(time) : sDateFormat.format(new Date());

            final JSONObject dataObj = new JSONObject();

            dataObj.put("#type", eventType);
            dataObj.put("#time", timeString);
            if(eventName != null && eventName.length() > 0) {
                dataObj.put("#event_name", eventName);
            }

            if (!TextUtils.isEmpty(getLoginId())) {
                dataObj.put("#account_id", getLoginId());
            }
            String identifyId = getIdentifyID();
            if(identifyId == null) {
                dataObj.put("#distinct_id", getRandomID());
            }
            else {
                dataObj.put("#distinct_id", identifyId);
            }

            final JSONObject sendProps = new JSONObject();
            if (null != properties) {
                final Iterator<?> propIterator = properties.keys();
                while (propIterator.hasNext()) {
                    final String key = (String) propIterator.next();
                    if (!properties.isNull(key)) {
                        sendProps.put(key, properties.get(key));
                    }
                }
            }

            JSONObject finalProperties = new JSONObject();
            if(eventType.equals("track")) {
                synchronized (mSuperProperties) {
                    JSONObject superProperties = mSuperProperties.get();
                    TDUtil.mergeJSONObject(superProperties, finalProperties);
                }
            }

            TDUtil.mergeJSONObject(sendProps, finalProperties);

            String networkType = TDUtil.networkType(mContext);
            if(eventType.equals("track")) {
                finalProperties.put("#network_type", networkType);
                if (!TextUtils.isEmpty(mVersionName)) {
                    finalProperties.put("#app_version", mVersionName);
                }
            }

            final EventTimer eventTimer;
            if (eventName != null) {
                synchronized (mTrackTimer) {
                    eventTimer = mTrackTimer.get(eventName);
                    mTrackTimer.remove(eventName);
                }
            } else {
                eventTimer = null;
            }

            if (null != eventTimer) {
                try {
                    Double duration = Double.valueOf(eventTimer.duration());
                    if (duration > 0) {
                        finalProperties.put("#duration", duration);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(finalProperties != null) {
                dataObj.put("properties", finalProperties);
            }


            mMessages.saveClickData(dataObj);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void user_add(String propertyName, Number propertyValue) {
        try {
            if (!(propertyValue instanceof Number)) {
                TDLog.d(TAG, "user_add value must be Number");
            } else {
                JSONObject json = new JSONObject();
                json.put(propertyName, propertyValue);
                clickEvent("user_add", null, json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void user_add(JSONObject property) {
        clickEvent("user_add", null, property);
    }

    @Override
    public void user_setOnce(JSONObject property) {
        clickEvent("user_setOnce", null, property);
    }

    @Override
    public void user_set(JSONObject property) {
        clickEvent("user_set", null, property);
    }

    @Override
    public void user_delete() {
        clickEvent("user_del", null, null);
    }

    @Override
    public void identify(String identify) {
        if (identify == null) {
            TDLog.d(TAG,"The identify cannot empty.");
            return;
        }

        synchronized (mIdentifyId) {
            mIdentifyId.put(identify);
        }
    }

    @Override
    public void login(String loginId) {
        try {
            if(TDUtil.checkNull(loginId)) {
                TDLog.d(TAG,"login_id cannot be empty.");
                return;
            }

            synchronized (mLoginId) {
                if (!loginId.equals(mLoginId.get())) {
                    mLoginId.put(loginId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logout() {
        try {
            synchronized (mLoginId) {
                mLoginId.put(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLoginId() {
        synchronized (mLoginId) {
            return mLoginId.get();
        }
    }

    private String getRandomID() {
        synchronized (mRandomID) {
            return mRandomID.get();
        }
    }

    private String getIdentifyID() {
        synchronized (mIdentifyId) {
            return mIdentifyId.get();
        }
    }

    public String getDistinctId(){
        String identifyId = getIdentifyID();
        if(identifyId == null) {
            return getRandomID();
        }
        else
        {
            return identifyId;
        }
    }

    @Override
    public JSONObject getSuperProperties(){
        synchronized (mSuperProperties) {
            return mSuperProperties.get();
        }
    }

    @Override
    public void setSuperProperties(JSONObject superProperties) {
        try {
            if (superProperties == null || !PropertyUtils.checkProperty(superProperties)) {
                return;
            }

            synchronized (mSuperProperties) {
                JSONObject properties = mSuperProperties.get();
                TDUtil.mergeJSONObject(superProperties, properties);
                mSuperProperties.put(properties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unsetSuperProperty(String superPropertyName) {
        try {
            if (superPropertyName == null) {
                return;
            }
            synchronized (mSuperProperties) {
                JSONObject superProperties = mSuperProperties.get();
                superProperties.remove(superPropertyName);
                mSuperProperties.put(superProperties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearSuperProperties() {
        synchronized (mSuperProperties) {
            mSuperProperties.put(new JSONObject());
        }
    }

    @Override
    public void timeEvent(final String eventName) {
        try {
            if(!PropertyUtils.checkString(eventName)) {
                TDLog.d(TAG, "timeEvent event name[" + eventName + "] is not valid");
                return;
            }

            synchronized (mTrackTimer) {
                mTrackTimer.put(eventName, new EventTimer(TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        if (activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
            return true;
        }

        if (activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class) != null) {
            return true;
        }

        return false;
    }

    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        if (eventType != null  && !mAutoTrackEventTypeList.contains(eventType)) {
            return true;
        }
        return false;
    }

    public boolean isAutoTrackEnabled() {
        return mAutoTrack;
    }

    public enum AutoTrackEventType {
        APP_START("ta_app_start"),
        APP_END("ta_app_end"),
        APP_CLICK("ta_app_click"),
        APP_VIEW_SCREEN("ta_app_view"),
        APP_CRASH("ta_app_crash");

        private final String eventName;

        public static AutoTrackEventType autoTrackEventTypeFromEventName(String eventName) {
            if (TextUtils.isEmpty(eventName)) {
                return null;
            }

            if ("ta_app_start".equals(eventName)) {
                return APP_START;
            } else if ("ta_app_end".equals(eventName)) {
                return APP_END;
            } else if ("ta_app_click".equals(eventName)) {
                return APP_CLICK;
            } else if ("ta_app_view".equals(eventName)) {
                return APP_VIEW_SCREEN;
            } else if ("ta_app_crash".equals(eventName)) {
                return APP_CRASH;
            }

            return null;
        }

        AutoTrackEventType(String eventName) {
            this.eventName = eventName;
        }

        String getEventName() {
            return eventName;
        }
    }

    public JSONObject getLastScreenTrackProperties() {
        return mLastScreenTrackProperties;
    }

    void trackViewScreenInternal(String url, JSONObject properties, boolean checkProperties) {
        try {
            if (checkProperties && !PropertyUtils.checkProperty(properties)) {
                TDLog.d(TAG, "Properties is not valid");
                return;
            }
            if ((!TextUtils.isEmpty(url) || properties != null)) {
                JSONObject trackProperties = new JSONObject();
                mLastScreenTrackProperties = properties;

                if (!TextUtils.isEmpty(mLastScreenUrl)) {
                    trackProperties.put("#referrer", mLastScreenUrl);
                }

                trackProperties.put("#url", url);
                mLastScreenUrl = url;
                if (properties != null) {
                    TDUtil.mergeJSONObject(properties, trackProperties);
                }
                autoTrack("ta_app_view", trackProperties);
            }
        } catch (JSONException e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }

    public void trackViewScreen(String url, JSONObject properties) {
        trackViewScreenInternal(url, properties, true);
    }

    @Override
    public void trackViewScreen(Activity activity) {
        try {
            if (activity == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            properties.put("#screen_name", activity.getClass().getCanonicalName());
            TDUtil.getScreenNameAndTitleFromActivity(properties, activity);

            if (activity instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;

                String screenUrl = screenAutoTracker.getScreenUrl();
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    TDUtil.mergeJSONObject(otherProperties, properties);
                }

                trackViewScreenInternal(screenUrl, properties, false);
            } else {
                autoTrack("ta_app_view", properties);
            }
        } catch (Exception e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }

    @Override
    public void trackViewScreen(android.app.Fragment fragment) {
        try {
            if (fragment == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            String fragmentName = fragment.getClass().getCanonicalName();
            String screenName = fragmentName;
            String title = AopUtil.getTitleFromFragment(fragment);

            if (Build.VERSION.SDK_INT >= 11) {
                Activity activity = fragment.getActivity();
                if (activity != null) {
                    if (TextUtils.isEmpty(title)) {
                        title = TDUtil.getActivityTitle(activity);
                    }
                    screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName);
                }
            }

            if (!TextUtils.isEmpty(title)) {
                properties.put(AopConstants.TITLE, title);
            }

            properties.put("#screen_name", screenName);
            autoTrack("ta_app_view", properties);
        } catch (Exception e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }


    @Override
    public void trackViewScreen(final Object fragment) {
        if (fragment == null) {
            return;
        }

        Class<?> supportFragmentClass = null;
        Class<?> appFragmentClass = null;
        Class<?> androidXFragmentClass = null;

        try {
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                appFragmentClass = Class.forName("android.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }
        } catch (Exception e) {
            //ignored
        }

        if (!(supportFragmentClass != null && supportFragmentClass.isInstance(fragment)) &&
                !(appFragmentClass != null && appFragmentClass.isInstance(fragment)) &&
                !(androidXFragmentClass != null && androidXFragmentClass.isInstance(fragment))) {
            return;
        }

        try {
            JSONObject properties = new JSONObject();
            String screenName = fragment.getClass().getCanonicalName();

            String title = AopUtil.getTitleFromFragment(fragment);

            if (Build.VERSION.SDK_INT >= 11) {
                Activity activity = null;
                try {
                    Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                    if (getActivityMethod != null) {
                        activity = (Activity) getActivityMethod.invoke(fragment);
                    }
                } catch (Exception e) {
                    //ignored
                }
                if (activity != null) {
                    if (TextUtils.isEmpty(title)) {
                        title = TDUtil.getActivityTitle(activity);
                    }
                    screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), screenName);
                }
            }

            if (!TextUtils.isEmpty(title)) {
                properties.put(AopConstants.TITLE, title);
            }

            properties.put(AopConstants.SCREEN_NAME, screenName);
            autoTrack("ta_app_view", properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void appEnterBackground() {
        synchronized (mTrackTimer) {
            try {
                Iterator iterator = mTrackTimer.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    if (entry != null) {
                        if ("ta_app_end".equals(entry.getKey().toString())) {
                            continue;
                        }
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null) {
                            long eventAccumulatedDuration = eventTimer.getEventAccumulatedDuration() + SystemClock.elapsedRealtime() - eventTimer.getStartTime();
                            eventTimer.setEventAccumulatedDuration(eventAccumulatedDuration);
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                TDLog.i(TAG, "appEnterBackground error:" + e.getMessage());
            }
        }
    }

    protected void appBecomeActive() {
        synchronized (mTrackTimer) {
            try {
                Iterator iter = mTrackTimer.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (entry != null) {
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null) {
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                TDLog.i(TAG, "appBecomeActive error:" + e.getMessage());
            }
        }
    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        mAutoTrack = true;
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
        }

        if (eventTypeList.contains(AutoTrackEventType.APP_CRASH)) {
            ExceptionHandler.init();
        }
        mAutoTrackEventTypeList.clear();
        mAutoTrackEventTypeList.addAll(eventTypeList);
    }

    protected void clearLastScreenUrl() {
        if (mClearReferrerWhenAppEnd) {
            mLastScreenUrl = null;
        }
    }

    public String getLastScreenUrl() {
        return mLastScreenUrl;
    }

    public void clearReferrerWhenAppEnd() {
        mClearReferrerWhenAppEnd = true;
    }

    void flush() {
        mMessages.flush();
    }

    private List<Class> mIgnoredViewTypeList = new ArrayList<>();
    public List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

    @Override
    public void ignoreViewType(Class viewType) {
        if (viewType == null) {
            return;
        }

        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        if (!mIgnoredViewTypeList.contains(viewType)) {
            mIgnoredViewTypeList.add(viewType);
        }
    }

    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        if (activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
            return true;
        }

        if (activity.getAnnotation(ThinkingDataIgnoreTrackAppClick.class) != null) {
            return true;
        }

        return false;
    }

    public boolean isButterknifeOnClickEnabled() {
        return mEnableButterknifeOnClick;
    }

    public boolean isTrackFragmentAppViewScreenEnabled() {
        return this.mTrackFragmentAppViewScreen;
    }

    @Override
    public void trackFragmentAppViewScreen() {
        this.mTrackFragmentAppViewScreen = true;
    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        if (!mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            mAutoTrackIgnoredActivities.add(activity.hashCode());
        }
    }

    @Override
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        for (Class<?> activity : activitiesList) {
            if (activity != null && !mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
                mAutoTrackIgnoredActivities.add(activity.hashCode());
            }
        }
    }

    @Override
    public void setViewID(View view, String viewID) {
        if (view != null && !TextUtils.isEmpty(viewID)) {
            view.setTag(R.id.thinking_analytics_tag_view_id, viewID);
        }
    }

    @Override
    public void setViewID(android.app.Dialog view, String viewID) {
        try {
            if (view != null && !TextUtils.isEmpty(viewID)) {
                if (view.getWindow() != null) {
                    view.getWindow().getDecorView().setTag(R.id.thinking_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }

        view.setTag(R.id.thinking_analytics_tag_view_properties, properties);
    }

    @Override
    public void ignoreView(View view) {
        if (view != null) {
            view.setTag(R.id.thinking_analytics_tag_view_ignored, "1");
        }
    }

    @Override
    public void setJsBridge(WebView webView) {
        if (null == webView) {
            TDLog.d(TAG, "setJsBridgeFailed: webView is null");
            return;
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(mContext), "ThinkingData_APP_JS_Bridge");

    }

    /* package */ interface InstanceProcessor {
        public void process(ThinkingAnalyticsSDK m);
    }

    /* package */ static void allInstances(InstanceProcessor processor) {
        synchronized (sInstanceMap) {
            for (final ThinkingAnalyticsSDK instance : sInstanceMap.values()) {
                processor.process(instance);
            }
        }
    }

    private final Context mContext;
    final DataHandle mMessages;

    private int mFlushBulkSize;
    private int mFlushInterval;

    private final String mAppKey;
    private final String mServerUrl;
    private final Map<String, Object> mDeviceInfo;

    private boolean mAutoTrack;
    private List<AutoTrackEventType> mAutoTrackEventTypeList;

    protected int getFlushBulkSize() {
        synchronized (this) {
            return mFlushBulkSize;
        }
    }

    protected int getFlushInterval() {
        synchronized (this) {
            return mFlushInterval;
        }
    }

    protected String getServerUrl() {
        return mServerUrl;
    }

    protected String getAppid() {
        return mAppKey;
    }

    protected Map<String, Object> getDeviceInfo() {
        return mDeviceInfo;
    }
    private final StorageLoginID mLoginId;
    private final StorageIdentifyId mIdentifyId;
    private final StorageRandomID mRandomID;
    private final StorageSuperProperties mSuperProperties;
    private final Map<String, EventTimer> mTrackTimer;
    private int mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI;

    private List<Integer> mAutoTrackIgnoredActivities;
    private JSONObject mLastScreenTrackProperties;
    private String mLastScreenUrl;
    private boolean mClearReferrerWhenAppEnd = false;
    private String mMainProcessName;

    private static volatile ThinkingAnalyticsSDK instance = null;
    private static final Map<Context, ThinkingAnalyticsSDK> sInstanceMap = new HashMap<>();
    private boolean mEnableButterknifeOnClick;
    private boolean mTrackFragmentAppViewScreen;
    public static boolean mEnableTracklog = false;
    private final String mVersionName;
}

