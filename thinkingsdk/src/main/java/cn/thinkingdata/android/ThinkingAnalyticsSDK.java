package cn.thinkingdata.android;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import cn.thinkingdata.android.persistence.StorageEnableFlag;
import cn.thinkingdata.android.persistence.StorageIdentifyId;
import cn.thinkingdata.android.persistence.StorageLoginID;
import cn.thinkingdata.android.persistence.StorageOptOutFlag;
import cn.thinkingdata.android.persistence.StorageRandomID;
import cn.thinkingdata.android.persistence.StorageSuperProperties;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDUtils;
import cn.thinkingdata.android.utils.PropertyUtils;
import cn.thinkingdata.android.utils.TDLog;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ThinkingAnalyticsSDK implements IThinkingAnalyticsAPI {

    /**
     * 当 SDK 初始化完成后，可以通过此接口获得保存的单例
     * @param context app context
     * @param appId APP ID
     * @return SDK 实例
     */
    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId) {
        return sharedInstance(context, appId, null, false);
    }

    /**
     * 初始化 SDK. 在调用此接口之前，track 功能不可用.
     * @param context app context
     * @param appId APP ID
     * @param url 接收端地址
     * @return SDK 实例
     */
    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url) {
        return sharedInstance(context, appId, url, true);
    }

    /**
     *  谨慎使用此接口，大多数情况下会默认绑定老版本数据到第一个实例化的 SDK 中
     * @param trackOldData 是否绑定老版本(1.2.0 及之前)的数据
     */
    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url, boolean trackOldData) {
        if (null == context) {
            TDLog.w(TAG, "App context is required to get SDK instance.");
            return null;
        }

        if (TextUtils.isEmpty(appId)) {
            TDLog.w(TAG, "APP ID is required to get SDK instance.");
            return null;
        }

        TDConfig config = TDConfig.getInstance(context, appId, url);
        if (null == config) {
            TDLog.w(TAG, "Cannot get valid TDConfig instance. Returning null");
            return null;
        }
        config.setTrackOldData(trackOldData);

        return sharedInstance(config);
    }

    public static ThinkingAnalyticsSDK sharedInstance(TDConfig config) {
        if (null == config) {
            TDLog.w(TAG, "Cannot initial SDK instance with null config instance.");
            return null;
        }

        synchronized (sInstanceMap) {

            Map<String, ThinkingAnalyticsSDK> instances = sInstanceMap.get(config.mContext);

            if (null == instances) {
                instances = new HashMap<>();
                sInstanceMap.put(config.mContext, instances);
                if (DatabaseAdapter.dbNotExist(config.mContext)
                        && SystemInformation.getInstance(config.mContext).hasNotBeenUpdatedSinceInstall()) {
                    sAppFirstInstallationMap.put(config.mContext, new LinkedList<String>());
                }
                TDQuitSafelyService.getInstance(config.mContext).start();
            }

            ThinkingAnalyticsSDK instance = instances.get(config.mToken);
            if (null == instance) {
                instance = new ThinkingAnalyticsSDK(config);
                instances.put(config.mToken, instance);
                if (sAppFirstInstallationMap.containsKey(config.mContext)) {
                    sAppFirstInstallationMap.get(config.mContext).add(config.mToken);
                }
            }
            return instance;
        }
    }

     // only for automatic test
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
     * SDK 构造函数，需要传入 TDConfig 实例. 用户可以获取 TDConfig 实例， 并做相关配置后初始化 SDK.
     * @param config TDConfig 实例
     * @param light 是否是轻实例（内部使用)
     */
    ThinkingAnalyticsSDK(TDConfig config, boolean... light) {
        mConfig = config;

        if (light.length > 0 && light[0]) {
            mLoginId = null;
            mIdentifyId = null;
            mSuperProperties = null;
            mOptOutFlag = null;
            mEnableFlag = null;
            mEnableTrackOldData = false;
            mTrackTimer = new HashMap<>();
            mMessages = getDataHandleInstance(config.mContext);
            mSystemInformation = SystemInformation.getInstance(config.mContext);
            return;
        }

        if (null == sStoredSharedPrefs) {
            sStoredSharedPrefs = sPrefsLoader.loadPreferences(config.mContext, PREFERENCE_NAME);
            sRandomID = new StorageRandomID(sStoredSharedPrefs);
            sOldLoginId = new StorageLoginID(sStoredSharedPrefs);
        }

        if (config.trackOldData() && !isOldDataTracked()) {
            mEnableTrackOldData = true;
        } else {
            mEnableTrackOldData = false;
        }

        // 获取保存在本地的用户ID和公共属性
        Future<SharedPreferences> storedPrefs = sPrefsLoader.loadPreferences(config.mContext, PREFERENCE_NAME + "_" + config.mToken);
        mLoginId = new StorageLoginID(storedPrefs);
        mIdentifyId = new StorageIdentifyId(storedPrefs);
        mSuperProperties = new StorageSuperProperties(storedPrefs);
        mOptOutFlag = new StorageOptOutFlag(storedPrefs);
        mEnableFlag = new StorageEnableFlag(storedPrefs);

        mSystemInformation = SystemInformation.getInstance(config.mContext);

        mMessages = getDataHandleInstance(config.mContext);

        if (mEnableTrackOldData) {
            mMessages.flushOldData(config.mToken);
        }

        mTrackTimer = new HashMap<>();

        mAutoTrackIgnoredActivities = new ArrayList<>();
        mAutoTrackEventTypeList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = (Application) config.mContext.getApplicationContext();
            app.registerActivityLifecycleCallbacks(new ThinkingDataActivityLifecycleCallbacks(this, mConfig.getMainProcessName()));
        }

        if (!config.isNormal()) {
            enableTrackLog(true);
        }

        TDLog.i(TAG, "Thank you very much for using Thinking Data. We will do our best to provide you with the best service.");
        TDLog.i(TAG, String.format("Thinking Data SDK version: %s, Initial mode: %s, APP ID ends with: %s, DeviceId: %s", TDConfig.VERSION,
                config.getMode().name(), config.mToken.substring(config.mToken.length() - 4), getDeviceId()));
    }

    /**
     * 打开/关闭 日志打印
     * @param enableLog true 打开日志; false 关闭日志
     */
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

                String time = eventObject.getString(TDConstants.KEY_TIME);
                double zoneOffset = 0.1;
                TIME_VALUE_TYPE timeValueType = TIME_VALUE_TYPE.TIME_ONLY;
                if (eventObject.has(TDConstants.KEY_ZONE_OFFSET)) {
                    zoneOffset = eventObject.getDouble(TDConstants.KEY_ZONE_OFFSET);
                    timeValueType = TIME_VALUE_TYPE.ALL;
                }

                String eventType = eventObject.getString(TDConstants.KEY_TYPE);

                JSONObject properties = eventObject.getJSONObject(TDConstants.KEY_PROPERTIES);
                for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
                    String key = (String) iterator.next();
                    if (key.equals(TDConstants.KEY_ACCOUNT_ID) || key.equals(TDConstants.KEY_DISTINCT_ID) || mSystemInformation.getDeviceInfo().containsKey(key)) {
                        iterator.remove();
                    }
                }

                DataDescription dataDescription;
                if (eventType.equals(TDConstants.TYPE_TRACK)) {
                    String eventName = eventObject.getString(TDConstants.KEY_EVENT_NAME);
                    dataDescription = new EventDescription(eventName, properties);
                } else {
                    dataDescription = new DataDescription(eventType, properties);
                }

                dataDescription.setTime(time, zoneOffset, timeValueType);
                dataDescription.setAutoTrackFlag();
                trackInternal(dataDescription);
            }
        } catch (Exception e) {
            TDLog.w(TAG, "Exception occurred when track data from H5.");
            e.printStackTrace();
        }
    }

    /**
     * 允许上报的网络类型
     */
    public enum ThinkingdataNetworkType {
        /** 默认设置，在3G、4G、5G、WiFi 环境下上报数据 */
        NETWORKTYPE_DEFAULT,
        /** 只在 WiFi 环境上报数据 */
        NETWORKTYPE_WIFI,
        /** 在所有网络类型中上报 */
        NETWORKTYPE_ALL
    }

    @Override
    public void setNetworkType(ThinkingdataNetworkType type) {
        if (hasDisabled()) return;
        mConfig.setNetworkType(type);
    }

    // used by unity SDK. Unity 2019.2.1f1 doesn't support enum type.
    public void setNetworkType(int type) {
        ThinkingdataNetworkType networkType;
        switch (type) {
            case 0: networkType = ThinkingdataNetworkType.NETWORKTYPE_DEFAULT; break;
            case 1: networkType = ThinkingdataNetworkType.NETWORKTYPE_WIFI; break;
            case 2: networkType = ThinkingdataNetworkType.NETWORKTYPE_ALL; break;
            default: return;
        }
        setNetworkType(networkType);
    }

    // autoTrack is used internal without property checking.
    void autoTrack(String eventName, JSONObject properties) {
        if (hasDisabled()) return;
        EventDescription event = new EventDescription(eventName, properties);
        event.setAutoTrackFlag();
        trackInternal(event);
    }

    @Override
    public void track(String eventName, JSONObject properties) {
        if (hasDisabled()) return;
        trackInternal(new EventDescription(eventName, properties));
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time) {
        if (hasDisabled()) return;
        EventDescription event = new EventDescription(eventName, properties);

        SimpleDateFormat sDateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
        sDateFormat.setTimeZone(mConfig.getDefaultTimeZone());
        String timeString = sDateFormat.format(time);
        event.setTime(timeString, 0.1, TIME_VALUE_TYPE.TIME_ONLY);
        trackInternal(event);
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time, TimeZone timeZone) {
        if (hasDisabled()) return;
        if (null == timeZone) {
            track(eventName, properties, time);
            return;
        }
        EventDescription event = new EventDescription(eventName, properties);
        SimpleDateFormat sDateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
        sDateFormat.setTimeZone(timeZone);
        String timeString = sDateFormat.format(time);
        event.setTime(timeString, TDUtils.getTimezoneOffset(time.getTime(), timeZone), TIME_VALUE_TYPE.ALL);

        trackInternal(event);
    }

    @Override
    public void track(String eventName) {
        if (hasDisabled()) return;
        trackInternal(new EventDescription(eventName, null));
    }

    // DataDescription 中时间的类型
    private enum TIME_VALUE_TYPE {
        NONE,
        TIME_ONLY,
        ALL
    };

    private class DataDescription {

        String type; // 数据类型
        String eventName; // 事件名称，如果有
        JSONObject properties; // 属性

        String timeString; // 时间字符串
        double zoneOffset; // 时区偏移，单位小时
        TIME_VALUE_TYPE timeValueType = TIME_VALUE_TYPE.NONE;

        boolean autoTrack; // 内部变量
        boolean saveData = SAVE_DATA_TO_DATABASE;

        private DataDescription() {
        }

        void setNoCache() {
            this.saveData = false;
        }

        void setAutoTrackFlag() {
            this.autoTrack = true;
        }

        DataDescription(String eventType, JSONObject properties) {
            this.type = eventType;
            this.properties = properties;
        }

        void setTime(String timeString, double zoneOffset, TIME_VALUE_TYPE timeValueType) {
            this.timeString = timeString;
            this.zoneOffset = zoneOffset;
            this.timeValueType = timeValueType;
        }
    }

    private class EventDescription extends DataDescription {
        EventDescription(String eventName, JSONObject properties) {
            this.type = TDConstants.TYPE_TRACK;
            this.eventName = eventName;
            this.properties = properties;
        }
    }

    private void trackInternal(DataDescription data) {
        if (TextUtils.isEmpty(data.type)) {
            TDLog.d(TAG, "EventType could not be empty");
            return;
        }
        if(data.type.equals(TDConstants.TYPE_TRACK)) {
            if(PropertyUtils.isInvalidName(data.eventName)) {
                TDLog.w(TAG, "Event name[" + data.eventName + "] is invalid. Event name must be string that starts with English letter, " +
                            "and contains letter, number, and '_'. The max length of the event name is 50.");
                if (mConfig.shouldThrowException()) throw new TDDebugException("Invalid event name: " + data.eventName);
                return;
            }
        }

        if (!data.autoTrack && !PropertyUtils.checkProperty(data.properties)) {
            TDLog.w(TAG, "The data will not be tracked due to properties checking failure: " + data.properties.toString());
            if (mConfig.shouldThrowException()) throw new TDDebugException("Invalid properties. Please refer to SDK debug log for detail reasons.");
            return;
        }

        try {
            String timeString;
            double offset;
            if (data.timeValueType == TIME_VALUE_TYPE.NONE) {
                SimpleDateFormat sDateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
                TimeZone timeZone = mConfig.getDefaultTimeZone();
                sDateFormat.setTimeZone(timeZone);
                Date currentDate = new Date();
                offset = TDUtils.getTimezoneOffset(currentDate.getTime(), timeZone);
                timeString = sDateFormat.format(currentDate);
            } else {
                timeString = data.timeString;
                offset = data.zoneOffset;
            }

            final JSONObject dataObj = new JSONObject();

            dataObj.put(TDConstants.KEY_TYPE, data.type);
            dataObj.put(TDConstants.KEY_TIME, timeString);
            if(data.type.equals(TDConstants.TYPE_TRACK)) {
                dataObj.put(TDConstants.KEY_EVENT_NAME, data.eventName);
            }

            if (!TextUtils.isEmpty(getLoginId())) {
                dataObj.put(TDConstants.KEY_ACCOUNT_ID, getLoginId());
            }
            dataObj.put(TDConstants.KEY_DISTINCT_ID, getDistinctId());

            final JSONObject sendProps = new JSONObject();
            if (null != data.properties) {
                final Iterator<?> propIterator = data.properties.keys();
                while (propIterator.hasNext()) {
                    final String key = (String) propIterator.next();
                    if (!data.properties.isNull(key)) {
                        sendProps.put(key, data.properties.get(key));
                    }
                }
            }

            JSONObject finalProperties = new JSONObject();
            if(data.type.equals(TDConstants.TYPE_TRACK)) {
                JSONObject superProperties = getSuperProperties();
                TDUtils.mergeJSONObject(superProperties, finalProperties);

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

            if(data.type.equals(TDConstants.TYPE_TRACK)) {
                finalProperties.put(TDConstants.KEY_NETWORK_TYPE, mSystemInformation.getNetworkType());
                if (!TextUtils.isEmpty(mSystemInformation.getAppVersionName())) {
                    finalProperties.put(TDConstants.KEY_APP_VERSION, mSystemInformation.getAppVersionName());
                }

                if (data.timeValueType != TIME_VALUE_TYPE.TIME_ONLY) {
                    finalProperties.put(TDConstants.KEY_ZONE_OFFSET, offset);
                }

                final EventTimer eventTimer;
                synchronized (mTrackTimer) {
                    eventTimer = mTrackTimer.get(data.eventName);
                    mTrackTimer.remove(data.eventName);
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
            if (mConfig.isDebugOnly() || mConfig.isDebug()) {
                mMessages.postToDebug(dataObj, getToken());
            } else {
                if (data.saveData) {
                    mMessages.saveClickData(dataObj, getToken());
                } else {
                    mMessages.postClickData(dataObj, getToken());
                }
            }
        } catch (Exception e) {
            TDLog.w(TAG, "Exception occurred in track data: " + data.type + ": " + data.properties);
            e.printStackTrace();
            if (mConfig.shouldThrowException()) throw new TDDebugException(e);
        }
    }

    @Override
    public void user_add(String propertyName, Number propertyValue) {
        if (hasDisabled()) return;
        try {
            if (null == propertyValue) {
                TDLog.d(TAG, "user_add value must be Number");
                if (mConfig.shouldThrowException()) throw new TDDebugException("Invalid property values for user add.");
            } else {
                JSONObject properties = new JSONObject();
                properties.put(propertyName, propertyValue);
                user_add(properties);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (mConfig.shouldThrowException()) throw new TDDebugException(e);
        }
    }

    @Override
    public void user_add(JSONObject properties) {
        if (hasDisabled()) return;
        trackInternal(new DataDescription(TDConstants.TYPE_USER_ADD, properties));
    }

    @Override
    public void user_setOnce(JSONObject properties) {
        if (hasDisabled()) return;
        trackInternal(new DataDescription(TDConstants.TYPE_USER_SET_ONCE, properties));
    }

    @Override
    public void user_set(JSONObject properties) {
        if (hasDisabled()) return;
        trackInternal(new DataDescription(TDConstants.TYPE_USER_SET, properties));
    }

    @Override
    public void user_delete() {
        if (hasDisabled()) return;
        trackInternal(new DataDescription(TDConstants.TYPE_USER_DEL, null));
    }

    @Override
    public void user_unset(String... properties) {
        if (hasDisabled()) return;
        JSONObject props = new JSONObject();
        for (String s : properties) {
            try {
                props.put(s, 0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (props.length() > 0) {
            trackInternal(new DataDescription(TDConstants.TYPE_USER_UNSET, props));
        }
    }

    @Override
    public void identify(String identity) {
        if (hasDisabled()) return;
        if (TextUtils.isEmpty(identity)) {
            TDLog.w(TAG,"The identity cannot be empty.");
            if (mConfig.shouldThrowException()) throw new TDDebugException("distinct id cannot be empty");
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
                TDLog.d(TAG,"The account id cannot be empty.");
                if (mConfig.shouldThrowException()) throw new TDDebugException("account id cannot be empty");
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

    String getLoginId() {
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

    String getRandomID() {
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
        } else
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
                if (mConfig.shouldThrowException()) throw new TDDebugException("Set super properties failed. Please refer to the SDK debug log for details.");
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

    /**
     * 动态公共属性接口.
     */
    public interface DynamicSuperPropertiesTracker {
        /**
         * 获取动态公共属性
         * @return 动态公共属性
         */
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
                if (mConfig.shouldThrowException()) throw new TDDebugException("Invalid event name for time event");
                return;
            }

            synchronized (mTrackTimer) {
                mTrackTimer.put(eventName, new EventTimer(TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
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
                getToken().equals(annotation1.appId()))) {
            return true;
        }

        ThinkingDataIgnoreTrackAppViewScreen annotation2 = activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class);
        if (null != annotation2 && (TextUtils.isEmpty(annotation2.appId()) ||
                getToken().equals(annotation2.appId()))) {
            return true;
        }


        return false;
    }

    boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        if (eventType != null  && !mAutoTrackEventTypeList.contains(eventType)) {
            return true;
        }
        return false;
    }

    boolean isAutoTrackEnabled() {
        if (hasDisabled()) return false;
        return mAutoTrack;
    }

    /**
     * 自动采集事件类型
     */
    public enum AutoTrackEventType {
        /** APP 启动事件 ta_app_start */
        APP_START(TDConstants.APP_START_EVENT_NAME),
        /** APP 关闭事件 ta_app_end */
        APP_END(TDConstants.APP_END_EVENT_NAME),
        /** 控件点击事件 ta_app_click */
        APP_CLICK(TDConstants.APP_CLICK_EVENT_NAME),
        /** 页面浏览事件 ta_app_view */
        APP_VIEW_SCREEN(TDConstants.APP_VIEW_EVENT_NAME),
        /** APP 崩溃事件 ta_app_crash */
        APP_CRASH(TDConstants.APP_CRASH_EVENT_NAME),
        /** APP 安装事件 ta_app_install */
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

    /* package */ void trackViewScreenInternal(String url, JSONObject properties) {
        if (hasDisabled()) return;
        try {
            if ((!TextUtils.isEmpty(url) || properties != null)) {
                JSONObject trackProperties = new JSONObject();
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

                trackViewScreenInternal(screenUrl, properties);
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
            String title = TDUtils.getTitleFromFragment(fragment, getToken());

            Activity activity = fragment.getActivity();
            if (activity != null) {
                if (TextUtils.isEmpty(title)) {
                    title = TDUtils.getActivityTitle(activity);
                }
                screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName);
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

            String title = TDUtils.getTitleFromFragment(fragment, getToken());

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

            if (!TextUtils.isEmpty(title)) {
                properties.put(TDConstants.TITLE, title);
            }

            properties.put(TDConstants.SCREEN_NAME, screenName);
            autoTrack(TDConstants.APP_VIEW_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* package */ void appEnterBackground() {
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

    /* package */ void appBecomeActive() {
        TDQuitSafelyService.getInstance(mConfig.mContext).start();
        synchronized (mTrackTimer) {
            try {
                Iterator iterator = mTrackTimer.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
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
    public void trackAppInstall() {
        if (hasDisabled()) return;
        enableAutoTrack(new ArrayList<>(Collections.singletonList(AutoTrackEventType.APP_INSTALL)));
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
            TDQuitSafelyService quitSafelyService = TDQuitSafelyService.getInstance(mConfig.mContext);
            if (null != quitSafelyService) {
                quitSafelyService.initExceptionHandler();
            }
        }

        if (eventTypeList.contains(AutoTrackEventType.APP_END)) {
            timeEvent(TDConstants.APP_END_EVENT_NAME);
        }

        if (eventTypeList.contains(AutoTrackEventType.APP_INSTALL))  {
            synchronized (sInstanceMap) {
                if (sAppFirstInstallationMap.containsKey(mConfig.mContext) &&
                        sAppFirstInstallationMap.get(mConfig.mContext).contains(getToken())) {
                    track(TDConstants.APP_INSTALL_EVENT_NAME);
                    sAppFirstInstallationMap.get(mConfig.mContext).remove(getToken());
                }
            }
        }

        mAutoTrackEventTypeList.clear();
        mAutoTrackEventTypeList.addAll(eventTypeList);
    }

    @Override
    public void flush() {
        if (hasDisabled()) return;
        mMessages.flush(getToken());
    }

    /* package */ List<Class> getIgnoredViewTypeList() {
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

    /* package */ boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
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
                getToken().equals(annotation1.appId()))) {
            return true;
        }

        ThinkingDataIgnoreTrackAppClick annotation2 = activity.getAnnotation(ThinkingDataIgnoreTrackAppClick.class);
        return null != annotation2 && (TextUtils.isEmpty(annotation2.appId()) ||
                getToken().equals(annotation2.appId()));

    }

    /* package */ boolean isTrackFragmentAppViewScreenEnabled() {
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
            TDUtils.setTag(getToken(), view, R.id.thinking_analytics_tag_view_id, viewID);
        }
    }

    @Override
    public void setViewID(android.app.Dialog view, String viewID) {
        if (hasDisabled()) return;
        try {
            if (view != null && !TextUtils.isEmpty(viewID)) {
                if (view.getWindow() != null) {
                    TDUtils.setTag(getToken(), view.getWindow().getDecorView(), R.id.thinking_analytics_tag_view_id, viewID);
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
        TDUtils.setTag(getToken(), view, R.id.thinking_analytics_tag_view_properties, properties);
    }

    @Override
    public void ignoreView(View view) {
        if (hasDisabled()) return;
        if (view != null) {
            TDUtils.setTag(getToken(), view, R.id.thinking_analytics_tag_view_ignored, "1");
        }
    }

    @Override
    public void setJsBridge(WebView webView) {
        if (null == webView) {
            TDLog.d(TAG, "SetJsBridge failed due to parameter webView is null");
            if (mConfig.shouldThrowException()) throw new TDDebugException("webView cannot be null for setJsBridge");
            return;
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new TDWebAppInterface(this), "ThinkingData_APP_JS_Bridge");
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

            addJavascriptInterface.invoke(x5WebView, new TDWebAppInterface(this), "ThinkingData_APP_JS_Bridge");
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
        void process(ThinkingAnalyticsSDK instance);
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

    /* package */ boolean shouldTrackCrash() {
        if (hasDisabled()) return false;
        return mTrackCrash;
    }

    /**
     * 打开/关闭 实例功能. 当关闭 SDK 功能时，之前的缓存数据会保留，并继续上报; 但是不会追踪之后的数据和改动.
     * @param enabled true 打开上报; false 关闭上报
     */
    @Override
    public void enableTracking(boolean enabled) {
        TDLog.d(TAG, "enableTracking: " + enabled);
        if (isEnabled() && !enabled) flush();
        mEnableFlag.put(enabled);
    }

    /**
     * 停止上报此用户数据，并且发送 user_del (不会重试)
     */
    @Override
    public void optOutTrackingAndDeleteUser() {
        DataDescription userDel = new DataDescription(TDConstants.TYPE_USER_DEL, null);
        userDel.setNoCache();
        trackInternal(userDel);
        optOutTracking();
    }

    /**
     * 停止上报此用户的数据. 调用此接口之后，会删除本地缓存数据和之前设置; 后续的上报和设置都无效.
     */
    @Override
    public void optOutTracking() {
        TDLog.d(TAG, "optOutTracking..." );
        mOptOutFlag.put(true);
        mMessages.emptyMessageQueue(getToken());

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
    @Override
    public void optInTracking() {
        TDLog.d(TAG, "optInTracking..." );
        mOptOutFlag.put(false);
        mMessages.flush(getToken());
    }

    /**
     * 当前实例 Enable 状态. 通过 enableTracking 设置
     * @return true 已经恢复上报; false 已暂停上报.
     */
    public boolean isEnabled() {
        return mEnableFlag.get();
    }

    /**
     * 当前实例是否可以上报
     * @return true 已开启; false 停止
     */
    boolean hasDisabled() {
        return !isEnabled() || hasOptOut();
    }

    /**
     * 当前实例 OptOut 状态. 通过 optOutTracking(), optInTracking() 设置
     * @return true 已停止上报; false 未停止上报.
     */
    public boolean hasOptOut() {
        return mOptOutFlag.get();
    }

    /**
     * 创建轻量级的 SDK 实例. 轻量级的 SDK 实例不支持缓存本地账号ID，访客ID，公共属性等.
     * @return SDK 实例
     */
    @Override
    public ThinkingAnalyticsSDK createLightInstance() {
        return new LightThinkingAnalyticsSDK(mConfig);
    }

    /**
     * 获取当前实例的 APP ID
     * @return APP ID
     */
    public String getToken() {
        return mConfig.mToken;
    }

    // 本地缓存（SharePreference) 相关变量，所有实例共享
    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static Future<SharedPreferences> sStoredSharedPrefs;
    private static final String PREFERENCE_NAME = "com.thinkingdata.analyse";
    private static StorageLoginID sOldLoginId;
    private static final Object sOldLoginIdLock = new Object();
    private static StorageRandomID sRandomID;
    private static final Object sRandomIDLock = new Object();

    // 本地缓存（SharePreference 相关变量), 单个实例独有. 其文件名称为 PREFERENCE_NAME_{{mToken}}
    private final StorageLoginID mLoginId;
    private final StorageIdentifyId mIdentifyId;
    private final StorageEnableFlag mEnableFlag;
    private final StorageOptOutFlag mOptOutFlag;
    private final StorageSuperProperties mSuperProperties;


    // 动态公共属性接口
    private DynamicSuperPropertiesTracker mDynamicSuperPropertiesTracker;

    // 缓存 timeEvent 累计时间
    private final Map<String, EventTimer> mTrackTimer;

    // 自动采集相关变量
    private boolean mAutoTrack;
    private boolean mTrackCrash;
    private boolean mTrackFragmentAppViewScreen;
    private List<AutoTrackEventType> mAutoTrackEventTypeList;
    private List<Integer> mAutoTrackIgnoredActivities;
    private List<Class> mIgnoredViewTypeList = new ArrayList<>();
    private String mLastScreenUrl;

    // 保存已经初始化的所有实例对象
    private static final Map<Context, Map<String, ThinkingAnalyticsSDK>> sInstanceMap = new HashMap<>();

    // 用于采集 APP 安装事件的逻辑
    private static final Map<Context, List<String>> sAppFirstInstallationMap = new HashMap<>();

    // 是否同步老版本数据，v1.3.0+ 与之前版本兼容所做的内部使用变量
    private final boolean mEnableTrackOldData;

    // 是否开启本地缓存. 当设置为 false 时，所有数据将直接发送到接收端
    private final boolean SAVE_DATA_TO_DATABASE = true;

    private final DataHandle mMessages;
    private TDConfig mConfig;
    private SystemInformation mSystemInformation;

    private static final String TAG = "ThinkingAnalyticsSDK";

}

/**
 * 轻量级实例，不支持本地缓存，与主实例共享 APP ID.
 */
class LightThinkingAnalyticsSDK extends ThinkingAnalyticsSDK {
    private String mDistinctId;
    private String mAccountId;
    private final JSONObject mSuperProperties;
    private boolean mEnabled = true;

    LightThinkingAnalyticsSDK(TDConfig config) {
        super(config, true);
        mSuperProperties = new JSONObject();
    }

    @Override
    public void identify(String identity) {
        mDistinctId = identity;
    }

    @Override
    public void setSuperProperties(JSONObject superProperties) {
        if (hasDisabled()) return;
        try {
            if (superProperties == null || !PropertyUtils.checkProperty(superProperties)) {
                return;
            }

            synchronized (mSuperProperties) {
                TDUtils.mergeJSONObject(superProperties, mSuperProperties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unsetSuperProperty(String superPropertyName) {
        if (hasDisabled()) return;
        try {
            if (superPropertyName == null) {
                return;
            }
            synchronized (mSuperProperties) {
                mSuperProperties.remove(superPropertyName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void clearSuperProperties() {
        if (hasDisabled()) return;
        synchronized (mSuperProperties) {
            Iterator keys = mSuperProperties.keys();
            while(keys.hasNext()) {
                keys.next();
                keys.remove();
            }
        }
    }

    @Override
    public String getDistinctId() {
        if (null != mDistinctId) {
            return mDistinctId;
        } else {
            return getRandomID();
        }
    }

    @Override
    public JSONObject getSuperProperties() {
        return mSuperProperties;
    }

    @Override
    public void setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType type) {

    }

    @Override
    public void enableAutoTrack(List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList) {

    }

    @Override
    public void trackFragmentAppViewScreen() {

    }

    @Override
    public void trackViewScreen(Activity activity) {

    }

    @Override
    public void trackViewScreen(Fragment fragment) {

    }

    @Override
    public void trackViewScreen(Object fragment) {

    }

    @Override
    public void setViewID(View view, String viewID) {

    }

    @Override
    public void setViewID(Dialog view, String viewID) {

    }

    @Override
    public void setViewProperties(View view, JSONObject properties) {

    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {

    }

    @Override
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {

    }

    @Override
    public void ignoreViewType(Class viewType) {

    }

    @Override
    public void ignoreView(View view) {

    }

    @Override
    public void setJsBridge(WebView webView) {

    }

    @Override
    public void setJsBridgeForX5WebView(Object x5WebView) {

    }

    @Override
    public void login(String accountId) {
        if (hasDisabled()) return;
        mAccountId = accountId;
    }

    @Override
    public void logout() {
        if (hasDisabled()) return;
        mAccountId = null;
    }

    @Override
    String getLoginId() {
        return mAccountId;
    }

    @Override
    public void optOutTracking() {
    }

    @Override
    public void optInTracking() {
    }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public boolean hasOptOut() {
        return false;
    }

    @Override
    public void optOutTrackingAndDeleteUser() {

    }

    @Override
    public void enableTracking(boolean enabled) {
        mEnabled = enabled;
    }
}
