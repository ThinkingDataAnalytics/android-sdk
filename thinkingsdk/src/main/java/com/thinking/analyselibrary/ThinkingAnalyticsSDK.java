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
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
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

    private static final String TAG = "ThinkingAnalyticsSDK";

    /**
     * 获取默认SDK实例，适合在只有一个实例的情况下使用
     * @return 第一个可用的SDK实例
     */
    @Deprecated
    public static ThinkingAnalyticsSDK sharedInstance() {
        synchronized (sInstanceMap) {
            if (sInstanceMap.size() > 0) {
                Iterator<Map<String,ThinkingAnalyticsSDK>> iterator = sInstanceMap.values().iterator();
                if (iterator.hasNext()) {
                    Map<String, ThinkingAnalyticsSDK> instanceMap = iterator.next();
                    if (instanceMap.size() > 0) {
                        return instanceMap.values().iterator().next();
                    }
                }
            }
            return null;
        }
    }

    @Deprecated
    public static ThinkingAnalyticsSDK sharedInstance(Context context) {
        if (null == context) {
            return null;
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();
            Map<String, ThinkingAnalyticsSDK> instanceMap = sInstanceMap.get(appContext);
            ThinkingAnalyticsSDK instance = null;
            if (instanceMap.size() > 0) {
                instance = instanceMap.values().iterator().next();
            }

            if (null == instance) {
                TDLog.d(TAG,"Please call method ThinkingAnalyticsSDK.sharedInstance(" +
                        "Context context,String appKey, String serverURL, String url) first ");
            }
            return instance;
        }
    }

    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId) {
        return sharedInstance(context, appId, null);

    }

    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url) {
        if (null == context) {
            TDLog.d(TAG, "param context is null");
            return null;
        }

        synchronized (sInstanceMap) {

            final String prefsName = "com.thinkingdata.analyse";
            if (null == sStoredPrefs) {
                sStoredPrefs = sPrefsLoader.loadPreferences(context, prefsName);
            }

            final Context appContext = context.getApplicationContext();

            Map<String, ThinkingAnalyticsSDK> instances = sInstanceMap.get(appContext);

            if (null == instances) {
                instances = new HashMap<>();
                sInstanceMap.put(appContext, instances);
            }

            ThinkingAnalyticsSDK instance = instances.get(appId);
            if (null == instance && !TextUtils.isEmpty(url)) {
                instance = new ThinkingAnalyticsSDK(appContext,
                        appId,
                        TDConfig.getInstance(appContext, url, appId));
                instances.put(appId, instance);
            }

            return instance;
        }
    }

    /**
     * 初始化SDK
     * @param context APP context
     * @param appId 项目的APP_ID
     * @param config 上报相关配置
     */
    ThinkingAnalyticsSDK(Context context, String appId, TDConfig config) {
        mContext = context;
        final String packageName = context.getApplicationContext().getPackageName();
        mToken = appId;
        mConfig = config;
        mVersionName = TDUtil.getVersionName(mContext);
        final Map<String, Object> deviceInfo = TDUtil.getDeviceInfo(mContext);
        mDeviceInfo = Collections.unmodifiableMap(deviceInfo);

        mMessages = DataHandle.getInstance(mContext, new JSONObject(mDeviceInfo));

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

        // 获取保存在本地的用户ID和公共属性
        sLoginId = new StorageLoginID(sStoredPrefs);
        sIdentifyId = new StorageIdentifyId(sStoredPrefs);
        sRandomID = new StorageRandomID(sStoredPrefs);
        sSuperProperties = new StorageSuperProperties(sStoredPrefs);

        TDLog.i(TAG, "Thank you very much for using Thinking Data. We will do our best to provide you with the best service.");
        TDLog.i(TAG, String.format("Thinking Data SDK version:%s", TDConfig.VERSION));
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


    public enum ThinkingdataNetworkType {
        NETWORKTYPE_DEFAULT,
        NETWORKTYPE_WIFI,
        NETWORKTYPE_ALL
    }

    @Override
    public void setNetworkType(ThinkingdataNetworkType type) {
        mConfig.setNetworkType(type);
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
                synchronized (sSuperProperties) {
                    JSONObject superProperties = sSuperProperties.get();
                    TDUtil.mergeJSONObject(superProperties, finalProperties);
                }

                try {
                    if (mDynamicSuperPropertiesTracker != null) {
                        JSONObject dynamicSuperProperties = mDynamicSuperPropertiesTracker.getDynamicSuperProperties();
                        if (dynamicSuperProperties != null && PropertyUtils.checkProperty(dynamicSuperProperties)) {
                            TDUtil.mergeJSONObject(dynamicSuperProperties, finalProperties);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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


            mMessages.saveClickData(dataObj, mToken);
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

        synchronized (sIdentifyId) {
            sIdentifyId.put(identify);
        }
    }

    @Override
    public void login(String loginId) {
        try {
            if(TDUtil.checkNull(loginId)) {
                TDLog.d(TAG,"login_id cannot be empty.");
                return;
            }

            synchronized (sLoginId) {
                if (!loginId.equals(sLoginId.get())) {
                    sLoginId.put(loginId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logout() {
        try {
            synchronized (sLoginId) {
                sLoginId.put(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLoginId() {
        synchronized (sLoginId) {
            return sLoginId.get();
        }
    }

    private String getRandomID() {
        synchronized (sRandomID) {
            return sRandomID.get();
        }
    }

    private String getIdentifyID() {
        synchronized (sIdentifyId) {
            return sIdentifyId.get();
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
        synchronized (sSuperProperties) {
            return sSuperProperties.get();
        }
    }

    @Override
    public void setSuperProperties(JSONObject superProperties) {
        try {
            if (superProperties == null || !PropertyUtils.checkProperty(superProperties)) {
                return;
            }

            synchronized (sSuperProperties) {
                JSONObject properties = sSuperProperties.get();
                TDUtil.mergeJSONObject(superProperties, properties);
                sSuperProperties.put(properties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface DynamicSuperPropertiesTracker {
        JSONObject getDynamicSuperProperties();
    }

    @Override
    public void setDynamicSuperPropertiesTracker(DynamicSuperPropertiesTracker dynamicSuperPropertiesTracker) {
        mDynamicSuperPropertiesTracker = dynamicSuperPropertiesTracker;
    }

    @Override
    public void unsetSuperProperty(String superPropertyName) {
        try {
            if (superPropertyName == null) {
                return;
            }
            synchronized (sSuperProperties) {
                JSONObject superProperties = sSuperProperties.get();
                superProperties.remove(superPropertyName);
                sSuperProperties.put(superProperties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearSuperProperties() {
        synchronized (sSuperProperties) {
            sSuperProperties.put(new JSONObject());
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
            mTrackCrash = true;
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
        mMessages.flush(mToken);
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
        webView.addJavascriptInterface(new WebAppInterface(this), "ThinkingData_APP_JS_Bridge");

    }

    /* package */ interface InstanceProcessor {
        public void process(ThinkingAnalyticsSDK instance);
    }

    /* package */ static void allInstances(InstanceProcessor processor) {
        synchronized (sInstanceMap) {
            for (final Map<String, ThinkingAnalyticsSDK> instances : sInstanceMap.values()) {
                for (final ThinkingAnalyticsSDK instance : instances.values()) {
                    processor.process(instance);
                }
            }
        }
    }


    boolean shouldTrackCrash() {
        return mTrackCrash;
    }

    private final Context mContext;
    final DataHandle mMessages;

    private final String mToken;
    private TDConfig mConfig;
    private final Map<String, Object> mDeviceInfo;

    private boolean mAutoTrack;
    private boolean mTrackCrash;
    private List<AutoTrackEventType> mAutoTrackEventTypeList;

    protected String getAppid() {
        return mToken;
    }

    protected Map<String, Object> getDeviceInfo() {
        return mDeviceInfo;
    }

    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static Future<SharedPreferences> sStoredPrefs;

    private static StorageLoginID sLoginId;
    private static StorageIdentifyId sIdentifyId;
    private static StorageRandomID sRandomID;
    private static StorageSuperProperties sSuperProperties;

    private DynamicSuperPropertiesTracker mDynamicSuperPropertiesTracker;
    private final Map<String, EventTimer> mTrackTimer;

    private List<Integer> mAutoTrackIgnoredActivities;
    private JSONObject mLastScreenTrackProperties;
    private String mLastScreenUrl;
    private boolean mClearReferrerWhenAppEnd = false;
    private String mMainProcessName;

    //private static final Map<Context, ThinkingAnalyticsSDK> sInstanceMap = new HashMap<>();
    private static final Map<Context, Map<String, ThinkingAnalyticsSDK>> sInstanceMap = new HashMap<>();
    private boolean mTrackFragmentAppViewScreen;
    public static boolean mEnableTracklog = false;
    private final String mVersionName;
    private boolean mEnableButterknifeOnClick;
}
