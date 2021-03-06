package cn.thinkingdata.android.utils.android;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cn.thinkingdata.android.R;
import cn.thinkingdata.android.utils.android.persistence.StorageEnableFlag;
import cn.thinkingdata.android.utils.android.persistence.StorageIdentifyId;
import cn.thinkingdata.android.utils.android.persistence.StorageLoginID;
import cn.thinkingdata.android.utils.android.persistence.StorageOptOutFlag;
import cn.thinkingdata.android.utils.android.persistence.StorageRandomID;
import cn.thinkingdata.android.utils.android.persistence.StorageSuperProperties;
import cn.thinkingdata.android.utils.android.utils.ICalibratedTime;
import cn.thinkingdata.android.utils.android.utils.ITime;
import cn.thinkingdata.android.utils.android.utils.PropertyUtils;
import cn.thinkingdata.android.utils.android.utils.TDCalibratedTime;
import cn.thinkingdata.android.utils.android.utils.TDCalibratedTimeWithNTP;
import cn.thinkingdata.android.utils.android.utils.TDConstants;
import cn.thinkingdata.android.utils.android.utils.TDLog;
import cn.thinkingdata.android.utils.android.utils.TDTime;
import cn.thinkingdata.android.utils.android.utils.TDTimeCalibrated;
import cn.thinkingdata.android.utils.android.utils.TDTimeConstant;
import cn.thinkingdata.android.utils.android.utils.TDUtils;

public class ThinkingAnalyticsSDK implements IThinkingAnalyticsAPI {

