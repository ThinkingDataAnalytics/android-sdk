/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import static cn.thinkingdata.analytics.utils.TDConstants.*;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.webkit.WebView;

import cn.thinkingdata.analytics.autotrack.TAExceptionHandler;
import cn.thinkingdata.analytics.autotrack.ThinkingDataActivityLifecycleCallbacks;
import cn.thinkingdata.analytics.data.DataDescription;
import cn.thinkingdata.analytics.data.DataHandle;
import cn.thinkingdata.analytics.data.EventTimer;
import cn.thinkingdata.analytics.data.SystemInformation;
import cn.thinkingdata.analytics.data.UserOperationHandler;
import cn.thinkingdata.analytics.persistence.CommonStorageManager;
import cn.thinkingdata.analytics.persistence.GlobalStorageManager;
import cn.thinkingdata.analytics.tasks.TrackTaskManager;
import cn.thinkingdata.analytics.utils.ICalibratedTime;
import cn.thinkingdata.analytics.aop.push.TAPushUtils;
import cn.thinkingdata.analytics.encrypt.ThinkingDataEncrypt;
import cn.thinkingdata.analytics.utils.TDDebugException;
import cn.thinkingdata.analytics.utils.broadcast.TDReceiver;
import cn.thinkingdata.analytics.utils.plugin.TDPluginUtils;
import cn.thinkingdata.core.preset.TDPresetUtils;
import cn.thinkingdata.core.receiver.TDAnalyticsObservable;
import cn.thinkingdata.core.router.TRouter;
import cn.thinkingdata.analytics.session.SessionManager;
import cn.thinkingdata.analytics.utils.CalibratedTimeManager;
import cn.thinkingdata.analytics.utils.ITime;
import cn.thinkingdata.analytics.utils.PropertyUtils;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.core.router.TRouterMap;
import cn.thinkingdata.core.router.provider.ISensitiveProvider;
import cn.thinkingdata.core.router.provider.callback.ISensitivePropertiesCallBack;
import cn.thinkingdata.core.utils.TDLog;
import cn.thinkingdata.analytics.utils.TDUtils;

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
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * SDK Instance Class.
 */
public class ThinkingAnalyticsSDK implements IThinkingAnalyticsAPI {

