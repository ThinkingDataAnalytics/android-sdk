package com.thinking.analyselibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import com.thinking.analyselibrary.persistence.StorageEnableFlag;
import com.thinking.analyselibrary.persistence.StorageIdentifyId;
import com.thinking.analyselibrary.persistence.StorageLoginID;
import com.thinking.analyselibrary.persistence.StorageOptOutFlag;
import com.thinking.analyselibrary.persistence.StorageRandomID;
import com.thinking.analyselibrary.persistence.StorageSuperProperties;
import com.thinking.analyselibrary.utils.TDConstants;
import com.thinking.analyselibrary.utils.TDUtils;
import com.thinking.analyselibrary.utils.PropertyUtils;
import com.thinking.analyselibrary.utils.TDLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
        return sharedInstance(context, appId, null, false);
    }

    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url) {
        return sharedInstance(context, appId, url, true);
    }

    /**
     *  谨慎使用此接口，大多数情况下会默认绑定老版本数据到第一个实例化的 SDK 中
     * @Param trackOldData 是否绑定老版本(1.2.0 及之前)的数据
     */
    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url, boolean trackOldData) {
        if (null == context) {
            TDLog.d(TAG, "App context is required to get SDK instance.");
            return null;
        }

        if (TextUtils.isEmpty(appId)) {
            TDLog.d(TAG, "APP ID is required to get SDK instance.");
            return null;
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            Map<String, ThinkingAnalyticsSDK> instances = sInstanceMap.get(appContext);

            if (null == instances) {
                instances = new HashMap<>();
                sInstanceMap.put(appContext, instances);
                if (DatabaseAdapter.dbNotExist(appContext)
                        && SystemInformation.getInstance(appContext).hasNotBeenUpdatedSinceInstall()) {
                    sAppFirstInstallationMap.put(appContext, new LinkedList<String>());
                }
            }

            ThinkingAnalyticsSDK instance = instances.get(appId);
            if (null == instance) {
                instance = new ThinkingAnalyticsSDK(appContext,
                        appId,
                        TDConfig.getInstance(appContext, url, appId), trackOldData);
                instances.put(appId, instance);
                if (sAppFirstInstallationMap.containsKey(appContext)) {
                    sAppFirstInstallationMap.get(appContext).add(appId);
                }
            }

            return instance;
        }
    }

    /**
     * only for internal use for automatic test
     * @param instance
     */
    static void addInstance(ThinkingAnalyticsSDK instance, Context context, String appId) {
        synchronized (sInstanceMap) {
            Map<String, ThinkingAnalyticsSDK> instances = sInstanceMap.get(context);
            if (null == instances) {
                instances = new HashMap<>();
                sInstanceMap.put(context, instances);
            }

            instances.put(appId, instance);
        }
    }


    // only the first instance is allowed to bind old data.
    private static boolean isOldDataTracked() {
        synchronized (sInstanceMap) {
            if (sInstanceMap.size() > 0) {
                for (Map<String, ThinkingAnalyticsSDK> instanceMap : sInstanceMap.values()) {
                    for (ThinkingAnalyticsSDK instance : instanceMap.values()) {
                        if (instance.mEnableTrackOldData) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    protected DataHandle getDataHandleInstance(Context context) {
        return DataHandle.getInstance(context);
    }

    /**
     * 初始化SDK
     * @param context APP context
     * @param appId 项目的APP_ID
     * @param config 上报相关配置
     * @param trackOldData 是否上报老数据，并使用之前设置的 account ID
     */
    ThinkingAnalyticsSDK(Context context, String appId, TDConfig config, final boolean trackOldData) {
        mContext = context.getApplicationContext();
        mConfig = config;
        mToken = appId;

        if (null == sStoredSharedPrefs) {
            sStoredSharedPrefs = sPrefsLoader.loadPreferences(context, PREFERENCE_NAME);
            sRandomID = new StorageRandomID(sStoredSharedPrefs);
            sOldLoginId = new StorageLoginID(sStoredSharedPrefs);
            sEnableFlag = new StorageEnableFlag(sStoredSharedPrefs);
        }

        if (trackOldData && !isOldDataTracked()) {
            mEnableTrackOldData = true;
        } else {
            mEnableTrackOldData = false;
        }

        // 获取保存在本地的用户ID和公共属性
        Future<SharedPreferences> storedPrefs = sPrefsLoader.loadPreferences(context, PREFERENCE_NAME + "_" + appId);
        mLoginId = new StorageLoginID(storedPrefs);
        mIdentifyId = new StorageIdentifyId(storedPrefs);
        mSuperProperties = new StorageSuperProperties(storedPrefs);
        mOptOutFlag = new StorageOptOutFlag(storedPrefs);

        mSystemInformation = SystemInformation.getInstance(context);

        mMessages = getDataHandleInstance(context);

        if (mEnableTrackOldData) {
            mMessages.flushOldData(mToken);
        }

        mTrackTimer = new HashMap<>();

        mAutoTrackIgnoredActivities = new ArrayList<>();
        mAutoTrack = mConfig.getAutoTrackConfig();
        mAutoTrackEventTypeList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = (Application) context.getApplicationContext();
            app.registerActivityLifecycleCallbacks(new ThinkingDataActivityLifecycleCallbacks(this, mConfig.getMainProcessName()));
        }

        TDLog.i(TAG, "Thank you very much for using Thinking Data. We will do our best to provide you with the best service.");
        TDLog.i(TAG, String.format("Thinking Data SDK version: %s, APP ID: %s", TDConfig.VERSION, appId));
    }

    public static void enableTrackLog(boolean enableLog) {
        TDLog.setEnableLog(enableLog);
    }

    // H5 与原生 SDK 打通，通过原生 SDK 发送数据
    void trackFromH5(String event) {
        if (hasDisabled()) return;
        if (TextUtils.isEmpty(event)) return;
        try {
            JSONArray data = new JSONObject(event).getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject eventObject = data.getJSONObject(i);

                SimpleDateFormat sDateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
                Date time = sDateFormat.parse(eventObject.getString(TDConstants.KEY_TIME));

                String event_type = eventObject.getString(TDConstants.KEY_TYPE);
                String event_name = null;
                if (event_type.equals(TDConstants.TYPE_TRACK)) {
                    event_name = eventObject.getString(TDConstants.KEY_EVENT_NAME);
                }

                JSONObject properties = eventObject.getJSONObject(TDConstants.KEY_PROPERTIES);
                for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
                    String key = (String) iterator.next();
                    if (key.equals(TDConstants.KEY_ACCOUNT_ID) || key.equals(TDConstants.KEY_DISTINCT_ID) || mSystemInformation.getDeviceInfo().containsKey(key)) {
                        iterator.remove();
                    }
                }

                clickEvent(event_type, event_name, properties, time, true, SAVE_DATA_TO_DATABASE);
            }
        } catch (Exception e) {
            TDLog.w(TAG, "Exception occured when track data from H5.");
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
        if (hasDisabled()) return;
        mConfig.setNetworkType(type);
    }


    void autoTrack(String eventName, JSONObject properties) {
        if (hasDisabled()) return;
        clickEvent(TDConstants.TYPE_TRACK, eventName, properties, new Date(), true, SAVE_DATA_TO_DATABASE);
    }

    @Override
    public void track(String eventName, JSONObject properties) {
        if (hasDisabled()) return;
        clickEvent(TDConstants.TYPE_TRACK, eventName, properties);
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time) {
        if (hasDisabled()) return;
        clickEvent(TDConstants.TYPE_TRACK, eventName, properties, time, false, SAVE_DATA_TO_DATABASE);
    }

    @Override
    public void track(String eventName) {
        if (hasDisabled()) return;
        clickEvent(TDConstants.TYPE_TRACK, eventName, null);
    }

    private void clickEvent(String eventType, String eventName, JSONObject properties) {
        clickEvent(eventType, eventName, properties, new Date(), false, SAVE_DATA_TO_DATABASE);
    }

    /**
     * 上报事件数据和用户属性数据.
     * @param eventType 数据类型：TDConstant.TYPE_*.
     * @param eventName 事件名称
     * @param properties 属性
     * @param time
     * @param isAutoTrack
     * @param saveData
     */
    private void clickEvent(String eventType, String eventName, JSONObject properties, Date time, boolean isAutoTrack, boolean saveData) {
        if (TextUtils.isEmpty(eventType)) {
            TDLog.d(TAG, "EventType could not be empty");
            return;
        }
        if(eventType.equals(TDConstants.TYPE_TRACK)) {
            if(PropertyUtils.isInvalidName(eventName)) {
                TDLog.w(TAG, "Event name[" + eventName + "] is invalid. Event name must be string that starts with English letter, " +
                            "and contains letter, number, and '_'. The max length of the event name is 50.");
                return;
            }
        }

        if (!isAutoTrack && !PropertyUtils.checkProperty(properties)) {
            TDLog.w(TAG, "The data will not be tracked due to properties checking failure: " + properties.toString());
            return;
        }

        try {
            SimpleDateFormat sDateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
            String timeString = (null != time) ? sDateFormat.format(time) : sDateFormat.format(new Date());

            final JSONObject dataObj = new JSONObject();

            dataObj.put(TDConstants.KEY_TYPE, eventType);
            dataObj.put(TDConstants.KEY_TIME, timeString);
            if(eventType.equals(TDConstants.TYPE_TRACK)) {
                dataObj.put(TDConstants.KEY_EVENT_NAME, eventName);
            }

            if (!TextUtils.isEmpty(getLoginId())) {
                dataObj.put(TDConstants.KEY_ACCOUNT_ID, getLoginId());
            }
            dataObj.put(TDConstants.KEY_DISTINCT_ID, getDistinctId());

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
            if(eventType.equals(TDConstants.TYPE_TRACK)) {
                synchronized (mSuperProperties) {
                    JSONObject superProperties = mSuperProperties.get();
                    TDUtils.mergeJSONObject(superProperties, finalProperties);
                }

                try {
                    if (mDynamicSuperPropertiesTracker != null) {
                        JSONObject dynamicSuperProperties = mDynamicSuperPropertiesTracker.getDynamicSuperProperties();
                        if (dynamicSuperProperties != null && PropertyUtils.checkProperty(dynamicSuperProperties)) {
                            TDUtils.mergeJSONObject(dynamicSuperProperties, finalProperties);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 用户设置的事件属性会覆盖公共属性
            TDUtils.mergeJSONObject(sendProps, finalProperties);

            if(eventType.equals(TDConstants.TYPE_TRACK)) {
                finalProperties.put(TDConstants.KEY_NETWORK_TYPE, mSystemInformation.getNetworkType());
                if (!TextUtils.isEmpty(mSystemInformation.getAppVersionName())) {
                    finalProperties.put(TDConstants.KEY_APP_VERSION, mSystemInformation.getAppVersionName());
                }

                final EventTimer eventTimer;
                synchronized (mTrackTimer) {
                    eventTimer = mTrackTimer.get(eventName);
                    mTrackTimer.remove(eventName);
                }

                if (null != eventTimer) {
                    try {
                        Double duration = Double.valueOf(eventTimer.duration());
                        if (duration > 0) {
                            finalProperties.put(TDConstants.KEY_DURATION, duration);
                        }
                    } catch (JSONException e) {
                        // ignore
                        e.printStackTrace();
                    }
                }
            }

            dataObj.put(TDConstants.KEY_PROPERTIES, finalProperties);
            if (saveData) {
                mMessages.saveClickData(dataObj, mToken);
            } else {
                mMessages.postClickData(dataObj, mToken);
            }
        } catch (Exception e) {
            TDLog.w(TAG, "Exception occurred in track data: " + eventType + ": " + properties);
            e.printStackTrace();
        }
    }

    @Override
    public void user_add(String propertyName, Number propertyValue) {
        if (hasDisabled()) return;
        try {
            if (null == propertyValue) {
                TDLog.d(TAG, "user_add value must be Number");
            } else {
                JSONObject json = new JSONObject();
                json.put(propertyName, propertyValue);
                clickEvent(TDConstants.TYPE_USER_ADD, null, json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void user_add(JSONObject property) {
        if (hasDisabled()) return;
        clickEvent(TDConstants.TYPE_USER_ADD, null, property);
    }

    @Override
    public void user_setOnce(JSONObject property) {
        if (hasDisabled()) return;
        clickEvent(TDConstants.TYPE_USER_SET_ONCE, null, property);
    }

    @Override
    public void user_set(JSONObject property) {
        if (hasDisabled()) return;
        clickEvent(TDConstants.TYPE_USER_SET, null, property);
    }

    @Override
    public void user_delete() {
        if (hasDisabled()) return;
        clickEvent(TDConstants.TYPE_USER_DEL, null, null);
    }

    @Override
    public void identify(String identity) {
        if (hasDisabled()) return;
        if (TextUtils.isEmpty(identity)) {
            TDLog.w(TAG,"The identity cannot be empty.");
            return;
        }

        synchronized (mIdentifyId) {
            mIdentifyId.put(identity);
        }
    }

    @Override
    public void login(String loginId) {
        if (hasDisabled()) return;
        try {
            if(TextUtils.isEmpty(loginId)) {
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
        if (hasDisabled()) return;
        try {
            synchronized (mLoginId) {
                mLoginId.put(null);
                if (mEnableTrackOldData) {
                    synchronized (sOldLoginIdLock) {
                        if (!TextUtils.isEmpty(sOldLoginId.get())) {
                            sOldLoginId.put(null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLoginId() {
        synchronized (mLoginId) {
            String loginId = mLoginId.get();
            if (TextUtils.isEmpty(loginId) && mEnableTrackOldData) {
                synchronized (sOldLoginIdLock) {
                    loginId = sOldLoginId.get();
                    if (!TextUtils.isEmpty(loginId)) {
                        mLoginId.put(loginId);
                        sOldLoginId.put(null);
                    }
                }
            }
            return loginId;
        }
    }

    private String getRandomID() {
        synchronized (sRandomIDLock) {
            return sRandomID.get();
        }
    }

    private String getIdentifyID() {
        synchronized (mIdentifyId) {
            return mIdentifyId.get();
        }
    }

    @Override
    public String getDistinctId() {
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
    public JSONObject getSuperProperties() {
        synchronized (mSuperProperties) {
            return mSuperProperties.get();
        }
    }

    @Override
    public void setSuperProperties(JSONObject superProperties) {
        if (hasDisabled()) return;
        try {
            if (superProperties == null || !PropertyUtils.checkProperty(superProperties)) {
                return;
            }

            synchronized (mSuperProperties) {
                JSONObject properties = mSuperProperties.get();
                TDUtils.mergeJSONObject(superProperties, properties);
                mSuperProperties.put(properties);
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
        if (hasDisabled()) return;
        mDynamicSuperPropertiesTracker = dynamicSuperPropertiesTracker;
    }

    @Override
    public void unsetSuperProperty(String superPropertyName) {
        if (hasDisabled()) return;
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
        if (hasDisabled()) return;
        synchronized (mSuperProperties) {
            mSuperProperties.put(new JSONObject());
        }
    }

    @Override
    public void timeEvent(final String eventName) {
        if (hasDisabled()) return;
        try {
            if(PropertyUtils.isInvalidName(eventName)) {
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

        ThinkingDataIgnoreTrackAppViewScreenAndAppClick annotation1 =
                activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreenAndAppClick.class);
        if (null != annotation1 && (TextUtils.isEmpty(annotation1.appId()) ||
                mToken.equals(annotation1.appId()))) {
            return true;
        }

        ThinkingDataIgnoreTrackAppViewScreen annotation2 = activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class);
        if (null != annotation2 && (TextUtils.isEmpty(annotation2.appId()) ||
                mToken.equals(annotation2.appId()))) {
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
        if (hasDisabled()) return false;
        return mAutoTrack;
    }

    public enum AutoTrackEventType {
        APP_START(TDConstants.APP_START_EVENT_NAME),
        APP_END(TDConstants.APP_END_EVENT_NAME),
        APP_CLICK(TDConstants.APP_CLICK_EVENT_NAME),
        APP_VIEW_SCREEN(TDConstants.APP_VIEW_EVENT_NAME),
        APP_CRASH(TDConstants.APP_CRASH_EVENT_NAME),
        APP_INSTALL(TDConstants.APP_INSTALL_EVENT_NAME);

        private final String eventName;

        public static AutoTrackEventType autoTrackEventTypeFromEventName(String eventName) {
            if (TextUtils.isEmpty(eventName)) {
                return null;
            }

            switch (eventName) {
                case TDConstants.APP_START_EVENT_NAME:
                    return APP_START;
                case TDConstants.APP_END_EVENT_NAME:
                    return APP_END;
                case TDConstants.APP_CLICK_EVENT_NAME:
                    return APP_CLICK;
                case TDConstants.APP_VIEW_EVENT_NAME:
                    return APP_VIEW_SCREEN;
                case TDConstants.APP_CRASH_EVENT_NAME:
                    return APP_CRASH;
                case TDConstants.APP_INSTALL_EVENT_NAME:
                    return APP_INSTALL;
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
        if (hasDisabled()) return;
        try {
            if (checkProperties && !PropertyUtils.checkProperty(properties)) {
                TDLog.d(TAG, "Properties is not valid");
                return;
            }
            if ((!TextUtils.isEmpty(url) || properties != null)) {
                JSONObject trackProperties = new JSONObject();
                mLastScreenTrackProperties = properties;

                if (!TextUtils.isEmpty(mLastScreenUrl)) {
                    trackProperties.put(TDConstants.KEY_REFERRER, mLastScreenUrl);
                }

                trackProperties.put(TDConstants.KEY_URL, url);
                mLastScreenUrl = url;
                if (properties != null) {
                    TDUtils.mergeJSONObject(properties, trackProperties);
                }
                autoTrack(TDConstants.APP_VIEW_EVENT_NAME, trackProperties);
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
        if (hasDisabled()) return;
        try {
            if (activity == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            properties.put(TDConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            TDUtils.getScreenNameAndTitleFromActivity(properties, activity);

            if (activity instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;

                String screenUrl = screenAutoTracker.getScreenUrl();
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    TDUtils.mergeJSONObject(otherProperties, properties);
                }

                trackViewScreenInternal(screenUrl, properties, false);
            } else {
                autoTrack(TDConstants.APP_VIEW_EVENT_NAME, properties);
            }
        } catch (Exception e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }

    @Override
    public void trackViewScreen(android.app.Fragment fragment) {
        if (hasDisabled()) return;
        try {
            if (fragment == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            String fragmentName = fragment.getClass().getCanonicalName();
            String screenName = fragmentName;
            String title = TDUtils.getTitleFromFragment(fragment, mToken);

            if (Build.VERSION.SDK_INT >= 11) {
                Activity activity = fragment.getActivity();
                if (activity != null) {
                    if (TextUtils.isEmpty(title)) {
                        title = TDUtils.getActivityTitle(activity);
                    }
                    screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName);
                }
            }

            if (!TextUtils.isEmpty(title)) {
                properties.put(TDConstants.TITLE, title);
            }

            properties.put(TDConstants.SCREEN_NAME, screenName);
            autoTrack(TDConstants.APP_VIEW_EVENT_NAME, properties);
        } catch (Exception e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }


    @Override
    public void trackViewScreen(final Object fragment) {
        if (hasDisabled()) return;
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

            String title = TDUtils.getTitleFromFragment(fragment, mToken);

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
                        title = TDUtils.getActivityTitle(activity);
                    }
                    screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), screenName);
                }
            }

            if (!TextUtils.isEmpty(title)) {
                properties.put(TDConstants.TITLE, title);
            }

            properties.put(TDConstants.SCREEN_NAME, screenName);
            autoTrack(TDConstants.APP_VIEW_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void appEnterBackground() {
        if (hasDisabled()) return;
        synchronized (mTrackTimer) {
            try {
                Iterator iterator = mTrackTimer.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    if (entry != null) {
                        if (TDConstants.APP_END_EVENT_NAME.equals(entry.getKey().toString())) {
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
        if (hasDisabled()) return;
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

    // used by unity SDK.
    public void trackAppInstall() {
        if (hasDisabled()) return;
        enableAutoTrack(new ArrayList<>(Arrays.asList(AutoTrackEventType.APP_INSTALL)));
    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        if (hasDisabled()) return;
        mAutoTrack = true;
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
        }

        if (eventTypeList.contains(AutoTrackEventType.APP_CRASH)) {
            mTrackCrash = true;
            ExceptionHandler.init();
        }

        if (eventTypeList.contains(AutoTrackEventType.APP_INSTALL))  {
            synchronized (sInstanceMap) {
                if (sAppFirstInstallationMap.containsKey(mContext) &&
                        sAppFirstInstallationMap.get(mContext).contains(mToken)) {
                    track(TDConstants.APP_INSTALL_EVENT_NAME);
                    sAppFirstInstallationMap.get(mContext).remove(mToken);
                }
            }
        }

        mAutoTrackEventTypeList.clear();
        mAutoTrackEventTypeList.addAll(eventTypeList);
    }

    protected void clearLastScreenUrl() {
        if (hasDisabled()) return;
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

    public void flush() {
        if (hasDisabled()) return;
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
        if (hasDisabled()) return;
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

        ThinkingDataIgnoreTrackAppViewScreenAndAppClick annotation1 =
                activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreenAndAppClick.class);
        if (null != annotation1 && (TextUtils.isEmpty(annotation1.appId()) ||
                mToken.equals(annotation1.appId()))) {
            return true;
        }

        ThinkingDataIgnoreTrackAppClick annotation2 = activity.getAnnotation(ThinkingDataIgnoreTrackAppClick.class);
        if (null != annotation2 && (TextUtils.isEmpty(annotation2.appId()) ||
                mToken.equals(annotation2.appId()))) {
            return true;
        }

        return false;
    }

    public boolean isTrackFragmentAppViewScreenEnabled() {
        return this.mTrackFragmentAppViewScreen;
    }

    @Override
    public void trackFragmentAppViewScreen() {
        if (hasDisabled()) return;
        this.mTrackFragmentAppViewScreen = true;
    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        if (hasDisabled()) return;
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
        if (hasDisabled()) return;
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
        if (hasDisabled()) return;
        if (view != null && !TextUtils.isEmpty(viewID)) {
            TDUtils.setTag(mToken, view, R.id.thinking_analytics_tag_view_id, viewID);
        }
    }

    @Override
    public void setViewID(android.app.Dialog view, String viewID) {
        if (hasDisabled()) return;
        try {
            if (view != null && !TextUtils.isEmpty(viewID)) {
                if (view.getWindow() != null) {
                    TDUtils.setTag(mToken, view.getWindow().getDecorView(), R.id.thinking_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void setViewProperties(View view, JSONObject properties) {
        if (hasDisabled()) return;
        if (view == null || properties == null) {
            return;
        }
        TDUtils.setTag(mToken, view, R.id.thinking_analytics_tag_view_properties, properties);
    }

    @Override
    public void ignoreView(View view) {
        if (hasDisabled()) return;
        if (view != null) {
            TDUtils.setTag(mToken, view, R.id.thinking_analytics_tag_view_ignored, "1");
        }
    }

    @Override
    public void setJsBridge(WebView webView) {
        if (null == webView) {
            TDLog.d(TAG, "SetJsBridge failed due to parameter webView is null");
            return;
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "ThinkingData_APP_JS_Bridge");

    }

    @Override
    public void setJsBridgeForX5WebView(Object x5WebView) {
        if (x5WebView == null) {
            TDLog.d(TAG, "SetJsBridge failed due to parameter webView is null");
            return;
        }

        try {
            Class<?> clazz = x5WebView.getClass();
            Method addJavascriptInterface = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            if (addJavascriptInterface == null) {
                return;
            }

            addJavascriptInterface.invoke(x5WebView, new WebAppInterface(this), "ThinkingData_APP_JS_Bridge");
        } catch (Exception e) {
            TDLog.w(TAG, "setJsBridgeForX5WebView failed: " +  e.toString());
        }

    }

    @Override
    public String getDeviceId() {
        if (mSystemInformation.getDeviceInfo().containsKey(TDConstants.KEY_DEVICE_ID)) {
            return (String) mSystemInformation.getDeviceInfo().get(TDConstants.KEY_DEVICE_ID);
        } else {
            return null;
        }
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
        if (hasDisabled()) return false;
        return mTrackCrash;
    }


    /**
     * 打开/关闭 SDK 上报功能. 当关闭 SDK 功能时，之前的缓存数据会保留，并继续上报; 但是不会追踪之后的数据和改动.
     * @param enabled boolean 是否打开 SDK 功能.
     */
    public static void enableTracking(boolean enabled) {
        TDLog.d(TAG, "enableTracking: " + enabled);
        sEnableFlag.put(enabled);
    }

    /**
     * 停止上报此用户数据，并且发送 user_del (不会重试)
     */
    public void optOutTrackingAndDeleteUser() {
        clickEvent(TDConstants.TYPE_USER_DEL, null, null, null, false, false);
        optOutTracking();
    }

    /**
     * 停止上报此用户的数据. 调用此接口之后，会删除本地缓存数据和之前设置; 后续的上报和设置都无效.
     */
    public void optOutTracking() {
        TDLog.d(TAG, "optOutTracking..." );
        mOptOutFlag.put(true);
        mMessages.emptyMessageQueue(mToken);

        synchronized (mTrackTimer) {
            mTrackTimer.clear();
        }

        mIdentifyId.put(null);
        mLoginId.put(null);
        synchronized (mSuperProperties) {
            mSuperProperties.put(new JSONObject());
        }
    }

    /**
     * 允许此实例的上报.
     */
    public void optInTracking() {
        TDLog.d(TAG, "optInTracking..." );
        mOptOutFlag.put(false);
        mMessages.flush(mToken);
    }

    public static boolean isEnabled() {
        return sEnableFlag.get();
    }

    private boolean hasDisabled() {
        return !isEnabled() || hasOptOut();
    }

    public boolean hasOptOut() {
        return mOptOutFlag.get();
    }

    private final DataHandle mMessages;

    private final String mToken;
    private TDConfig mConfig;
    private SystemInformation mSystemInformation;

    private boolean mAutoTrack;
    private boolean mTrackCrash;
    private List<AutoTrackEventType> mAutoTrackEventTypeList;

    public String getToken() {
        return mToken;
    }

    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static Future<SharedPreferences> sStoredSharedPrefs;
    private static final String PREFERENCE_NAME = "com.thinkingdata.analyse";

    private final StorageLoginID mLoginId;
    private static StorageLoginID sOldLoginId;
    private static final Object sOldLoginIdLock = new Object();
    private final StorageIdentifyId mIdentifyId;
    private static StorageRandomID sRandomID;
    private static StorageEnableFlag sEnableFlag;
    private final StorageOptOutFlag mOptOutFlag;
    private static final Object sRandomIDLock = new Object();
    private final StorageSuperProperties mSuperProperties;

    private DynamicSuperPropertiesTracker mDynamicSuperPropertiesTracker;
    private final Map<String, EventTimer> mTrackTimer;

    private List<Integer> mAutoTrackIgnoredActivities;
    private JSONObject mLastScreenTrackProperties;
    private String mLastScreenUrl;
    private boolean mClearReferrerWhenAppEnd = false;

    private static final Map<Context, Map<String, ThinkingAnalyticsSDK>> sInstanceMap = new HashMap<>();
    private static final Map<Context, List<String>> sAppFirstInstallationMap = new HashMap<>();
    private boolean mTrackFragmentAppViewScreen;
    private final boolean mEnableTrackOldData; // 是否同步老版本数据
    private Context mContext;

    private final boolean SAVE_DATA_TO_DATABASE = true;
}