    /**
     * ??? SDK ???????????????????????????????????????????????????????????????
     * @param context app context
     * @return SDK ??????
     */
    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId) {
        return sharedInstance(context, appId, null, false);
    }

    /**
     * ????????? SDK. ???????????????????????????track ???????????????.
     * @param context app context
     * @param appId APP ID
     * @param url ???????????????
     * @return SDK ??????
     */
    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url) {
        return sharedInstance(context, appId, url, true);
    }

    /**
     *  ???????????????????????????????????????????????????????????????????????????????????????????????? SDK ???
     * @param trackOldData ?????????????????????(1.2.0 ?????????)?????????
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

        TDConfig config;
        try {
            config = TDConfig.getInstance(context, appId, url);
        } catch (IllegalArgumentException e) {
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

        if(!TDUtils.isMainProcess(config.mContext))
        {
            return new SubprocessThinkingAnalyticsSDK(config);
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
    public static Map<Context, Map<String, ThinkingAnalyticsSDK>> instances()
    {
        return sInstanceMap;
    }

    /**
     * SDK ??????????????????????????? TDConfig ??????. ?????????????????? TDConfig ????????? ?????????????????????????????? SDK.
     * @param config TDConfig ??????
     * @param light ?????????????????????????????????)
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

        // ??????????????????????????????ID???????????????
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


        mLifecycleCallbacks = new ThinkingDataActivityLifecycleCallbacks(this, mConfig.getMainProcessName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = (Application) config.mContext.getApplicationContext();
            app.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
        }

        if (!config.isNormal()) {
            enableTrackLog(true);
        }

        if(config.isEnableMutiprocess()&& TDUtils.isMainProcess(config.mContext))
        {
            TDReceiver.registerReceiver(config.mContext);
        }

        TDLog.i(TAG, String.format("Thinking Analytics SDK %s instance initialized successfully with mode: %s, APP ID ends with: %s, server url: %s, device ID: %s", TDConfig.VERSION,
                config.getMode().name(), TDUtils.getSuffix(config.mToken, 4), config.getServerUrl(), getDeviceId()));
    }
    /**
     * ??????/?????? ????????????
     * @param enableLog true ????????????; false ????????????
     */
    public static void enableTrackLog(boolean enableLog) {
        TDLog.setEnableLog(enableLog);
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @param libName ?????????????????? #lib ????????????
     * @param libVersion ?????????????????? #lib_version ????????????
     */
    public static void setCustomerLibInfo(String libName, String libVersion) {
        SystemInformation.setLibraryInfo(libName, libVersion);
    }

    // H5 ????????? SDK ????????????????????? SDK ????????????
    void trackFromH5(String event) {
        if (hasDisabled()) return;
        if (TextUtils.isEmpty(event)) return;
        try {
            JSONArray data = new JSONObject(event).getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject eventObject = data.getJSONObject(i);

                String timeString = eventObject.getString(TDConstants.KEY_TIME);

                Double zoneOffset = null;
                if (eventObject.has(TDConstants.KEY_ZONE_OFFSET)) {
                    zoneOffset = eventObject.getDouble(TDConstants.KEY_ZONE_OFFSET);
                }

                ITime time = getTime(timeString, zoneOffset);

                String eventType = eventObject.getString(TDConstants.KEY_TYPE);

                TDConstants.DataType type = TDConstants.DataType.get(eventType);
                if (null == type) {
                    TDLog.w(TAG, "Unknown data type from H5. ignoring...");
                    return;
                }

                JSONObject properties = eventObject.getJSONObject(TDConstants.KEY_PROPERTIES);
                for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
                    String key = (String) iterator.next();
                    if (key.equals(TDConstants.KEY_ACCOUNT_ID) || key.equals(TDConstants.KEY_DISTINCT_ID) || mSystemInformation.getDeviceInfo().containsKey(key)) {
                        iterator.remove();
                    }
                }

                DataDescription dataDescription;
                if (type.isTrack()) {
                    String eventName = eventObject.getString(TDConstants.KEY_EVENT_NAME);

                    Map<String, String> extraFields = new HashMap<>();
                    if (eventObject.has(TDConstants.KEY_FIRST_CHECK_ID)) {
                        extraFields.put(TDConstants.KEY_FIRST_CHECK_ID, eventObject.getString(TDConstants.KEY_FIRST_CHECK_ID));
                    }
                    if (eventObject.has(TDConstants.KEY_EVENT_ID)) {
                        extraFields.put(TDConstants.KEY_EVENT_ID, eventObject.getString(TDConstants.KEY_EVENT_ID));
                    }

                    track(eventName, properties, time, false, extraFields, type);
                } else {
                    dataDescription = new DataDescription(this, type, properties, time);
                    trackInternal(dataDescription);
                }
            }
        } catch (Exception e) {
            TDLog.w(TAG, "Exception occurred when track data from H5.");
            e.printStackTrace();
        }
    }

    /**
     * ???????????????????????????
     */
    public enum ThinkingdataNetworkType {
        /** ??????????????????3G???4G???5G???WiFi ????????????????????? */
        NETWORKTYPE_DEFAULT,
        /** ?????? WiFi ?????????????????? */
        NETWORKTYPE_WIFI,
        /** ?????????????????????????????? */
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
        //autoTrack(eventName, properties, null);
        if (hasDisabled()) return;
        track(eventName, properties, getTime(), false);
    }

    @Override
    public void track(String eventName, JSONObject properties) {
        if (hasDisabled()) return;
        track(eventName, properties, getTime());
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time) {
        if (hasDisabled()) return;
        track(eventName, properties, getTime(time, null));
    }

    @Override
    public void  track(String eventName, JSONObject properties, Date time, TimeZone timeZone) {
        if (hasDisabled()) return;
        track(eventName, properties, getTime(time, timeZone));
    }

    private void track(String eventName, JSONObject properties, ITime time) {
        track(eventName, properties, time, true);
    }

    private void track(String eventName, JSONObject properties, ITime time, boolean doFormatChecking) {
        track(eventName, properties, time, doFormatChecking, null, null);
    }

    private void track(String eventName, JSONObject properties, ITime time, boolean doFormatChecking, Map<String, String> extraFields, TDConstants.DataType type) {
        if (mConfig.isDisabledEvent(eventName)) {
            TDLog.d(TAG, "Ignoring disabled event [" + eventName +"]");
            return;
        }

        try {
            if(doFormatChecking && PropertyUtils.isInvalidName(eventName)) {
                TDLog.w(TAG, "Event name[" + eventName + "] is invalid. Event name must be string that starts with English letter, " +
                        "and contains letter, number, and '_'. The max length of the event name is 50.");
                if (mConfig.shouldThrowException()) throw new TDDebugException("Invalid event name: " + eventName);
            }

            if (doFormatChecking && !PropertyUtils.checkProperty(properties)) {
                TDLog.w(TAG, "The data contains invalid key or value: " + properties.toString());
                if (mConfig.shouldThrowException()) throw new TDDebugException("Invalid properties. Please refer to SDK debug log for detail reasons.");
            }

            JSONObject finalProperties = obtainDefaultEventProperties(eventName);

            if (null != properties) {
                TDUtils.mergeJSONObject(properties, finalProperties, mConfig.getDefaultTimeZone());
            }

            TDConstants.DataType dataType = type == null ? TDConstants.DataType.TRACK : type;

            DataDescription dataDescription = new DataDescription(this, dataType, finalProperties, time);
            dataDescription.eventName = eventName;
            if (null != extraFields) {
                dataDescription.setExtraFields(extraFields);
            }

            trackInternal(dataDescription);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void track(String eventName) {
        if (hasDisabled()) return;
        track(eventName, null, getTime());
    }

    @Override
    public void track(ThinkingAnalyticsEvent event) {
        if (hasDisabled()) return;
        if (null == event) {
            TDLog.w(TAG, "Ignoring empty event...");
            return;
        }
        ITime time;
        if (event.getEventTime() != null) {
            time = getTime(event.getEventTime(), event.getTimeZone());
        } else {
            time = getTime();
        }

        Map<String, String> extraFields = new HashMap<>();
        if (TextUtils.isEmpty(event.getExtraField())) {
            TDLog.w(TAG, "Invalid ExtraFields. Ignoring...");
        } else {
            String extraValue;
            if (event instanceof TDFirstEvent && event.getExtraValue() == null) {
                extraValue = getDeviceId();
            } else {
                extraValue = event.getExtraValue();
            }

            extraFields.put(event.getExtraField(), extraValue);
        }

        track(event.getEventName(), event.getProperties(), time, true, extraFields, event.getDataType());
    }


    void trackInternal(DataDescription dataDescription) {
        if (mConfig.isDebugOnly() || mConfig.isDebug()) {
            mMessages.postToDebug(dataDescription);
        } else if (dataDescription.saveData) {
            mMessages.saveClickData(dataDescription);
        } else {
            mMessages.postClickData(dataDescription);
        }
    }

    private JSONObject obtainDefaultEventProperties(String eventName) {

        JSONObject finalProperties = new JSONObject();
        try {
            TDUtils.mergeJSONObject(getSuperProperties(), finalProperties, mConfig.getDefaultTimeZone());
            try {
                if (mDynamicSuperPropertiesTracker != null) {
                    JSONObject dynamicSuperProperties = mDynamicSuperPropertiesTracker.getDynamicSuperProperties();
                    if (dynamicSuperProperties != null && PropertyUtils.checkProperty(dynamicSuperProperties)) {
                        TDUtils.mergeJSONObject(dynamicSuperProperties, finalProperties, mConfig.getDefaultTimeZone());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            TDUtils.mergeJSONObject(mMessages.deviceInfo(),finalProperties,mConfig.getDefaultTimeZone());
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
            finalProperties.put(TDConstants.KEY_NETWORK_TYPE, mSystemInformation.getNetworkType());
        } catch (Exception e) {

        }

        return finalProperties;
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
    public void user_append(JSONObject properties) {
        user_append(properties, null);
    }

    public void user_append(JSONObject properties, Date date) {
        if (hasDisabled()) return;
        user_operations(TDConstants.DataType.USER_APPEND, properties, date);
    }

    @Override
    public void user_add(JSONObject properties) {
        user_add(properties, null);
    }

    public void user_add(JSONObject properties, Date date) {
        if (hasDisabled()) return;
        user_operations(TDConstants.DataType.USER_ADD, properties, date);
    }

    @Override
    public void user_setOnce(JSONObject properties) {
       user_setOnce(properties, null);
    }

    public void user_setOnce(JSONObject properties, Date date) {
        if (hasDisabled()) return;
        user_operations(TDConstants.DataType.USER_SET_ONCE, properties, date);
    }

    @Override
    public void user_set(JSONObject properties) {
        user_set(properties, null);
    }

    public void user_set(JSONObject properties, Date date) {
        user_operations(TDConstants.DataType.USER_SET, properties, date);
    }

    void user_operations(TDConstants.DataType type, JSONObject properties, Date date) {
        if (hasDisabled()) return;
        if (!PropertyUtils.checkProperty(properties)) {
            TDLog.w(TAG, "The data contains invalid key or value: " + properties.toString());
            if (mConfig.shouldThrowException()) throw new TDDebugException("Invalid properties. Please refer to SDK debug log for detail reasons.");
        }
        try {
            ITime time = date == null ? getTime() : getTime(date, null);
            JSONObject finalProperties = new JSONObject();
            if (properties != null) {
                TDUtils.mergeJSONObject(properties, finalProperties, mConfig.getDefaultTimeZone());
            }
            trackInternal(new DataDescription(this, type, finalProperties, time));
        } catch (Exception e) {
            TDLog.w(TAG, e.getMessage());
        }
    }

    @Override
    public void user_delete() {
        user_delete(null);
    }

    public void user_delete(Date date) {
        if (hasDisabled()) return;
        user_operations(TDConstants.DataType.USER_DEL, null, date);
    }

    @Override
    public void user_unset(String... properties) {
        if (hasDisabled()) return;
        if (properties == null) return;
        JSONObject props = new JSONObject();
        for (String s : properties) {
            try {
                props.put(s, 0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (props.length() > 0) {
            user_unset(props, null);
        }
    }

    public void user_unset(JSONObject properties, Date date) {
        if (hasDisabled()) return;
        user_operations(TDConstants.DataType.USER_UNSET, properties, date);

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
                TDUtils.mergeJSONObject(superProperties, properties, mConfig.getDefaultTimeZone());
                mSuperProperties.put(properties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????????????????.
     */
    public interface DynamicSuperPropertiesTracker {
        /**
         * ????????????????????????
         * @return ??????????????????
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
                TDLog.w(TAG, "timeEvent event name[" + eventName + "] is not valid");
                //if (mConfig.shouldThrowException()) throw new TDDebugException("Invalid event name for time event");
                //return;
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
     * ????????????????????????
     */
    public enum AutoTrackEventType {
        /** APP ???????????? ta_app_start */
        APP_START(TDConstants.APP_START_EVENT_NAME),
        /** APP ???????????? ta_app_end */
        APP_END(TDConstants.APP_END_EVENT_NAME),
        /** ?????????????????? ta_app_click */
        APP_CLICK(TDConstants.APP_CLICK_EVENT_NAME),
        /** ?????????????????? ta_app_view */
        APP_VIEW_SCREEN(TDConstants.APP_VIEW_EVENT_NAME),
        /** APP ???????????? ta_app_crash */
        APP_CRASH(TDConstants.APP_CRASH_EVENT_NAME),
        /** APP ???????????? ta_app_install */
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
                    TDUtils.mergeJSONObject(properties, trackProperties, mConfig.getDefaultTimeZone());
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
                    TDUtils.mergeJSONObject(otherProperties, properties, mConfig.getDefaultTimeZone());
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
    public void trackViewScreen(Fragment fragment) {
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

    // used by unity SDK. Unity 2019.2.1f1 doesn't support enum type.
    private static final int APP_START = 1;
    private static final int APP_END = 1 << 1;
    private static final int APP_CRASH = 1 << 4;
    private static final int APP_INSTALL = 1 << 5;
    public void enableAutoTrack(int types) {
        List<AutoTrackEventType>eventTypeList = new ArrayList<>();
        if ((types & APP_START) > 0) {
            eventTypeList.add(AutoTrackEventType.APP_START);
        }

        if ((types & APP_END) > 0) {
            eventTypeList.add(AutoTrackEventType.APP_END);
        }

        if ((types & APP_INSTALL) > 0) {
            eventTypeList.add(AutoTrackEventType.APP_INSTALL);
        }

        if ((types & APP_CRASH) > 0) {
            eventTypeList.add(AutoTrackEventType.APP_CRASH);
        }


        if (eventTypeList.size() > 0) {
            enableAutoTrack(eventTypeList);
        }
    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        if (hasDisabled()) return;

        mAutoTrack = true;
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
        }

        if (eventTypeList.contains(AutoTrackEventType.APP_INSTALL))  {
            synchronized (sInstanceMap) {
                if (sAppFirstInstallationMap.containsKey(mConfig.mContext) &&
                        sAppFirstInstallationMap.get(mConfig.mContext).contains(getToken())) {
                    track(TDConstants.APP_INSTALL_EVENT_NAME);
                    flush();
                    sAppFirstInstallationMap.get(mConfig.mContext).remove(getToken());
                }
            }
        }

        if (eventTypeList.contains(AutoTrackEventType.APP_CRASH)) {
            mTrackCrash = true;
            TDQuitSafelyService quitSafelyService = TDQuitSafelyService.getInstance(mConfig.mContext);
            if (null != quitSafelyService) {
                quitSafelyService.initExceptionHandler();
            }
        }

        // ????????????????????????timeEvent?????????????????????????????????????????????
        if (!mAutoTrackEventTypeList.contains(AutoTrackEventType.APP_END)
                && eventTypeList.contains(AutoTrackEventType.APP_END)) {
            timeEvent(TDConstants.APP_END_EVENT_NAME);
        }



        synchronized (this) {
            mAutoTrackStartTime = getTime();
            mAutoTrackStartProperties = obtainDefaultEventProperties(TDConstants.APP_START_EVENT_NAME);
        }

        mAutoTrackEventTypeList.clear();
        mAutoTrackEventTypeList.addAll(eventTypeList);
        if (mAutoTrackEventTypeList.contains(AutoTrackEventType.APP_START)) {
            mLifecycleCallbacks.onAppStartEventEnabled();
        }
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
    public void setViewID(Dialog view, String viewID) {
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
     * ??????/?????? ????????????. ????????? SDK ????????????????????????????????????????????????????????????; ??????????????????????????????????????????.
     * @param enabled true ????????????; false ????????????
     */
    @Override
    public void enableTracking(boolean enabled) {
        TDLog.d(TAG, "enableTracking: " + enabled);
        if (isEnabled() && !enabled) flush();
        mEnableFlag.put(enabled);
    }

    /**
     * ?????????????????????????????????????????? user_del (????????????)
     */
    @Override
    public void optOutTrackingAndDeleteUser() {
        DataDescription userDel = new DataDescription(this, TDConstants.DataType.USER_DEL, null, getTime());
        userDel.setNoCache();
        trackInternal(userDel);
        optOutTracking();
    }

    /**
     * ??????????????????????????????. ??????????????????????????????????????????????????????????????????; ?????????????????????????????????.
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
     * ????????????????????????.
     */
    @Override
    public void optInTracking() {
        TDLog.d(TAG, "optInTracking..." );
        mOptOutFlag.put(false);
        mMessages.flush(getToken());
    }

    /**
     * ???????????? Enable ??????. ?????? enableTracking ??????
     * @return true ??????????????????; false ???????????????.
     */
    public boolean isEnabled() {
        return mEnableFlag.get();
    }

    /**
     * ??????????????????????????????
     * @return true ?????????; false ??????
     */
    boolean hasDisabled() {
        return !isEnabled() || hasOptOut();
    }

    /**
     * ???????????? OptOut ??????. ?????? optOutTracking(), optInTracking() ??????
     * @return true ???????????????; false ???????????????.
     */
    public boolean hasOptOut() {
        return mOptOutFlag.get();
    }

    /**
     * ?????????????????? SDK ??????. ???????????? SDK ?????????????????????????????????ID?????????ID??????????????????.
     * @return SDK ??????
     */
    @Override
    public ThinkingAnalyticsSDK createLightInstance() {
        return new LightThinkingAnalyticsSDK(mConfig);
    }

    @Override
    public TDPresetProperties getPresetProperties() {
        JSONObject presetProperties = SystemInformation.getInstance(mConfig.mContext).currentPresetProperties();
        String networkType = SystemInformation.getInstance(mConfig.mContext).getNetworkType();
        double zoneOffset = getTime().getZoneOffset();
        try {
            presetProperties.put(TDConstants.KEY_NETWORK_TYPE,networkType);
            presetProperties.put(TDConstants.KEY_ZONE_OFFSET,zoneOffset);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        TDPresetProperties presetPropertiesModel = new TDPresetProperties(presetProperties);
        return presetPropertiesModel;
    }



    /**
     * ????????????????????? APP ID
     * @return APP ID
     */
    public String getToken() {
        return mConfig.mToken;
    }

    public String getTimeString(Date date) {
       return getTime(date, mConfig.getDefaultTimeZone()).getTime();
    }

    // ???????????????SharePreference) ?????????????????????????????????
    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static Future<SharedPreferences> sStoredSharedPrefs;
    private static final String PREFERENCE_NAME = "com.thinkingdata.analyse";
    private static StorageLoginID sOldLoginId;
    private static final Object sOldLoginIdLock = new Object();
    private static StorageRandomID sRandomID;
    private static final Object sRandomIDLock = new Object();

    // ???????????????SharePreference ????????????), ??????????????????. ?????????????????? PREFERENCE_NAME_{{mToken}}
    private final StorageLoginID mLoginId;
    private final StorageIdentifyId mIdentifyId;
    private final StorageEnableFlag mEnableFlag;
    private final StorageOptOutFlag mOptOutFlag;
    private final StorageSuperProperties mSuperProperties;


    // ????????????????????????
    private DynamicSuperPropertiesTracker mDynamicSuperPropertiesTracker;

    // ?????? timeEvent ????????????
    final Map<String, EventTimer> mTrackTimer;

    // ????????????????????????
    private boolean mAutoTrack;
    private boolean mTrackCrash;
    private boolean mTrackFragmentAppViewScreen;
    private List<AutoTrackEventType> mAutoTrackEventTypeList;
    private List<Integer> mAutoTrackIgnoredActivities;
    private List<Class> mIgnoredViewTypeList = new ArrayList<>();
    private String mLastScreenUrl;
    private ThinkingDataActivityLifecycleCallbacks mLifecycleCallbacks;

    // ??????????????????????????????????????????
    private static final Map<Context, Map<String, ThinkingAnalyticsSDK>> sInstanceMap = new HashMap<>();

    // ???????????? APP ?????????????????????
    private static final Map<Context, List<String>> sAppFirstInstallationMap = new HashMap<>();

    // ??????????????????????????????v1.3.0+ ????????????????????????????????????????????????
    private final boolean mEnableTrackOldData;

    private final DataHandle mMessages;
    TDConfig mConfig;
    private SystemInformation mSystemInformation;

    static final String TAG = "ThinkingAnalyticsSDK";

    // ??????????????????????????????????????????????????????????????????
    private ITime mAutoTrackStartTime;
    synchronized ITime getAutoTrackStartTime() {
        return mAutoTrackStartTime;
    }
    private JSONObject mAutoTrackStartProperties;
    synchronized JSONObject getAutoTrackStartProperties() {
        return mAutoTrackStartProperties == null ? new JSONObject() : mAutoTrackStartProperties;
    }

    private static ICalibratedTime sCalibratedTime;
    private final static ReentrantReadWriteLock sCalibratedTimeLock = new ReentrantReadWriteLock();

    // ????????????????????? ITime ??????
    private ITime getTime() {
        ITime result;
        sCalibratedTimeLock.readLock().lock();
        if (null != sCalibratedTime) {
            result = new TDTimeCalibrated(sCalibratedTime, mConfig.getDefaultTimeZone());
        } else {
            result = new TDTime(new Date(), mConfig.getDefaultTimeZone());
        }
        sCalibratedTimeLock.readLock().unlock();
        return result;
    }

    // ??????????????? date ??? timeZone ????????? ITime ??????
    // ?????? timeZone ??? null, ??????????????????????????? #zone_offset ??????.
    private ITime getTime(Date date, TimeZone timeZone) {
        if (null == timeZone) {
            TDTime time = new TDTime(date, mConfig.getDefaultTimeZone());
            time.disableZoneOffset();
            return time;
        }
        return new TDTime(date, timeZone);
    }

    // ????????????????????? ITime ??????
    private ITime getTime(String timeString, Double zoneOffset) {
        return new TDTimeConstant(timeString, zoneOffset);
    }

    DynamicSuperPropertiesTracker getDynamicSuperPropertiesTracker()
    {
        return  mDynamicSuperPropertiesTracker;
    }

    /**
     * ????????????
     * @param timestamp ???????????????
     */
    public static void calibrateTime(long timestamp) {
        setCalibratedTime(new TDCalibratedTime(timestamp));
    }

    /**
     * ??????????????? NTP Server ????????????
     * @param ntpServer NTP Server ??????
     */
    public static void calibrateTimeWithNtp(String... ntpServer) {
        if (null == ntpServer) return;
        setCalibratedTime(new TDCalibratedTimeWithNTP(ntpServer));
    }

    // For Unity 2018.04 version
    public static void calibrateTimeWithNtpForUnity(String ntpServer) {
        calibrateTimeWithNtp(ntpServer);
    }

    /**
     * ?????????????????? ICalibratedTime ????????????
     * @param calibratedTime ICalibratedTime ??????
     */
    private static void setCalibratedTime(ICalibratedTime calibratedTime) {
        sCalibratedTimeLock.writeLock().lock();
        sCalibratedTime = calibratedTime;
        sCalibratedTimeLock.writeLock().unlock();
    }
}

/**
 * ???????????????????????????????????????????????????????????? APP ID.
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
                TDUtils.mergeJSONObject(superProperties, mSuperProperties, mConfig.getDefaultTimeZone());
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
    public void setNetworkType(ThinkingdataNetworkType type) {

    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {

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

/**
 * ???????????????
 */
class  SubprocessThinkingAnalyticsSDK extends ThinkingAnalyticsSDK
{
    Context mContext;
    private String mDistinctId;
    private String mAccountId;
    private boolean mEnabled = true;
    public SubprocessThinkingAnalyticsSDK(TDConfig config) {
        super(config);
        this.mContext = config.mContext;
    }
    @Override
    public void identify(String distinctId) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_IDENTIFY);
        if(distinctId != null && distinctId.length() > 0)
        {
            intent.putExtra(TDConstants.KEY_DISTINCT_ID,distinctId);
        }else
        {
            intent.putExtra(TDConstants.KEY_DISTINCT_ID,"");
        }
        mContext.sendBroadcast(intent);
    }

    @Override
    public void login(String accountId)
    {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_LOGIN);
        if(accountId != null && accountId.length() > 0)
        {
            intent.putExtra(TDConstants.KEY_ACCOUNT_ID,accountId);
        }else
        {
            intent.putExtra(TDConstants.KEY_ACCOUNT_ID,"");
        }
        mContext.sendBroadcast(intent);
    }
    @Override
    public void flush()
    {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_FLUSH);
        mContext.sendBroadcast(intent);
    }
    @Override
    public void logout()
    {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_LOGOUT);
        mContext.sendBroadcast(intent);
    }


    @Override
    void user_operations(TDConstants.DataType type, JSONObject properties, Date date)
    {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_USER_PROPERTY_SET);
        intent.putExtra(TDConstants.TD_KEY_USER_PROPERTY_SET_TYPE,type.getType());
        if(properties != null)
        {
            JSONObject realProperties = new JSONObject();
            try {
                TDUtils.mergeJSONObject(properties,realProperties,mConfig.getDefaultTimeZone());
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
            intent.putExtra(TDConstants.KEY_PROPERTIES,realProperties.toString());
        }
        if(date != null)
        {
            intent.putExtra(TDConstants.TD_KEY_DATE,date.getTime());
        }
        mContext.sendBroadcast(intent);
    }

    @Override
    void autoTrack(String eventName, JSONObject properties) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.KEY_EVENT_NAME,eventName);
        properties = properties == null ? new JSONObject() : properties;
        JSONObject realProperties = obtainProperties(eventName,properties);
        intent.putExtra(TDConstants.KEY_PROPERTIES,realProperties.toString());
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK_AUTO_EVENT);
        mContext.sendBroadcast(intent);
    }
    public  JSONObject obtainProperties(String eventName,JSONObject properties)
    {
        JSONObject realProperties = new JSONObject();
        try{
            realProperties.put(TDConstants.TD_KEY_BUNDLE_ID, TDUtils.getCurrentProcessName(mContext));
            double duration = getEventDuration(eventName);
            if(duration > 0)
            {
                realProperties.put(TDConstants.KEY_DURATION,duration);
            }
        }catch (JSONException exception)
        {
        }
        if(getDynamicSuperPropertiesTracker() != null)
        {
            JSONObject dynamicProperties = getDynamicSuperPropertiesTracker().getDynamicSuperProperties();
            if(dynamicProperties != null)
            {
                try {
                    TDUtils.mergeJSONObject(dynamicProperties,realProperties,mConfig.getDefaultTimeZone());
                } catch (JSONException exception) {
                    exception.printStackTrace();
                }
            }
        }
        try {
            TDUtils.mergeJSONObject(properties,realProperties,mConfig.getDefaultTimeZone());
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return  realProperties;
    }
    @Override
    public void track(ThinkingAnalyticsEvent event) {
        Intent intent = getIntent();
        JSONObject properties = null;
        switch (event.getDataType())
        {
            case TRACK_OVERWRITE:
            {
                intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK_OVERWRITE_EVENT);
            }
            break;
            case TRACK_UPDATE:
            {
                intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK_UPDATABLE_EVENT);
            }
            break;
            case TRACK:
            {
                intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK_FIRST_EVENT);
            }
            break;
        }
        intent.putExtra(TDConstants.KEY_EVENT_NAME,event.getEventName());
        properties = event.getProperties() == null ? new JSONObject() : event.getProperties();
        JSONObject realProperties = obtainProperties(event.getEventName(),properties);
        intent.putExtra(TDConstants.KEY_PROPERTIES,realProperties.toString());
        if(event.getEventTime() != null)
        {
            intent.putExtra(TDConstants.TD_KEY_DATE,event.getEventTime().getTime());
        }
        if(event.getTimeZone() != null)
        {
            intent.putExtra(TDConstants.TD_KEY_TIMEZONE,event.getTimeZone().getID());
        }
        intent.putExtra(TDConstants.TD_KEY_EXTRA_FIELD,event.getExtraValue());
        mContext.sendBroadcast(intent);

    }
    @Override
    public void track(String eventName)
    {
        track(eventName,null,null,null);
    }
    @Override
    public void track(String eventName,JSONObject properties)
    {
        track(eventName,properties,null,null);
    }
    @Override
    public void track(String eventName,JSONObject properties,Date time)
    {
        track(eventName,properties,time,null);
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time, TimeZone timeZone) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK);
        intent.putExtra(TDConstants.KEY_EVENT_NAME,eventName);
        if(properties == null)
        {
            properties = new JSONObject();
        }
        JSONObject realProperties = obtainProperties(eventName,properties);
        intent.putExtra(TDConstants.KEY_PROPERTIES,realProperties.toString());
        if(time != null)
        {
            intent.putExtra(TDConstants.TD_KEY_DATE,time.getTime());
        }
        if(timeZone != null)
        {
            intent.putExtra(TDConstants.TD_KEY_TIMEZONE,timeZone.getID());
        }
        mContext.sendBroadcast(intent);
    }

    public Intent getIntent()
    {
        Intent intent = new Intent();
        String mainProcessName = TDUtils.getMainProcessName(mContext);
        if(mainProcessName.length() == 0)
        {
            mainProcessName = TDConstants.TD_RECEIVER_FILTER;
        }else
        {
            mainProcessName = mainProcessName+"." + TDConstants.TD_RECEIVER_FILTER;
        }
        intent.setAction(mainProcessName);
        intent.putExtra(TDConstants.KEY_APP_ID,mConfig.mToken);
        return  intent;
    }

    @Override
    public void setSuperProperties(JSONObject superProperties) {
        JSONObject properties = new JSONObject();
        try {
            TDUtils.mergeJSONObject(superProperties,properties,mConfig.getDefaultTimeZone());
            Intent intent = getIntent();
            intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_SET_SUPER_PROPERTIES);
            if(superProperties != null)
            {
                intent.putExtra(TDConstants.KEY_PROPERTIES,properties.toString());
            }
            mContext.sendBroadcast(intent);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void unsetSuperProperty(String superPropertyName) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_UNSET_SUPER_PROPERTIES);
        if(superPropertyName != null)
        {
            intent.putExtra(TDConstants.KEY_PROPERTIES,superPropertyName);
        }
        mContext.sendBroadcast(intent);
    }

    @Override
    public void clearSuperProperties() {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_CLEAR_SUPER_PROPERTIES);
        mContext.sendBroadcast(intent);
    }

    double getEventDuration(String eventName)
    {
        final EventTimer eventTimer;
        Double duration = 0d;
        synchronized (mTrackTimer) {
            eventTimer = mTrackTimer.get(eventName);
            mTrackTimer.remove(eventName);
        }
        if (null != eventTimer) {
           duration = Double.valueOf(eventTimer.duration());
        }
        return duration;
    }

    @Override
    public void setNetworkType(ThinkingdataNetworkType type) {

    }


    @Override
    public void optOutTracking() {
    }

    @Override
    public void optInTracking() {
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

    }
}