    /**
     * After the SDK initialization is complete, the saved singleton can be obtained through this interface.
     *
     * @param context app context
     * @param appId   app id
     * @return SDK instance
     */
    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId) {
        return sharedInstance(context, appId, null, false);
    }

    /**
     * Initialize the SDK. The track function is not available until this interface is invoked.
     *
     * @param context app context
     * @param appId   APP ID
     * @param url     server url
     * @return SDK instance
     */
    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url) {
        return sharedInstance(context, appId, url, true);
    }

    /**
     * Use this interface sparingly. In most cases, the old version data will be bound to the first instantiated SDK by default.
     *
     * @param context      context
     * @param appId        app id
     * @param url          server url
     * @param trackOldData Whether to bind data from older versions (1.2.0 and earlier)
     * @return ThinkingAnalyticsSDK
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

    /**
     * @param config TDConfig
     * @return {@link ThinkingAnalyticsSDK}
     */
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
                Pair<Long, Boolean> installInfo = TDUtils.getInstallInfo(config.mContext);
                long installTime = installInfo.first;
                long lastInstallTime = GlobalStorageManager.getInstance(config.mContext).getLastInstallTime();
                boolean installTimeMatched;
                if (lastInstallTime <= 0L) {
                    installTimeMatched = false;
                } else {
                    installTimeMatched = installTime <= lastInstallTime;
                }
                if (!installTimeMatched) {
                    GlobalStorageManager.getInstance(config.mContext).saveLastInstallTime(installTime);
                }
                boolean hasNotUpdated = installInfo.second;
                if (!installTimeMatched && hasNotUpdated) {
                    sAppFirstInstallationMap.put(config.mContext, new LinkedList<String>());
                }
            }

            ThinkingAnalyticsSDK instance = instances.get(config.getName());
            if (null == instance) {
                try {
                    if (!TDUtils.isMainProcess(config.mContext)) {
                        instance = new SubprocessThinkingAnalyticsSDK(config);
                    } else {
                        instance = new ThinkingAnalyticsSDK(config);
                        if (sAppFirstInstallationMap.containsKey(config.mContext)) {
                            sAppFirstInstallationMap.get(config.mContext).add(config.getName());
                        }
                    }
                    instances.put(config.getName(), instance);
                } catch (Exception e) {
                    return null;
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

    // only for automatic test
    static Map<String, ThinkingAnalyticsSDK> getInstanceMap(Context context) {
        return sInstanceMap.get(context);
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
     * SDK constructor, you need to pass in an instance of TDConfig. You can obtain the TDConfig instance, perform related configurations, and then initialize the SDK.
     *
     * @param config TDConfig instance
     * @param light  Light instance or not
     */
    ThinkingAnalyticsSDK(TDConfig config, final boolean... light) {
        mConfig = config;
        if (TextUtils.isEmpty(mConfig.getServerUrl()) || TextUtils.isEmpty(mConfig.mToken)) {
            throw new IllegalArgumentException("invalid appId or serverUrl");
        }
        mAutoTrackEventProperties = new JSONObject();

        if (!TDPresetProperties.disableList.contains(TDConstants.KEY_FPS)) {
            if (null == Looper.myLooper()) {
                Looper.prepare();
            }
            TDUtils.listenFPS();
        }
        mCalibratedTimeManager = new CalibratedTimeManager(config);
        mUserOperationHandler = new UserOperationHandler(this, config);
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mSystemInformation = SystemInformation.getInstance(mConfig.mContext, mConfig.getDefaultTimeZone());
                if (light.length > 0 && light[0]) {
                    return;
                }
                if (!mConfig.isNormal() || TDUtils.isLogControlFileExist()) {
                    enableTrackLog(true);
                }
            }
        });
        if (light.length > 0 && light[0]) {
            mEnableTrackOldData = false;
            mTrackTimer = new HashMap<>();
            mMessages = getDataHandleInstance(config.mContext);
            return;
        }
        mEnableTrackOldData = config.trackOldData() && !isOldDataTracked();
        mStorageManager = new CommonStorageManager(config.mContext, config.getName());
        mMessages = getDataHandleInstance(config.mContext);
        mMessages.handleTrackPauseToken(getToken(), mStorageManager.getPausePostFlag());
        // reset Storage Data
        // 1.identifyId
        String identifyId = getIdentifyID();
        if (identifyId == null) {
            identifyId = getRandomID();
        }
        setStatusIdentifyId(identifyId);
        // 2. accountId
        setStatusAccountId(mStorageManager.getLoginId(mEnableTrackOldData, mConfig.mContext));
        // 3. trackStatus
        TATrackStatus trackStatus = TATrackStatus.NORMAL;
        if (mStorageManager.getPausePostFlag()) {
            trackStatus = TATrackStatus.SAVE_ONLY;
        } else if (!mStorageManager.getEnableFlag()) {
            trackStatus = TATrackStatus.PAUSE;
        } else if (mStorageManager.getOptOutFlag()) {
            trackStatus = TATrackStatus.STOP;
        }
        setStatusTrackStatus(trackStatus);

        if (config.mEnableEncrypt) {
            ThinkingDataEncrypt.createInstance(config.getName(), config);
        }

        if (mEnableTrackOldData) {
            mMessages.flushOldData(config.getName());
        }
        mTrackTimer = new HashMap<>();

        mAutoTrackIgnoredActivities = new ArrayList<>();
        mAutoTrackEventTypeList = new ArrayList<>();

        mLifecycleCallbacks = new ThinkingDataActivityLifecycleCallbacks(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = ( Application ) config.mContext.getApplicationContext();
            app.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
        }
        TRouter.init();
        if (config.isEnableMutiprocess() && TDUtils.isMainProcess(config.mContext)) {
            TDReceiver.registerReceiver(config.mContext);
        }

        TDPluginUtils.clearPluginEvent(this);
        TDLog.i(TAG, String.format("[ThinkingData] Info: ThinkingData SDK %s initialize success with mode: %s, APP ID ends with: %s, server url: %s", TDConfig.VERSION,
                config.getMode().name(), TDUtils.getSuffix(config.mToken, 4), config.getServerUrl()));
    }

    /**
     * Enable or disable log printing.
     *
     * @param enableLog true or false
     */
    public static void enableTrackLog(boolean enableLog) {
        TDLog.setEnableLog(enableLog);
    }

    /**
     * Invoke this interface with caution. This interface is used to more accurately set the reporting mode in scenarios where a third party framework or game engine is used.
     *
     * @param libName    Corresponding to the #lib preset attribute in the event table
     * @param libVersion Preset attributes corresponding to the event standard #lib_version
     */
    public static void setCustomerLibInfo(String libName, String libVersion) {
        SystemInformation.setLibraryInfo(libName, libVersion);
    }

    /**
     * Indicates the network type that can be reported.
     */
    public enum ThinkingdataNetworkType {
        /**
         * Default setting: Data is reported in 3G, 4G, 5G, and WiFi environments.
         */
        NETWORKTYPE_DEFAULT,
        /**
         * Data is reported only in the WiFi environment.
         */
        NETWORKTYPE_WIFI,
        /**
         * This parameter is reported on all network types.
         */
        NETWORKTYPE_ALL
    }

    @Override
    public void setNetworkType(ThinkingdataNetworkType type) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        mConfig.setNetworkType(type);
    }

    // autoTrack is used internal without property checking.
    public void autoTrack(String eventName, JSONObject properties) {
        //autoTrack(eventName, properties, null);
        track(eventName, properties, mCalibratedTimeManager.getTime(), false);
    }

    public void autoTrack(String eventName, JSONObject properties, ITime time) {
        //autoTrack(eventName, properties, null);
        track(eventName, properties, time, false);
    }

    @Override
    public void track(String eventName, JSONObject properties) {
        track(eventName, properties, mCalibratedTimeManager.getTime());
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time) {
        track(eventName, properties, mCalibratedTimeManager.getTime(time, null));
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time, TimeZone timeZone) {
        track(eventName, properties, mCalibratedTimeManager.getTime(time, timeZone));
    }

    private void track(String eventName, JSONObject properties, ITime time) {
        track(eventName, properties, time, true);
    }

    private void track(String eventName, JSONObject properties, ITime time, boolean doFormatChecking) {
        track(eventName, properties, time, doFormatChecking, null, null, 0);
    }

    @Override
    public void track(String eventName) {
        track(eventName, null, mCalibratedTimeManager.getTime());
    }

    @Override
    public void track(ThinkingAnalyticsEvent event) {

        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        if (null == event) {
            TDLog.w(TAG, "Ignoring empty event...");
            return;
        }
        ITime time;
        if (event.getEventTime() != null) {
            time = mCalibratedTimeManager.getTime(event.getEventTime(), event.getTimeZone());
        } else {
            time = mCalibratedTimeManager.getTime();
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

        track(event.getEventName(), event.getProperties(), time, true, extraFields, event.getDataType(), 0);
    }

    void track(final String eventName, final JSONObject properties, final ITime time, final boolean doFormatChecking, final Map<String, String> extraFields, final TDConstants.DataType type, final int isTrackDebugType) {

        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        final ThinkingAnalyticsSDK self = this;
        final long systemUpdateTime = SystemClock.elapsedRealtime();
        final String accountId = getStatusAccountId();
        final String distinctId = getStatusIdentifyId();
        final boolean isSaveOnly = isStatusTrackSaveOnly();
        AutoTrackEventType eventType = AutoTrackEventType.autoTrackEventTypeFromEventName(eventName);
        JSONObject autoTrackPro = null;
        if (null != eventType && mAutoTrackDynamicProperties != null) {
            try {
                autoTrackPro = mAutoTrackDynamicProperties.getAutoTrackDynamicProperties();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        final JSONObject finalAutoTrackProperties = autoTrackPro;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {

                if (mConfig.isDisabledEvent(eventName)) {
                    TDLog.d(TAG, "Ignoring disabled event [" + eventName + "]");
                    return;
                }

                try {

                    final boolean isFromSubProcess = properties != null && properties.has(TDPresetUtils.KEY_BUNDLE_ID) && properties.has(KEY_SUBPROCESS_TAG);

                    if (doFormatChecking && PropertyUtils.isInvalidName(eventName)) {
                        TDLog.e(TAG, "[ThinkingData] Error: Incorrect Event name[" + eventName + "]. Event name must be string that starts with English letter, "
                                + "and contains letter, number, and '_'. The max length of the event name is 50.");
                        if (mConfig.shouldThrowException()) {
                            throw new TDDebugException("Invalid event name: " + eventName);
                        }
                    }

                    if (doFormatChecking && !PropertyUtils.checkProperty(properties)) {
                        TDLog.w(TAG, "[ThinkingData] Warning: The data contains invalid key or value: " + properties.toString());
                        if (mConfig.shouldThrowException()) {
                            throw new TDDebugException("Invalid properties. Please refer to SDK debug log for detail reasons.");
                        }
                    }

                    JSONObject finalProperties = obtainDefaultEventProperties(eventName, systemUpdateTime, isFromSubProcess);

                    if (null != finalAutoTrackProperties) {
                        TDUtils.mergeJSONObject(finalAutoTrackProperties, finalProperties, mConfig.getDefaultTimeZone());
                    }

                    if (null != properties) {
                        TDUtils.mergeJSONObject(properties, finalProperties, mConfig.getDefaultTimeZone());
                    }

                    if (!isFromSubProcess) {
                        AutoTrackEventType eventType = AutoTrackEventType.autoTrackEventTypeFromEventName(eventName);
                        if (null != eventType) {
                            if (mAutoTrackEventListener != null) {
                                JSONObject addProperties = mAutoTrackEventListener.eventCallback(eventType, finalProperties);
                                if (null != addProperties) {
                                    TDUtils.mergeJSONObject(addProperties, finalProperties, mConfig.getDefaultTimeZone());
                                }
                            } else {
                                TDLog.i(TAG, "No mAutoTrackEventListener");
                            }
                        }
                    }

                    if (isFromSubProcess && finalProperties.has(KEY_SUBPROCESS_TAG)) {
                        finalProperties.remove(KEY_SUBPROCESS_TAG);
                    }

                    TDConstants.DataType dataType = type == null ? TDConstants.DataType.TRACK : type;
                    DataDescription dataDescription = new DataDescription(self, dataType, finalProperties, time, distinctId, accountId, isSaveOnly);
                    dataDescription.eventName = eventName;
                    dataDescription.isTrackDebugType = isTrackDebugType;
                    if (null != extraFields) {
                        dataDescription.setExtraFields(extraFields);
                    }
                    trackInternal(dataDescription);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void trackWithDebugOnly(String eventName, JSONObject properties) {
        track(eventName, properties, mCalibratedTimeManager.getTime(), false, null, null, 2);
    }

    // 此方法必须在异步队列中执行
    public void trackInternal(final DataDescription dataDescription) {
        dataDescription.mergeSensitiveProperties(mConfig.mContext, new ISensitivePropertiesCallBack() {
            @Override
            public void onSuccess(JSONObject json) {
                if (mConfig.isDebugOnly() || mConfig.isDebug() || dataDescription.isTrackDebugType == 2) {
                    mMessages.postToDebug(dataDescription);
                } else if (dataDescription.saveData) {
                    mMessages.saveClickData(dataDescription);
                } else {
                    mMessages.postClickData(dataDescription);
                }
            }
        });
    }

    private JSONObject obtainDefaultEventProperties(String eventName, long systemUpdateTime, boolean isFromSubProcess) {

        JSONObject finalProperties = new JSONObject();
        try {
            //Preset properties
            TDUtils.mergeJSONObject(new JSONObject(mSystemInformation.getDeviceInfo()), finalProperties, mConfig.getDefaultTimeZone());
            if (!TextUtils.isEmpty(mSystemInformation.getAppVersionName())) {
                finalProperties.put(TDPresetUtils.KEY_APP_VERSION, mSystemInformation.getAppVersionName());
            }
            if (!TDPresetProperties.disableList.contains(TDConstants.KEY_FPS)) {
                finalProperties.put(TDConstants.KEY_FPS, TDUtils.getFPS());
            }
            if (!TDPresetProperties.disableList.contains(TDPresetUtils.KEY_DEVICE_ID)) {
                if (!finalProperties.has(TDPresetUtils.KEY_DEVICE_ID)) {
                    finalProperties.put(TDPresetUtils.KEY_DEVICE_ID, mSystemInformation.getDeviceId());
                }
            }

            //Static public property
            TDUtils.mergeJSONObject(getSuperProperties(), finalProperties, mConfig.getDefaultTimeZone());

            //Automatically collects event custom properties
            if (!isFromSubProcess) {
                JSONObject autoTrackProperties = this.getAutoTrackProperties().optJSONObject(eventName);
                if (autoTrackProperties != null) {
                    TDUtils.mergeJSONObject(autoTrackProperties, finalProperties, mConfig.getDefaultTimeZone());
                }
            }

            //Dynamic public property
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

            if (!isFromSubProcess) {
                final EventTimer eventTimer;
                synchronized (mTrackTimer) {
                    eventTimer = mTrackTimer.get(eventName);
                    mTrackTimer.remove(eventName);
                }
                if (null != eventTimer) {
                    try {
                        Double duration = Double.valueOf(eventTimer.duration(systemUpdateTime));
                        if (duration > 0 && !TDPresetProperties.disableList.contains(TDConstants.KEY_DURATION)) {
                            finalProperties.put(TDConstants.KEY_DURATION, duration);
                        }

                        Double backgroundDuration = Double.valueOf(eventTimer.backgroundDuration());
                        //to-do
                        if (backgroundDuration > 0 && !eventName.equals(TDConstants.APP_END_EVENT_NAME) && !TDPresetProperties.disableList.contains(TDConstants.KEY_BACKGROUND_DURATION)) {
                            finalProperties.put(TDConstants.KEY_BACKGROUND_DURATION, backgroundDuration);
                        }
                    } catch (JSONException e) {
                        // ignore
                        e.printStackTrace();
                    }
                }
            }
            if (!TDPresetProperties.disableList.contains(TDPresetUtils.KEY_NETWORK_TYPE)) {
                finalProperties.put(TDPresetUtils.KEY_NETWORK_TYPE, mSystemInformation.getCurrentNetworkType());
            }
            if (!TDPresetProperties.disableList.contains(TDConstants.KEY_RAM)) {
                finalProperties.put(TDConstants.KEY_RAM, mSystemInformation.getRAM(mConfig.mContext));
            }
            if (!TDPresetProperties.disableList.contains(TDConstants.KEY_DISK)) {
                finalProperties.put(TDConstants.KEY_DISK, mSystemInformation.getDisk(mConfig.mContext, false));
            }


        } catch (Exception ignored) {
            //ignored
        }

        return finalProperties;
    }

    @Override
    public void user_add(JSONObject properties) {
        mUserOperationHandler.user_add(properties, null);
    }

    @Override
    public void user_add(String propertyName, Number propertyValue) {
        mUserOperationHandler.user_add(propertyName, propertyValue);
    }

    /**
     * user_add
     *
     * @param properties JSONObject
     * @param date       Date
     */
    public void user_add(JSONObject properties, Date date) {
        mUserOperationHandler.user_add(properties, date);
    }

    @Override
    public void user_append(JSONObject properties) {
        mUserOperationHandler.user_append(properties, null);
    }

    /**
     * user_append
     *
     * @param properties JSONObject
     * @param date       Date
     */
    public void user_append(JSONObject properties, Date date) {
        mUserOperationHandler.user_append(properties, date);
    }

    @Override
    public void user_uniqAppend(JSONObject property) {
        mUserOperationHandler.user_uniqAppend(property, null);
    }

    public void user_uniqAppend(JSONObject properties, Date date) {
        mUserOperationHandler.user_uniqAppend(properties, date);
    }

    @Override
    public void user_setOnce(JSONObject properties) {
        mUserOperationHandler.user_setOnce(properties, null);
    }

    /**
     * user_setOnce
     *
     * @param properties JSONObject
     * @param date       Date
     */
    public void user_setOnce(JSONObject properties, Date date) {
        mUserOperationHandler.user_setOnce(properties, date);
    }

    @Override
    public void user_set(JSONObject properties) {
        mUserOperationHandler.user_set(properties, null);
    }

    public void user_set(JSONObject properties, Date date) {
        mUserOperationHandler.user_set(properties, date);
    }

    public void user_operations(final TDConstants.DataType type, final JSONObject properties, final Date date) {
        mUserOperationHandler.userOperation(type, properties, date);
    }

    @Override
    public void user_delete() {
        mUserOperationHandler.user_delete(null);
    }

    /**
     * user_delete
     *
     * @param date Date
     */
    public void user_delete(Date date) {
        mUserOperationHandler.user_delete(date);
    }

    @Override
    public void user_unset(String... properties) {
        mUserOperationHandler.user_unset(properties);
    }

    /**
     * user_unset
     *
     * @param properties JSONObject
     * @param date       Date
     */
    public void user_unset(JSONObject properties, Date date) {
        mUserOperationHandler.user_unset(properties, date);
    }

    @Override
    public void identify(final String identity) {

        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        if (TDUtils.isEmpty(identity)) {
            TDLog.w(TAG, "The identity cannot be empty.");
            if (mConfig.shouldThrowException()) {
                throw new TDDebugException("distinct id cannot be empty");
            }
            return;
        }

        TDLog.i(TAG, "[ThinkingData] Info: Setting distinct ID, DistinctId = " + identity);

        setStatusIdentifyId(identity);

        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.setIdentifyId(identity, mConfig.shouldThrowException());
            }
        });

        TDAnalyticsObservable.getInstance().onSetDistinctIdMethodCalled(getLoginId(), identity, mConfig.mToken);
        TAPushUtils.handlePushTokenAfterLogin(this);
    }

    @Override
    public void login(final String loginId) {

        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        if (TDUtils.isEmpty(loginId)) {
            TDLog.w(TAG, "The account id cannot be empty.");
            if (mConfig.shouldThrowException()) {
                throw new TDDebugException("account id cannot be empty");
            }
            return;
        }
        TDLog.i(TAG, "[ThinkingData] Info: Login SDK, AccountId = " + loginId);
        setStatusAccountId(loginId);

        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.saveLoginId(loginId, mConfig.shouldThrowException());
            }
        });

        TDAnalyticsObservable.getInstance().onLoginMethodCalled(loginId, getDistinctId(), mConfig.mToken);

        TAPushUtils.handlePushTokenAfterLogin(this);
    }

    @Override
    public void logout() {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        TDLog.i(TAG, "[ThinkingData] Info: Logout SDK");
        setStatusAccountId(null);
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.logout(mEnableTrackOldData, mConfig.mContext);
            }
        });

        TDAnalyticsObservable.getInstance().onLogoutMethodCalled(getDistinctId(), mConfig.mToken);
    }

    String getLoginId() {
        return getStatusAccountId();
    }

    String getRandomID() {
        return GlobalStorageManager.getInstance(mConfig.mContext).getRandomID();
    }

    private String getIdentifyID() {
        return mStorageManager.getIdentifyId();
    }

    @Override
    public String getDistinctId() {

        String identifyId = getStatusIdentifyId();
        if (identifyId == null) {
            return getRandomID();
        } else {
            return identifyId;
        }
    }

    @Override
    public JSONObject getSuperProperties() {
        return mStorageManager.getSuperProperties();
    }

    @Override
    public void setSuperProperties(final JSONObject superProperties) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.setSuperProperties(superProperties, mConfig.getDefaultTimeZone(), mConfig.shouldThrowException());
            }
        });
    }

    /**
     * Dynamic public attribute interface.
     */
    public interface DynamicSuperPropertiesTracker {
        JSONObject getDynamicSuperProperties();
    }

    public interface AutoTrackDynamicProperties {
        JSONObject getAutoTrackDynamicProperties();
    }

    /**
     * Provides the current event properties and gets the user's new properties.
     */
    public interface AutoTrackEventListener {

        /**
         * GetProperties
         *
         * @param eventType  Current event name
         * @param properties Current Event properties
         * @return JSONObject User Added Properties
         */
        JSONObject eventCallback(AutoTrackEventType eventType, JSONObject properties);
    }

    @Override
    public void setDynamicSuperPropertiesTracker(DynamicSuperPropertiesTracker dynamicSuperPropertiesTracker) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        mDynamicSuperPropertiesTracker = dynamicSuperPropertiesTracker;
    }

    public void setAutoTrackDynamicProperties(AutoTrackDynamicProperties autoTrackDynamicProperties) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        mAutoTrackDynamicProperties = autoTrackDynamicProperties;
    }


    @Override
    public void unsetSuperProperty(final String superPropertyName) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.unsetSuperProperty(superPropertyName);
            }
        });
    }

    @Override
    public void clearSuperProperties() {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.clearSuperProperties();
            }
        });
    }

    @Override
    public void timeEvent(final String eventName) {

        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        final long systemUpdateTime = SystemClock.elapsedRealtime();

        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {

                try {
                    if (PropertyUtils.isInvalidName(eventName)) {
                        TDLog.w(TAG, "timeEvent event name[" + eventName + "] is not valid");
                        //if (mConfig.shouldThrowException()) throw new TDDebugException("Invalid event name for time event");
                        //return;
                    }

                    synchronized (mTrackTimer) {
                        mTrackTimer.put(eventName, new EventTimer(TimeUnit.SECONDS, systemUpdateTime));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null
                && mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        ThinkingDataIgnoreTrackAppViewScreenAndAppClick annotation1 =
                activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreenAndAppClick.class);
        if (null != annotation1 && (TextUtils.isEmpty(annotation1.appId())
                || getToken().equals(annotation1.appId()))) {
            return true;
        }

        ThinkingDataIgnoreTrackAppViewScreen annotation2 = activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class);
        if (null != annotation2 && (TextUtils.isEmpty(annotation2.appId())
                || getToken().equals(annotation2.appId()))) {
            return true;
        }


        return false;
    }

    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        return eventType != null && !mAutoTrackEventTypeList.contains(eventType);
    }

    public boolean isAutoTrackEnabled() {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) {
            return false;
        }
        return mAutoTrack;
    }

    /**
     * auto-tracking event types.
     */
    public enum AutoTrackEventType {
        /**
         * APP active event ta_app_start.
         */
        APP_START(TDConstants.APP_START_EVENT_NAME),
        /**
         * APP inactive event ta_app_end.
         */
        APP_END(TDConstants.APP_END_EVENT_NAME),
        /**
         * widget click event ta_app_click.
         */
        APP_CLICK(TDConstants.APP_CLICK_EVENT_NAME),
        /**
         * page browsing event ta_app_view.
         */
        APP_VIEW_SCREEN(TDConstants.APP_VIEW_EVENT_NAME),
        /**
         * APP crash event ta_app_crash.
         */
        APP_CRASH(TDConstants.APP_CRASH_EVENT_NAME),
        /**
         * APP install event ta_app_install.
         */
        APP_INSTALL(TDConstants.APP_INSTALL_EVENT_NAME);

        private final String eventName;

        /**
         * Obtain the auto-tracking event type based on the event name
         *
         * @param eventName event name
         * @return {@link AutoTrackEventType}
         */
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
                default:
                    break;
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

    /* package */
    public void trackViewScreenInternal(String url, JSONObject properties) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) {
            return;
        }
        try {
            if ((!TextUtils.isEmpty(url) || properties != null)) {
                JSONObject trackProperties = new JSONObject();
                if (!TextUtils.isEmpty(mLastScreenUrl) && !TDPresetProperties.disableList.contains(TDConstants.KEY_REFERRER)) {
                    trackProperties.put(TDConstants.KEY_REFERRER, mLastScreenUrl);
                }
                if (!TDPresetProperties.disableList.contains(TDConstants.KEY_URL)) {
                    trackProperties.put(TDConstants.KEY_URL, url);
                }
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
    public void ignoreAppViewEventInExtPackage() {
        this.mIgnoreAppViewInExtPackage = true;
    }

    public boolean isIgnoreAppViewInExtPackage() {
        return this.mIgnoreAppViewInExtPackage;
    }

    @Override
    public void trackViewScreen(Activity activity) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) {
            return;
        }
        try {
            if (activity == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            if (!TDPresetProperties.disableList.contains(TDConstants.SCREEN_NAME)) {
                properties.put(TDConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            }
            TDUtils.getScreenNameAndTitleFromActivity(properties, activity);

            if (activity instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = ( ScreenAutoTracker ) activity;

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
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) {
            return;
        }
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

            if (!TextUtils.isEmpty(title) && !TDPresetProperties.disableList.contains(TDConstants.TITLE)) {
                properties.put(TDConstants.TITLE, title);
            }
            if (!TDPresetProperties.disableList.contains(TDConstants.SCREEN_NAME)) {
                properties.put(TDConstants.SCREEN_NAME, screenName);
            }
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = ( ScreenAutoTracker ) fragment;

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
    public void trackViewScreen(final Object fragment) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) {
            return;
        }
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

        if (!(supportFragmentClass != null && supportFragmentClass.isInstance(fragment))
                && !(appFragmentClass != null && appFragmentClass.isInstance(fragment))
                && !(androidXFragmentClass != null && androidXFragmentClass.isInstance(fragment))) {
            return;
        }

        try {
            JSONObject properties = new JSONObject();
            String screenName = fragment.getClass().getCanonicalName();

            String title = TDUtils.getTitleFromFragment(fragment, getToken());

            Activity activity = null;
            try {
                Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                activity = ( Activity ) getActivityMethod.invoke(fragment);
            } catch (Exception e) {
                //ignored
            }
            if (activity != null) {
                if (TextUtils.isEmpty(title)) {
                    title = TDUtils.getActivityTitle(activity);
                }
                screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), screenName);
            }

            if (!TextUtils.isEmpty(title) && !TDPresetProperties.disableList.contains(TDConstants.TITLE)) {
                properties.put(TDConstants.TITLE, title);
            }
            if (!TDPresetProperties.disableList.contains(TDConstants.SCREEN_NAME)) {
                properties.put(TDConstants.SCREEN_NAME, screenName);
            }
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = ( ScreenAutoTracker ) fragment;

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
            e.printStackTrace();
        }
    }

    /* package */
    public void appEnterBackground() {
        synchronized (mTrackTimer) {
            try {
                Iterator iterator = mTrackTimer.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = ( Map.Entry ) iterator.next();
                    if (entry != null) {
                        if (TDConstants.APP_END_EVENT_NAME.equals(entry.getKey().toString())) {
                            continue;
                        }
                        EventTimer eventTimer = ( EventTimer ) entry.getValue();
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

    /* package */
    public void appBecomeActive() {
        synchronized (mTrackTimer) {
            try {
                Iterator iterator = mTrackTimer.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = ( Map.Entry ) iterator.next();
                    if (entry != null) {
                        EventTimer eventTimer = ( EventTimer ) entry.getValue();
                        if (eventTimer != null) {
                            long backgroundDuration = eventTimer.getBackgroundDuration() + SystemClock.elapsedRealtime() - eventTimer.getStartTime();
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                            eventTimer.setBackgroundDuration(backgroundDuration);
                        }
                    }
                }
            } catch (Exception e) {
                TDLog.i(TAG, "appBecomeActive error:" + e.getMessage());
            } finally {
                flush();
            }
        }
    }

    @Override
    public void trackAppInstall() {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        enableAutoTrack(new ArrayList<>(Collections.singletonList(AutoTrackEventType.APP_INSTALL)));
    }

    /**
     * Start auto-tracking event and set custom properties.
     *
     * @param eventTypeList auto-tracking event collections
     * @param properties    user-defined attribute
     */
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList, JSONObject properties) {
        setAutoTrackProperties(eventTypeList, properties);
        enableAutoTrack(eventTypeList);
    }

    /**
     * Start automatic collection and set event callbacks.
     *
     * @param eventTypeList          auto-tracking event collections
     * @param autoTrackEventListener callback interface
     */
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList, AutoTrackEventListener autoTrackEventListener) {
        mAutoTrackEventListener = autoTrackEventListener;
        enableAutoTrack(eventTypeList);
    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        mAutoTrack = true;
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
        }

        if (eventTypeList.contains(AutoTrackEventType.APP_INSTALL)) {
            synchronized (sInstanceMap) {
                if (sAppFirstInstallationMap.containsKey(mConfig.mContext)
                        && sAppFirstInstallationMap.get(mConfig.mContext).contains(getToken())) {
                    track(TDConstants.APP_INSTALL_EVENT_NAME);
                    flush();
                    sAppFirstInstallationMap.get(mConfig.mContext).remove(getToken());
                    TRouter.getInstance().build(TRouterMap.PRESET_TEMPLATE_ROUTE_PATH).withAction("triggerAppInstallEvent")
                            .withString("appId", getToken()).navigation();
                }
            }
        }

        if (eventTypeList.contains(AutoTrackEventType.APP_CRASH)) {
            mTrackCrash = true;
            TAExceptionHandler handler = TAExceptionHandler.getInstance(mConfig.mContext);
            if (null != handler) {
                handler.initExceptionHandler();
            }
        }

        if (!mAutoTrackEventTypeList.contains(AutoTrackEventType.APP_END)
                && eventTypeList.contains(AutoTrackEventType.APP_END)) {
            timeEvent(TDConstants.APP_END_EVENT_NAME);
            mLifecycleCallbacks.updateShouldTrackEvent(true);
        }


        synchronized (this) {
            final long systemUpdateTime = SystemClock.elapsedRealtime();
            mAutoTrackStartTime = mCalibratedTimeManager.getTime();
            mAutoTrackStartProperties = obtainDefaultEventProperties(TDConstants.APP_START_EVENT_NAME, systemUpdateTime, false);
        }

        mAutoTrackEventTypeList.clear();
        mAutoTrackEventTypeList.addAll(eventTypeList);
        if (mAutoTrackEventTypeList.contains(AutoTrackEventType.APP_START)) {
            mLifecycleCallbacks.onAppStartEventEnabled();
        }
    }

    /**
     * Get the local area/country code
     *
     * @return local region
     */
    public static String getLocalRegion() {
        return Locale.getDefault().getCountry();
    }

    /**
     * Set custom properties for automatic collection events
     *
     * @param eventTypeList            event list
     * @param autoTrackEventProperties JSONObject
     */
    @Override
    public void setAutoTrackProperties(List<AutoTrackEventType> eventTypeList, JSONObject autoTrackEventProperties) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        try {
            if (autoTrackEventProperties == null || !PropertyUtils.checkProperty(autoTrackEventProperties)) {
                if (mConfig.shouldThrowException()) {
                    throw new TDDebugException("Set autoTrackEvent properties failed. Please refer to the SDK debug log for details.");
                }
                return;
            }
            JSONObject allAutoTrackEventProperties = new JSONObject();
            for (AutoTrackEventType eventType : eventTypeList) {
                JSONObject newJSONObject = new JSONObject();
                TDUtils.mergeJSONObject(autoTrackEventProperties, newJSONObject, mConfig.getDefaultTimeZone());
                allAutoTrackEventProperties.put(eventType.getEventName(), newJSONObject);
            }
            synchronized (mAutoTrackEventProperties) {
                TDUtils.mergeNestedJSONObject(allAutoTrackEventProperties, mAutoTrackEventProperties, mConfig.getDefaultTimeZone());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush() {
        final boolean hasDisabled = getStatusHasDisabled();
        final boolean isSaveOnly = isStatusTrackSaveOnly();
        if (hasDisabled || isSaveOnly) {
            return;
        }
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mMessages.flush(getToken());
            }
        });
    }

    /* package */ List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

    @Override
    public void ignoreViewType(Class viewType) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

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
        if (mAutoTrackIgnoredActivities != null
                && mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        ThinkingDataIgnoreTrackAppViewScreenAndAppClick annotation1 =
                activity.getAnnotation(ThinkingDataIgnoreTrackAppViewScreenAndAppClick.class);
        if (null != annotation1 && (TextUtils.isEmpty(annotation1.appId())
                || getToken().equals(annotation1.appId()))) {
            return true;
        }

        ThinkingDataIgnoreTrackAppClick annotation2 = activity.getAnnotation(ThinkingDataIgnoreTrackAppClick.class);
        return null != annotation2 && (TextUtils.isEmpty(annotation2.appId())
                || getToken().equals(annotation2.appId()));

    }

    /* package */ boolean isTrackFragmentAppViewScreenEnabled() {
        return this.mTrackFragmentAppViewScreen;
    }

    @Override
    public void trackFragmentAppViewScreen() {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        this.mTrackFragmentAppViewScreen = true;
    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

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
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

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
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        if (view != null && !TextUtils.isEmpty(viewID)) {
            TDUtils.setTag(getToken(), view, R.id.thinking_analytics_tag_view_id, viewID);
        }
    }

    @Override
    public void setViewID(Dialog view, String viewID) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

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
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        if (view == null || properties == null) {
            return;
        }
        TDUtils.setTag(getToken(), view, R.id.thinking_analytics_tag_view_properties, properties);
    }

    @Override
    public void ignoreView(View view) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;

        if (view != null) {
            TDUtils.setTag(getToken(), view, R.id.thinking_analytics_tag_view_ignored, "1");
        }
    }

    @SuppressLint("AddJavascriptInterface")
    @Override
    public void setJsBridge(WebView webView) {
        if (null == webView) {
            TDLog.d(TAG, "SetJsBridge failed due to parameter webView is null");
            if (mConfig.shouldThrowException()) {
                throw new TDDebugException("webView cannot be null for setJsBridge");
            }
            return;
        }

        webView.getSettings().setJavaScriptEnabled(true);
        Map<String, Object> deviceInfo = SystemInformation.getInstance(mConfig.mContext, mConfig.getDefaultTimeZone()).getDeviceInfo();
        webView.addJavascriptInterface(new TDWebAppInterface(this, deviceInfo), "ThinkingData_APP_JS_Bridge");
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
            Map<String, Object> deviceInfo = SystemInformation.getInstance(mConfig.mContext, mConfig.getDefaultTimeZone()).getDeviceInfo();
            addJavascriptInterface.invoke(x5WebView, new TDWebAppInterface(this, deviceInfo), "ThinkingData_APP_JS_Bridge");
        } catch (Exception e) {
            TDLog.w(TAG, "setJsBridgeForX5WebView failed: " + e.toString());
        }

    }

    @Override
    public String getDeviceId() {
        return SystemInformation.getInstance(mConfig.mContext, mConfig.getDefaultTimeZone()).getDeviceId();
    }

    /* package */ public interface InstanceProcessor {
        void process(ThinkingAnalyticsSDK instance);
    }

    public static void allInstances(InstanceProcessor processor) {
        synchronized (sInstanceMap) {
            for (final Map<String, ThinkingAnalyticsSDK> instances : sInstanceMap.values()) {
                for (final ThinkingAnalyticsSDK instance : instances.values()) {
                    processor.process(instance);
                }
            }
        }
    }

    public static ThinkingAnalyticsSDK getInstanceByAppId(String name) {
        synchronized (sInstanceMap) {
            for (final Map<String, ThinkingAnalyticsSDK> instances : sInstanceMap.values()) {
                for (final ThinkingAnalyticsSDK instance : instances.values()) {
                    if (TextUtils.isEmpty(name)) {
                        return instance;
                    }
                    if (TextUtils.equals(name, instance.getToken())) {
                        return instance;
                    }
                }
            }
        }
        return null;
    }

    /* package */
    public boolean shouldTrackCrash() {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) {
            return false;
        }
        return mTrackCrash;
    }

    /**
     * TATrackStatus
     */
    public enum TATrackStatus {
        //Stop SDK data tracking
        PAUSE,
        //Stop SDK data tracking to clear the cache
        STOP,
        //Stop SDK data reporting
        SAVE_ONLY,
        //resume all status
        NORMAL,
    }

    @Override
    public void setTrackStatus(final TATrackStatus status) {

        setStatusTrackStatus(status);

        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {

                switch (status) {
                    case PAUSE:
                        mStorageManager.saveOptOutFlag(false);
                        mStorageManager.savePausePostFlag(false);
                        mMessages.handleTrackPauseToken(getToken(), false);
                        enableTracking(false);
                        TDLog.i(TAG, "[ThinkingData] Info: Change Status to Pause");
                        break;
                    case STOP:
                        mStorageManager.saveEnableFlag(true);
                        mStorageManager.savePausePostFlag(false);
                        mMessages.handleTrackPauseToken(getToken(), false);
                        optOutTracking();
                        TDLog.i(TAG, "[ThinkingData] Info: Change Status to Stop");
                        break;
                    case SAVE_ONLY:
                        mStorageManager.saveEnableFlag(true);
                        mStorageManager.saveOptOutFlag(false);
                        mStorageManager.savePausePostFlag(true);
                        mMessages.handleTrackPauseToken(getToken(), true);
                        TDLog.i(TAG, "[ThinkingData] Info: Change Status to SaveOnly");
                        break;
                    case NORMAL:
                        mStorageManager.saveEnableFlag(true);
                        mStorageManager.saveOptOutFlag(false);
                        mStorageManager.savePausePostFlag(false);
                        mMessages.handleTrackPauseToken(getToken(), false);
                        TDLog.i(TAG, "[ThinkingData] Info: Change Status to Normal");
                        flush();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * @param enabled true or false
     */
    @Override
    @Deprecated
    public void enableTracking(final boolean enabled) {

        if (!enabled) {
            flush();
        }
        setStatusTrackStatus(TATrackStatus.PAUSE);
        mStorageManager.saveEnableFlag(enabled);
    }

    @Override
    @Deprecated
    public void optOutTrackingAndDeleteUser() {
        setStatusTrackStatus(TATrackStatus.STOP);
        final ThinkingAnalyticsSDK self = this;
        final ITime time = mCalibratedTimeManager.getTime();
        final String identifyId = getStatusIdentifyId();
        final String accountId = getStatusAccountId();
        DataDescription userDel = new DataDescription(self, TDConstants.DataType.USER_DEL, null, time, identifyId, accountId, false);
        userDel.setNoCache();
        trackInternal(userDel);
        optOutTracking();
    }

    @Override
    @Deprecated
    public void optOutTracking() {
        setStatusTrackStatus(TATrackStatus.PAUSE);
        mStorageManager.saveOptOutFlag(true);
        mMessages.emptyMessageQueue(getToken());

        synchronized (mTrackTimer) {
            mTrackTimer.clear();
        }

        setStatusAccountId(null);
        setStatusIdentifyId(getRandomID());
        mStorageManager.clearIdentify();
        mStorageManager.clearLoginId();
        mStorageManager.clearSuperProperties();
    }

    @Override
    @Deprecated
    public void optInTracking() {
        setStatusTrackStatus(TATrackStatus.NORMAL);
        mStorageManager.saveOptOutFlag(false);
        mMessages.flush(getToken());
    }

    /**
     * Status of the current instance Enable. Enable enableTracking.
     *
     * @return true or false .
     */
    public boolean isEnabled() {
        return mStorageManager.getEnableFlag();
    }

    /**
     * Whether the current instance can be reported.
     *
     * @return true or false
     */
    boolean hasDisabled() {
        return !isEnabled() || hasOptOut();
    }

    /**
     * Current instance OptOut status. optOutTracking(), optInTracking() Settings.
     *
     * @return true or false
     */
    public boolean hasOptOut() {
        return mStorageManager.getOptOutFlag();
    }

    /**
     * Create lightweight SDK instances. Lightweight SDK instances do not support caching of local account ids, guest ids, public properties, etc.
     *
     * @return SDK instance
     */
    @Override
    public ThinkingAnalyticsSDK createLightInstance() {
        return new LightThinkingAnalyticsSDK(mConfig);
    }

    @Override
    public TDPresetProperties getPresetProperties() {
        JSONObject presetProperties = SystemInformation.getInstance(mConfig.mContext).currentPresetProperties();
        String networkType = SystemInformation.getInstance(mConfig.mContext).getCurrentNetworkType();
        double zoneOffset = mCalibratedTimeManager.getTime().getZoneOffset();
        try {
            if (!TDPresetProperties.disableList.contains(TDPresetUtils.KEY_NETWORK_TYPE)) {
                presetProperties.put(TDPresetUtils.KEY_NETWORK_TYPE, networkType);
            }

            presetProperties.put(TDConstants.KEY_ZONE_OFFSET, zoneOffset);

            if (!TDPresetProperties.disableList.contains(TDConstants.KEY_RAM)) {
                presetProperties.put(TDConstants.KEY_RAM, SystemInformation.getInstance(mConfig.mContext).getRAM(mConfig.mContext));
            }
            if (!TDPresetProperties.disableList.contains(TDConstants.KEY_DISK)) {
                presetProperties.put(TDConstants.KEY_DISK, SystemInformation.getInstance(mConfig.mContext).getDisk(mConfig.mContext, false));
            }
            if (!TDPresetProperties.disableList.contains(TDConstants.KEY_FPS)) {
                presetProperties.put(TDConstants.KEY_FPS, TDUtils.getFPS());
            }
            if (!TDPresetProperties.disableList.contains(TDPresetUtils.KEY_DEVICE_ID)) {
                if (!presetProperties.has(TDPresetUtils.KEY_DEVICE_ID)) {
                    presetProperties.put(TDPresetUtils.KEY_DEVICE_ID, SystemInformation.getInstance(mConfig.mContext).getDeviceId());
                }
            }
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return new TDPresetProperties(presetProperties);
    }

    private final JSONObject mAutoTrackEventProperties;

    @Override
    public JSONObject getAutoTrackProperties() {
        return mAutoTrackEventProperties;
    }

    public void trackAppCrashAndEndEvent(JSONObject properties) {
        mLifecycleCallbacks.trackAppCrashAndEndEvent(properties);
    }

    public String getToken() {
        return mConfig.getName();
    }

    public String getTimeString(Date date) {
        return mCalibratedTimeManager.getTime(date, mConfig.getDefaultTimeZone()).getTime();
    }

    public String getCurrentTime() {
        return mCalibratedTimeManager.getTime().getTime();
    }

    public Double getCurrentZoneOffset() {
        return mCalibratedTimeManager.getTime().getZoneOffset();
    }

    //Local cache (SharePreference related variable), unique to a single instance. Its file name is PREFERENCE_NAME_{{name}}
    private CommonStorageManager mStorageManager;

    public CalibratedTimeManager mCalibratedTimeManager;

    //User attribute processing
    private final UserOperationHandler mUserOperationHandler;

    //Dynamic public attribute interface
    private DynamicSuperPropertiesTracker mDynamicSuperPropertiesTracker;
    public AutoTrackDynamicProperties mAutoTrackDynamicProperties;


    //Automatic event collection callback interface
    private AutoTrackEventListener mAutoTrackEventListener;

    //Cache the cumulative time of timeEvent
    final Map<String, EventTimer> mTrackTimer;

    //Automatic collection of relevant variables
    private boolean mAutoTrack;
    private boolean mTrackCrash;
    private boolean mTrackFragmentAppViewScreen;
    private boolean mIgnoreAppViewInExtPackage = false;

    private List<AutoTrackEventType> mAutoTrackEventTypeList;
    private List<Integer> mAutoTrackIgnoredActivities;
    private List<Class> mIgnoredViewTypeList = new ArrayList<>();
    private String mLastScreenUrl;
    private ThinkingDataActivityLifecycleCallbacks mLifecycleCallbacks;

    //Save all instance objects that have been initialized
    private static final Map<Context, Map<String, ThinkingAnalyticsSDK>> sInstanceMap = new HashMap<>();

    // Logic used to collect APP installation events
    private static final Map<Context, List<String>> sAppFirstInstallationMap = new HashMap<>();

    // Whether to synchronize old version data, v1.3.0+ is compatible with previous versions of the internal use variable
    private final boolean mEnableTrackOldData;

    protected final DataHandle mMessages;
    public TDConfig mConfig;
    private SystemInformation mSystemInformation;

    static final String TAG = "ThinkingAnalyticsSDK";

    // Special processing of startup events to record the time when automatic collection is enabled
    private ITime mAutoTrackStartTime;

    public synchronized ITime getAutoTrackStartTime() {
        return mAutoTrackStartTime;
    }

    private JSONObject mAutoTrackStartProperties;


    public synchronized JSONObject getAutoTrackStartProperties() {
        return mAutoTrackStartProperties == null ? new JSONObject() : mAutoTrackStartProperties;
    }

    DynamicSuperPropertiesTracker getDynamicSuperPropertiesTracker() {
        return mDynamicSuperPropertiesTracker;
    }

    public List<AutoTrackEventType> getAutoTrackEventTypeList() {
        return mAutoTrackEventTypeList;
    }

    public static ICalibratedTime getCalibratedTime() {
        return CalibratedTimeManager.getCalibratedTime();
    }

    public static void calibrateTime(long timestamp) {
        if (timestamp <= 0) return;
        TDLog.i(TAG, "[ThinkingData] Info: Time Calibration with timestamp(" + timestamp + ")");
        CalibratedTimeManager.calibrateTime(timestamp);
    }

    public static void calibrateTimeWithNtp(String... ntpServer) {
        CalibratedTimeManager.calibrateTimeWithNtp(ntpServer);
    }

    /**
     * Synchronize three-party data.
     */
    @Override
    public void enableThirdPartySharing(int types) {
        TRouter.getInstance().build("/thingkingdata/third/party")
                .withAction("enableThirdPartySharing")
                .withInt("type", types)
                .withObject("instance", this)
                .withString("loginId", getLoginId())
                .navigation();
    }

    /**
     * Synchronize three-party data
     *
     * @param type int
     * @param obj  extras
     */
    public void enableThirdPartySharing(int type, Object obj) {
        TRouter.getInstance().build("/thingkingdata/third/party")
                .withAction("enableThirdPartySharingWithParams")
                .withInt("type", type)
                .withObject("instance", this)
                .withString("loginId", getLoginId())
                .withObject("params", obj)
                .navigation();
    }

    // TA实例的内存状态，在获取值的时候不受异步队列影响
    // Instance Status
    protected String _statusIdentifyId;
    protected String _statusAccountId;
    protected TATrackStatus _statusTrackStatus;

    public synchronized String getStatusIdentifyId() {
        return _statusIdentifyId;
    }

    protected synchronized void setStatusIdentifyId(String value) {
        _statusIdentifyId = value;
    }

    public synchronized String getStatusAccountId() {
        return _statusAccountId;
    }

    protected synchronized void setStatusAccountId(String value) {
        _statusAccountId = value;
    }

    protected synchronized TATrackStatus getStatusTrackStatus() {
        return _statusTrackStatus;
    }

    protected synchronized void setStatusTrackStatus(TATrackStatus value) {
        _statusTrackStatus = value;
    }

    public synchronized boolean getStatusHasDisabled() {
        final TATrackStatus trackStatus = getStatusTrackStatus();
        if (trackStatus == TATrackStatus.STOP || trackStatus == TATrackStatus.PAUSE) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean isStatusTrackSaveOnly() {
        return getStatusTrackStatus() == TATrackStatus.SAVE_ONLY;
    }

}

/**
 * Lightweight instance, does not support local cache, shares APP ID with main instance.
 */
class LightThinkingAnalyticsSDK extends ThinkingAnalyticsSDK {
    private String mDistinctId;
    private String mAccountId;
    private final JSONObject mSuperProperties;
    private boolean mEnabled = true;

    LightThinkingAnalyticsSDK(TDConfig config) {
        super(config, true);
        mSuperProperties = new JSONObject();
        setStatusIdentifyId(getRandomID());
    }

    @Override
    public void identify(String identity) {
        if (TDUtils.isEmpty(identity)) {
            TDLog.w(TAG, "The identity cannot be empty.");
            if (mConfig.shouldThrowException()) {
                throw new TDDebugException("distinct id cannot be empty");
            }
            return;
        }
        setStatusIdentifyId(identity);
    }

    @Override
    public void setSuperProperties(final JSONObject superProperties) {

        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (superProperties == null || !PropertyUtils.checkProperty(superProperties)) {
                        return;
                    }

                    TDUtils.mergeJSONObject(superProperties, mSuperProperties, mConfig.getDefaultTimeZone());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void unsetSuperProperty(final String superPropertyName) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    @Override
    public void clearSuperProperties() {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                synchronized (mSuperProperties) {
                    Iterator keys = mSuperProperties.keys();
                    while (keys.hasNext()) {
                        keys.next();
                        keys.remove();
                    }
                }
            }
        });
    }

    @Override
    public String getDistinctId() {
        String identifyId = getStatusIdentifyId();
        if (identifyId == null) {
            return getRandomID();
        } else {
            return identifyId;
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
    public void login(final String accountId) {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) {
            return;
        }
        setStatusAccountId(accountId);
    }

    @Override
    public void logout() {
        final boolean hasDisabled = getStatusHasDisabled();
        if (hasDisabled) return;
        setStatusAccountId(null);
    }

    @Override
    String getLoginId() {
        return getStatusAccountId();
    }

    @Override
    public void optOutTracking() {
    }

    @Override
    public void optInTracking() {
    }

    @Override
    public boolean isEnabled() {
        return !getStatusHasDisabled();
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
        if (enabled) {
            setStatusTrackStatus(TATrackStatus.NORMAL);
        } else {
            setStatusTrackStatus(TATrackStatus.PAUSE);
        }
    }

    @Override
    public void setTrackStatus(final TATrackStatus status) {
        setStatusTrackStatus(status);
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case PAUSE:
                        mMessages.handleTrackPauseToken(getToken(), false);
                        enableTracking(false);
                        break;
                    case STOP:
                        mEnabled = true;
                        mMessages.handleTrackPauseToken(getToken(), false);
                        optOutTracking();
                        break;
                    case SAVE_ONLY:
                        mEnabled = true;
                        mMessages.handleTrackPauseToken(getToken(), true);
                        break;
                    case NORMAL:
                        mEnabled = true;
                        mMessages.handleTrackPauseToken(getToken(), false);
                        flush();
                        break;
                    default:
                        break;
                }
            }
        });

    }
}

/**
 * Child process instance.
 */
class SubprocessThinkingAnalyticsSDK extends ThinkingAnalyticsSDK {
    Context mContext;
    String currentProcessName;

    public SubprocessThinkingAnalyticsSDK(TDConfig config) {
        super(config);
        this.mContext = config.mContext;
        mAutoTrackEventProperties = new JSONObject();
        currentProcessName = TDUtils.getCurrentProcessName(mContext);
    }

    @Override
    public void identify(String distinctId) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_IDENTIFY);
        if (distinctId != null && distinctId.length() > 0) {
            intent.putExtra(TDConstants.KEY_DISTINCT_ID, distinctId);
        } else {
            intent.putExtra(TDConstants.KEY_DISTINCT_ID, "");
        }
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void login(String accountId) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_LOGIN);
        if (accountId != null && accountId.length() > 0) {
            intent.putExtra(TDConstants.KEY_ACCOUNT_ID, accountId);
        } else {
            intent.putExtra(TDConstants.KEY_ACCOUNT_ID, "");
        }
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void flush() {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_FLUSH);
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void logout() {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_LOGOUT);
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void user_operations(TDConstants.DataType type, JSONObject properties, Date date) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_USER_PROPERTY_SET);
        intent.putExtra(TDConstants.TD_KEY_USER_PROPERTY_SET_TYPE, type.getType());
        if (properties != null) {
            JSONObject realProperties = new JSONObject();
            try {
                TDUtils.mergeJSONObject(properties, realProperties, mConfig.getDefaultTimeZone());
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
            intent.putExtra(TDConstants.KEY_PROPERTIES, realProperties.toString());
        }
        if (date != null) {
            intent.putExtra(TDConstants.TD_KEY_DATE, date.getTime());
        }
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }
    }

    private final JSONObject mAutoTrackEventProperties;

    /**
     * Set custom properties for automatic collection events.
     *
     * @param eventTypeList            event list
     * @param autoTrackEventProperties JSONObject
     */
    @Override
    public void setAutoTrackProperties(List<AutoTrackEventType> eventTypeList, JSONObject autoTrackEventProperties) {
        if (hasDisabled()) {
            return;
        }
        try {
            if (autoTrackEventProperties == null || !PropertyUtils.checkProperty(autoTrackEventProperties)) {
                if (mConfig.shouldThrowException()) {
                    throw new TDDebugException("Set autoTrackEvent properties failed. Please refer to the SDK debug log for details.");
                }
                return;
            }
            JSONObject allAutoTrackEventProperties = new JSONObject();
            for (AutoTrackEventType eventType : eventTypeList) {
                JSONObject newJSONObject = new JSONObject();
                TDUtils.mergeJSONObject(autoTrackEventProperties, newJSONObject, mConfig.getDefaultTimeZone());
                allAutoTrackEventProperties.put(eventType.getEventName(), newJSONObject);
            }
            synchronized (mAutoTrackEventProperties) {
                TDUtils.mergeNestedJSONObject(allAutoTrackEventProperties, mAutoTrackEventProperties, mConfig.getDefaultTimeZone());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public JSONObject getAutoTrackProperties() {
        return mAutoTrackEventProperties;
    }

    @Override
    public void autoTrack(String eventName, JSONObject properties) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.KEY_EVENT_NAME, eventName);
        properties = properties == null ? new JSONObject() : properties;
        JSONObject realProperties = obtainProperties(eventName, properties);
        try {
            JSONObject autoTrackProperties = this.getAutoTrackProperties().optJSONObject(eventName);
            if (autoTrackProperties != null) {
                TDUtils.mergeJSONObject(autoTrackProperties, realProperties, mConfig.getDefaultTimeZone());
            }
            intent.putExtra(TDConstants.KEY_PROPERTIES, realProperties.toString());
            intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK_AUTO_EVENT);
            if (null != mContext) {
                mContext.sendBroadcast(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject obtainProperties(String eventName, JSONObject properties) {

        JSONObject realProperties = new JSONObject();
        final long systemUpdateTime = SystemClock.elapsedRealtime();
        try {
            // add SubProcess tag
            realProperties.put(KEY_SUBPROCESS_TAG, true);

            if (!TDPresetProperties.disableList.contains(TDPresetUtils.KEY_BUNDLE_ID)) {
                realProperties.put(TDPresetUtils.KEY_BUNDLE_ID, currentProcessName);
            }
            double duration = getEventDuration(eventName, systemUpdateTime);
            if (duration > 0 && !TDPresetProperties.disableList.contains(TDConstants.KEY_DURATION)) {
                realProperties.put(TDConstants.KEY_DURATION, duration);
            }
        } catch (JSONException exception) {
            //ignored
        }
        if (getDynamicSuperPropertiesTracker() != null) {
            JSONObject dynamicProperties = getDynamicSuperPropertiesTracker().getDynamicSuperProperties();
            if (dynamicProperties != null) {
                try {
                    TDUtils.mergeJSONObject(dynamicProperties, realProperties, mConfig.getDefaultTimeZone());
                } catch (JSONException exception) {
                    exception.printStackTrace();
                }
            }
        }
        try {
            TDUtils.mergeJSONObject(properties, realProperties, mConfig.getDefaultTimeZone());
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return realProperties;
    }

    @Override
    public void track(ThinkingAnalyticsEvent event) {
        Intent intent = getIntent();
        JSONObject properties;
        switch (event.getDataType()) {
            case TRACK_OVERWRITE:
                intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK_OVERWRITE_EVENT);
                break;
            case TRACK_UPDATE:
                intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK_UPDATABLE_EVENT);
                break;
            case TRACK:
                intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK_FIRST_EVENT);
                break;
            default:
                break;
        }
        intent.putExtra(TDConstants.KEY_EVENT_NAME, event.getEventName());
        properties = event.getProperties() == null ? new JSONObject() : event.getProperties();
        JSONObject realProperties = obtainProperties(event.getEventName(), properties);
        intent.putExtra(TDConstants.KEY_PROPERTIES, realProperties.toString());
        if (event.getEventTime() != null) {
            intent.putExtra(TDConstants.TD_KEY_DATE, event.getEventTime().getTime());
        }
        if (event.getTimeZone() != null) {
            intent.putExtra(TDConstants.TD_KEY_TIMEZONE, event.getTimeZone().getID());
        }
        intent.putExtra(TDConstants.TD_KEY_EXTRA_FIELD, event.getExtraValue());
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }

    }

    @Override
    public void track(String eventName) {
        track(eventName, null, null, null);
    }

    @Override
    public void track(String eventName, JSONObject properties) {
        track(eventName, properties, null, null);
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time) {
        track(eventName, properties, time, null);
    }

    @Override
    public void track(String eventName, JSONObject properties, Date time, TimeZone timeZone) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_TRACK);
        intent.putExtra(TDConstants.KEY_EVENT_NAME, eventName);
        if (properties == null) {
            properties = new JSONObject();
        }
        JSONObject realProperties = obtainProperties(eventName, properties);
        intent.putExtra(TDConstants.KEY_PROPERTIES, realProperties.toString());
        if (time != null) {
            intent.putExtra(TDConstants.TD_KEY_DATE, time.getTime());
        }
        if (timeZone != null) {
            intent.putExtra(TDConstants.TD_KEY_TIMEZONE, timeZone.getID());
        }
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }
    }

    public Intent getIntent() {
        Intent intent = new Intent();
        String mainProcessName = TDUtils.getMainProcessName(mContext);
        if (mainProcessName.length() == 0) {
            mainProcessName = TDConstants.TD_RECEIVER_FILTER;
        } else {
            mainProcessName = mainProcessName + "." + TDConstants.TD_RECEIVER_FILTER;
        }
        intent.setAction(mainProcessName);
        intent.putExtra(TDConstants.KEY_APP_ID, mConfig.getName());
        return intent;
    }

    @Override
    public void setSuperProperties(JSONObject superProperties) {
        JSONObject properties = new JSONObject();
        try {
            TDUtils.mergeJSONObject(superProperties, properties, mConfig.getDefaultTimeZone());
            Intent intent = getIntent();
            intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_SET_SUPER_PROPERTIES);
            if (superProperties != null) {
                intent.putExtra(TDConstants.KEY_PROPERTIES, properties.toString());
            }
            if (null != mContext) {
                mContext.sendBroadcast(intent);
            }
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void unsetSuperProperty(String superPropertyName) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_UNSET_SUPER_PROPERTIES);
        if (superPropertyName != null) {
            intent.putExtra(TDConstants.KEY_PROPERTIES, superPropertyName);
        }
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void clearSuperProperties() {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_CLEAR_SUPER_PROPERTIES);
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }
    }

    double getEventDuration(String eventName, long systemUpdateTime) {
        final EventTimer eventTimer;
        double duration = 0d;
        synchronized (mTrackTimer) {
            eventTimer = mTrackTimer.get(eventName);
            mTrackTimer.remove(eventName);
        }
        if (null != eventTimer) {
            duration = Double.parseDouble(eventTimer.duration(systemUpdateTime));
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

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList, AutoTrackEventListener autoTrackEventListener) {

    }

    @Override
    public void setTrackStatus(TATrackStatus status) {

    }
}
