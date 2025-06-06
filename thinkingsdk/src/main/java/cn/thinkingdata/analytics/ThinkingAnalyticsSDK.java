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
import cn.thinkingdata.analytics.utils.CommonUtil;
import cn.thinkingdata.analytics.aop.push.TAPushUtils;
import cn.thinkingdata.analytics.encrypt.ThinkingDataEncrypt;
import cn.thinkingdata.analytics.utils.broadcast.TDReceiver;
import cn.thinkingdata.analytics.utils.plugin.TDPluginUtils;
import cn.thinkingdata.core.preset.TDPresetUtils;
import cn.thinkingdata.core.receiver.TDAnalyticsObservable;
import cn.thinkingdata.core.router.TRouter;
import cn.thinkingdata.analytics.utils.CalibratedTimeManager;
import cn.thinkingdata.analytics.utils.ITime;
import cn.thinkingdata.analytics.utils.PropertyUtils;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.core.router.TRouterMap;
import cn.thinkingdata.core.router.provider.callback.ISensitivePropertiesCallBack;
import cn.thinkingdata.core.utils.TDLog;
import cn.thinkingdata.analytics.utils.TDUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

    static final String TAG = "ThinkingAnalyticsSDK";
    private static final Map<Context, Map<String, ThinkingAnalyticsSDK>> sInstanceMap = new HashMap<>();
    private static boolean isFirstInstall = false;
    private boolean isAppIdFirstInstall = false;
    public TDConfig mConfig;
    protected JSONObject mAutoTrackEventProperties;
    public CalibratedTimeManager mCalibratedTimeManager;
    protected UserOperationHandler mUserOperationHandler;
    // Whether to synchronize old version data, v1.3.0+ is compatible with previous versions of the internal use variable
    protected boolean mEnableTrackOldData;
    //Local cache (SharePreference related variable), unique to a single instance. Its file name is PREFERENCE_NAME_{{name}}
    private CommonStorageManager mStorageManager;
    protected DataHandle mMessages;
    protected SystemInformation mSystemInformation;
    protected String mCurrentAccountId;
    protected final Object lockAccountObj = new Object();
    protected String mCurrentDistinctId;
    protected final Object lockDistinctId = new Object();
    protected TATrackStatus mCurrentTrackStatus;
    protected final Object lockTrackStatus = new Object();
    private List<Integer> mAutoTrackIgnoredActivities;
    protected Map<String, EventTimer> mTrackTimer;
    private String mLastScreenUrl;
    private List<Class> mIgnoredViewTypeList;
    private ThinkingDataActivityLifecycleCallbacks mLifecycleCallbacks;
    public boolean mTrackFragmentAppViewScreen;
    private List<AutoTrackEventType> mAutoTrackEventTypeList;
    //Dynamic public attribute interface
    protected DynamicSuperPropertiesTracker mDynamicSuperPropertiesTracker;
    public AutoTrackDynamicProperties mAutoTrackDynamicProperties;
    //Automatic event collection callback interface
    private AutoTrackEventListener mAutoTrackEventListener;
    //Automatic collection of relevant variables
    private boolean mAutoTrack;
    public boolean mTrackCrash;
    protected boolean mIgnoreAppViewInExtPackage = false;
    private ThinkingSDKErrorCallback mSDKCallback;

    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId) {
        return sharedInstance(context, appId, null, false);
    }

    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url) {
        return sharedInstance(context, appId, url, true);
    }

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
        synchronized (sInstanceMap) {
            Map<String, ThinkingAnalyticsSDK> instances = sInstanceMap.get(config.mContext);
            if (null == instances) {
                instances = new HashMap<>();
                sInstanceMap.put(config.mContext, instances);
            }
            ThinkingAnalyticsSDK instance = instances.get(config.getName());
            if (null == instance) {
                try {
                    if (!CommonUtil.isMainProcess(config.mContext)) {
                        instance = new SubprocessThinkingAnalyticsSDK(config);
                    } else {
                        instance = new ThinkingAnalyticsSDK(config);
                    }
                    instances.put(config.getName(), instance);
                } catch (Exception e) {
                    return null;
                }
            }
            return instance;
        }
    }

    public static void calibrateTime(long timestamp) {
        if (timestamp <= 0) return;
        TDLog.i(TAG, "[ThinkingData] Info: Time Calibration with timestamp(" + timestamp + ")");
        CalibratedTimeManager.calibrateTime(timestamp);
    }

    public static void calibrateTimeWithNtp(String... ntpServer) {
        CalibratedTimeManager.calibrateTimeWithNtp(ntpServer);
    }

    public ThinkingAnalyticsSDK() {

    }

    public ThinkingAnalyticsSDK(TDConfig config) {
        mConfig = config;
        if (TextUtils.isEmpty(mConfig.getServerUrl()) || TextUtils.isEmpty(mConfig.mToken)) {
            throw new IllegalArgumentException("invalid appId or serverUrl");
        }
        if (!TDPresetProperties.disableList.contains(TDConstants.KEY_FPS)) {
            if (null == Looper.myLooper()) {
                Looper.prepare();
            }
            CommonUtil.listenFPS();
        }
        mCalibratedTimeManager = new CalibratedTimeManager(config);
        mUserOperationHandler = new UserOperationHandler(this, config);
        mStorageManager = new CommonStorageManager(mConfig.mContext, mConfig.getName());
        mTrackTimer = new HashMap<>();
        mAutoTrackIgnoredActivities = new ArrayList<>();
        mAutoTrackEventTypeList = new ArrayList<>();
        mLifecycleCallbacks = new ThinkingDataActivityLifecycleCallbacks(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = ( Application ) config.mContext.getApplicationContext();
            app.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
        }
        if (config.isEnableMutiprocess() && CommonUtil.isMainProcess(config.mContext)) {
            TDReceiver.registerReceiver(config.mContext);
        }
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mAutoTrackEventProperties = new JSONObject();
                mSystemInformation = SystemInformation.getInstance(mConfig.mContext, mConfig.getDefaultTimeZone());
                mEnableTrackOldData = mConfig.trackOldData();
                mMessages = DataHandle.getInstance(mConfig.mContext);
                mMessages.handleTrackPauseToken(getToken(), mStorageManager.getPausePostFlag());
                if (mEnableTrackOldData) {
                    mMessages.flushOldData(mConfig.getName());
                }
                //init accountId distinctId
                getLoginId();
                getDistinctId();
                synchronized (lockTrackStatus) {
                    if (mStorageManager.getPausePostFlag()) {
                        mCurrentTrackStatus = TATrackStatus.SAVE_ONLY;
                    } else if (!mStorageManager.getEnableFlag()) {
                        mCurrentTrackStatus = TATrackStatus.PAUSE;
                    } else if (mStorageManager.getOptOutFlag()) {
                        mCurrentTrackStatus = TATrackStatus.STOP;
                    } else {
                        mCurrentTrackStatus = TATrackStatus.NORMAL;
                    }
                }
                if (mConfig.mEnableEncrypt) {
                    ThinkingDataEncrypt.createInstance(mConfig.getName(), mConfig);
                }
                TRouter.init();
                if (!mConfig.isNormal() || TDUtils.isLogControlFileExist()) {
                    TDLog.setEnableLog(true);
                }
                TDPluginUtils.clearPluginEvent(ThinkingAnalyticsSDK.this);
                TDLog.i(TAG, String.format("[ThinkingData] Info: ThinkingData SDK %s initialize success with mode: %s, APP ID ends with: %s, server url: %s", TDConfig.VERSION,
                        mConfig.getMode().name(), TDUtils.getSuffix(mConfig.mToken, 4), mConfig.getServerUrl()));
                TRouter.getInstance().build(TRouterMap.PRESET_TEMPLATE_ROUTE_PATH).withAction("triggerSdkInit")
                        .withString("appId", mConfig.getName()).navigation();
                TDAnalyticsObservable.getInstance().onSdkInitCalled(mConfig.mToken);
            }
        });
    }

    @Override
    public void login(final String loginId) {
        if (isTrackDisabled()) return;
        if (CommonUtil.isEmpty(loginId)) {
            TDLog.w(TAG, "The account id cannot be empty.");
            return;
        }
        synchronized (lockAccountObj) {
            mCurrentAccountId = loginId;
        }
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.saveLoginId(loginId);
                TDLog.i(TAG, "[ThinkingData] Info: Login SDK, AccountId = " + loginId);
                TDAnalyticsObservable.getInstance().onLoginMethodCalled(loginId, getDistinctId(), mConfig.mToken);
                TAPushUtils.handlePushTokenAfterLogin(ThinkingAnalyticsSDK.this);
            }
        });
    }

    @Override
    public void logout() {
        if (isTrackDisabled()) return;
        synchronized (lockAccountObj) {
            mCurrentAccountId = null;
        }
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.logout(mEnableTrackOldData, mConfig.mContext);
                TDLog.i(TAG, "[ThinkingData] Info: Logout SDK");
                TDAnalyticsObservable.getInstance().onLogoutMethodCalled(getDistinctId(), mConfig.mToken);
            }
        });
    }

    public String getLoginId() {
        synchronized (lockAccountObj) {
            if (TextUtils.isEmpty(mCurrentAccountId)) {
                mCurrentAccountId = mStorageManager.getLoginId(mEnableTrackOldData, mConfig.mContext);
            }
            return mCurrentAccountId;
        }
    }

    @Override
    public void identify(final String identity) {
        if (isTrackDisabled()) return;
        if (CommonUtil.isEmpty(identity)) {
            TDLog.w(TAG, "The identity cannot be empty.");
            return;
        }
        synchronized (lockDistinctId) {
            mCurrentDistinctId = identity;
        }
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.setIdentifyId(identity);
                TDLog.i(TAG, "[ThinkingData] Info: Setting distinct ID, DistinctId = " + identity);
                TDAnalyticsObservable.getInstance().onSetDistinctIdMethodCalled(getLoginId(), identity, mConfig.mToken);
                TAPushUtils.handlePushTokenAfterLogin(ThinkingAnalyticsSDK.this);
            }
        });
    }

    @Override
    public String getDistinctId() {
        synchronized (lockDistinctId) {
            if (TextUtils.isEmpty(mCurrentDistinctId)) {
                mCurrentDistinctId = mStorageManager.getIdentifyId();
                if (TextUtils.isEmpty(mCurrentDistinctId)) {
                    mCurrentDistinctId = getRandomID();
                }
            }
            return mCurrentDistinctId;
        }
    }

    protected String getRandomID() {
        return GlobalStorageManager.getInstance(mConfig.mContext).getRandomID();
    }

    @Override
    public String getDeviceId() {
        return SystemInformation.getInstance(mConfig.mContext, mConfig.getDefaultTimeZone()).getDeviceId();
    }

    public boolean isTrackDisabled() {
        synchronized (lockTrackStatus) {
            return mCurrentTrackStatus == TATrackStatus.STOP || mCurrentTrackStatus == TATrackStatus.PAUSE;
        }
    }

    public boolean isStatusTrackSaveOnly() {
        synchronized (lockTrackStatus) {
            return mCurrentTrackStatus == TATrackStatus.SAVE_ONLY;
        }
    }


    public enum ThinkingdataNetworkType {
        NETWORKTYPE_DEFAULT,
        NETWORKTYPE_WIFI,
        NETWORKTYPE_ALL
    }

    @Override
    public void setNetworkType(ThinkingdataNetworkType type) {
        if (isTrackDisabled()) return;
        mConfig.setNetworkType(type);
    }

    // autoTrack is used internal without property checking.
    public void autoTrack(String eventName, JSONObject properties) {
        track(eventName, properties, mCalibratedTimeManager.getTime(), false);
    }

    public void autoTrack(String eventName, JSONObject properties, ITime time) {
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
        if (isTrackDisabled()) return;
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
        if (isTrackDisabled()) return;
        final ThinkingAnalyticsSDK self = this;
        final long systemUpdateTime = SystemClock.elapsedRealtime();
        final String accountId = getLoginId();
        final String distinctId = getDistinctId();
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
        final JSONObject cloneJson = CommonUtil.cloneJsonObject(properties);
        final JSONObject finalAutoTrackProperties = autoTrackPro;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                if (mConfig.isDisabledEvent(eventName)) {
                    TDLog.d(TAG, "Ignoring disabled event [" + eventName + "]");
                    return;
                }
                try {
                    final boolean isFromSubProcess = cloneJson != null && cloneJson.has(TDPresetUtils.KEY_BUNDLE_ID) && cloneJson.has(KEY_SUBPROCESS_TAG);
                    if (doFormatChecking && PropertyUtils.isInvalidName(eventName)) {
                        TDLog.e(TAG, "[ThinkingData] Error: Incorrect Event name[" + eventName + "]. Event name must be string that starts with English letter, "
                                + "and contains letter, number, and '_'. The max length of the event name is 50.");
                    }
                    if (doFormatChecking && !PropertyUtils.checkProperty(cloneJson)) {
                        TDLog.w(TAG, "[ThinkingData] Warning: The data contains invalid key or value: " + cloneJson.toString());
                    }
                    JSONObject finalProperties = obtainDefaultEventProperties(eventName, systemUpdateTime, isFromSubProcess);
                    if (null != finalAutoTrackProperties) {
                        TDUtils.mergeJSONObject(finalAutoTrackProperties, finalProperties, mConfig.getDefaultTimeZone());
                    }
                    if (null != cloneJson) {
                        TDUtils.mergeJSONObject(cloneJson, finalProperties, mConfig.getDefaultTimeZone());
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
                finalProperties.put(TDConstants.KEY_FPS, CommonUtil.getFPS());
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
                JSONObject autoTrackProperties = this.mAutoTrackEventProperties.optJSONObject(eventName);
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
                final EventTimer eventTimer = mTrackTimer.get(eventName);
                mTrackTimer.remove(eventName);

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

    public void user_add(JSONObject properties, Date date) {
        mUserOperationHandler.user_add(properties, date);
    }

    @Override
    public void user_append(JSONObject properties) {
        mUserOperationHandler.user_append(properties, null);
    }

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

    public void user_delete(Date date) {
        mUserOperationHandler.user_delete(date);
    }

    @Override
    public void user_unset(String... properties) {
        mUserOperationHandler.user_unset(properties);
    }

    public void user_unset(JSONObject properties, Date date) {
        mUserOperationHandler.user_unset(properties, date);
    }

    @Override
    public void flush() {
        if (isTrackDisabled() || isStatusTrackSaveOnly()) {
            return;
        }
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mMessages.flush(getToken());
            }
        });
    }

    @Override
    public JSONObject getSuperProperties() {
        return mStorageManager.getSuperProperties();
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
                presetProperties.put(TDConstants.KEY_FPS, CommonUtil.getFPS());
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

    @Override
    public void setSuperProperties(final JSONObject superProperties) {
        if (isTrackDisabled()) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.setSuperProperties(superProperties, mConfig.getDefaultTimeZone());
            }
        });
    }

    @Override
    public void unsetSuperProperty(final String superPropertyName) {
        if (isTrackDisabled()) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.unsetSuperProperty(superPropertyName);
            }
        });
    }

    @Override
    public void clearSuperProperties() {
        if (isTrackDisabled()) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mStorageManager.clearSuperProperties();
            }
        });
    }

    @SuppressLint("AddJavascriptInterface")
    @Override
    public void setJsBridge(WebView webView) {
        if (null == webView) {
            TDLog.d(TAG, "SetJsBridge failed due to parameter webView is null");
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

    public interface DynamicSuperPropertiesTracker {
        JSONObject getDynamicSuperProperties();
    }

    public interface AutoTrackDynamicProperties {
        JSONObject getAutoTrackDynamicProperties();
    }

    public interface ThinkingSDKErrorCallback {
        void onSDKErrorCallback(int code, String errorMsg, String ext);
    }


    public void registerErrorCallback(ThinkingSDKErrorCallback callback){
        mSDKCallback = callback;
    }

    public ThinkingSDKErrorCallback getSDKErrorCallback(){
        return mSDKCallback;
    }

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
        if (isTrackDisabled()) return;
        mDynamicSuperPropertiesTracker = dynamicSuperPropertiesTracker;
    }

    public void setAutoTrackDynamicProperties(AutoTrackDynamicProperties autoTrackDynamicProperties) {
        if (isTrackDisabled()) return;
        mAutoTrackDynamicProperties = autoTrackDynamicProperties;
    }

    @Override
    public void timeEvent(final String eventName) {
        if (isTrackDisabled()) return;
        final long systemUpdateTime = SystemClock.elapsedRealtime();
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (PropertyUtils.isInvalidName(eventName)) {
                        TDLog.w(TAG, "timeEvent event name[" + eventName + "] is not valid");
                    }
                    mTrackTimer.put(eventName, new EventTimer(TimeUnit.SECONDS, systemUpdateTime));
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

    boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
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

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        if (isTrackDisabled()) return;

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
        if (isTrackDisabled()) return;

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

    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        return eventType != null && !mAutoTrackEventTypeList.contains(eventType);
    }

    public boolean isAutoTrackEnabled() {
        if (isTrackDisabled()) {
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

    public void trackViewScreenInternal(String url, JSONObject properties) {
        if (isTrackDisabled()) {
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
        if (isTrackDisabled()) {
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
        if (isTrackDisabled()) {
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
        if (isTrackDisabled()) {
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

    public void appEnterBackground() {
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    public void appBecomeActive() {
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
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
                }
            }
        });
    }

    @Override
    public void trackAppInstall() {
        if (isTrackDisabled()) return;
        enableAutoTrack(new ArrayList<>(Collections.singletonList(AutoTrackEventType.APP_INSTALL)));
    }

    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList, JSONObject properties) {
        setAutoTrackProperties(eventTypeList, properties);
        enableAutoTrack(eventTypeList);
    }

    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList, AutoTrackEventListener autoTrackEventListener) {
        mAutoTrackEventListener = autoTrackEventListener;
        enableAutoTrack(eventTypeList);
    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        if (isTrackDisabled()) return;
        mAutoTrack = true;
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
        }
        if (eventTypeList.contains(AutoTrackEventType.APP_INSTALL)) {
            final ITime time = mCalibratedTimeManager.getTime();
            TrackTaskManager.getInstance().addTask(new Runnable() {
                @Override
                public void run() {
                    Pair<Long, Boolean> installInfo = TDUtils.getInstallInfo(mConfig.mContext);
                    if (!isFirstInstall) {
                        long installTime = installInfo.first;
                        long lastInstallTime = GlobalStorageManager.getInstance(mConfig.mContext).getLastInstallTime();
                        isFirstInstall = lastInstallTime == 0L;
                        if (isFirstInstall) {
                            GlobalStorageManager.getInstance(mConfig.mContext).saveLastInstallTime(installTime);
                        }
                    }
                    if (isFirstInstall && installInfo.second && !isAppIdFirstInstall) {
                        isAppIdFirstInstall = true;
                        track(TDConstants.APP_INSTALL_EVENT_NAME, null, time);
                        flush();
                        TRouter.getInstance().build(TRouterMap.PRESET_TEMPLATE_ROUTE_PATH).withAction("triggerAppInstallEvent")
                                .withString("appId", getToken()).navigation();
                    }
                }
            });
        }
        if (eventTypeList.contains(AutoTrackEventType.APP_CRASH)) {
            TrackTaskManager.getInstance().addTask(new Runnable() {
                @Override
                public void run() {
                    mTrackCrash = true;
                    TAExceptionHandler handler = TAExceptionHandler.getInstance(mConfig.mContext);
                    if (null != handler) {
                        handler.initExceptionHandler();
                    }
                }
            });
        }
        if (!mAutoTrackEventTypeList.contains(AutoTrackEventType.APP_END)
                && eventTypeList.contains(AutoTrackEventType.APP_END)) {
            timeEvent(TDConstants.APP_END_EVENT_NAME);
            mLifecycleCallbacks.updateShouldTrackEvent(true);
        }
        mAutoTrackEventTypeList.clear();
        mAutoTrackEventTypeList.addAll(eventTypeList);
        if (mAutoTrackEventTypeList.contains(AutoTrackEventType.APP_START)) {
            mLifecycleCallbacks.onAppStartEventEnabled();
        }
    }

    @Override
    public void setAutoTrackProperties(final List<AutoTrackEventType> eventTypeList, final JSONObject autoTrackEventProperties) {
        if (isTrackDisabled()) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (autoTrackEventProperties == null || !PropertyUtils.checkProperty(autoTrackEventProperties)) {
                        return;
                    }
                    JSONObject allAutoTrackEventProperties = new JSONObject();
                    for (AutoTrackEventType eventType : eventTypeList) {
                        JSONObject newJSONObject = new JSONObject();
                        TDUtils.mergeJSONObject(autoTrackEventProperties, newJSONObject, mConfig.getDefaultTimeZone());
                        allAutoTrackEventProperties.put(eventType.getEventName(), newJSONObject);
                    }
                    TDUtils.mergeNestedJSONObject(allAutoTrackEventProperties, mAutoTrackEventProperties, mConfig.getDefaultTimeZone());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void trackAppCrashAndEndEvent(JSONObject properties) {
        mLifecycleCallbacks.trackAppCrashAndEndEvent(properties);
    }

    List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }
        return mIgnoredViewTypeList;
    }

    @Override
    public void ignoreViewType(Class viewType) {
        if (isTrackDisabled()) return;
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

    @Override
    public void trackFragmentAppViewScreen() {
        if (isTrackDisabled()) return;
        this.mTrackFragmentAppViewScreen = true;
    }

    @Override
    public void setViewID(View view, String viewID) {
        if (isTrackDisabled()) return;

        if (view != null && !TextUtils.isEmpty(viewID)) {
            TDUtils.setTag(getToken(), view, R.id.thinking_analytics_tag_view_id, viewID);
        }
    }

    @Override
    public void setViewID(Dialog view, String viewID) {
        if (isTrackDisabled()) return;
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
        if (isTrackDisabled()) return;
        if (view == null || properties == null) {
            return;
        }
        TDUtils.setTag(getToken(), view, R.id.thinking_analytics_tag_view_properties, properties);
    }

    @Override
    public void ignoreView(View view) {
        if (isTrackDisabled()) return;
        if (view != null) {
            TDUtils.setTag(getToken(), view, R.id.thinking_analytics_tag_view_ignored, "1");
        }
    }

    public interface InstanceProcessor {
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
        synchronized (lockTrackStatus) {
            mCurrentTrackStatus = status;
        }
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {

                switch (status) {
                    case PAUSE:
                        mStorageManager.saveOptOutFlag(false);
                        mStorageManager.savePausePostFlag(false);
                        mMessages.handleTrackPauseToken(getToken(), false);
                        flush();
                        mStorageManager.saveEnableFlag(false);
                        TDLog.i(TAG, "[ThinkingData] Info: Change Status to Pause");
                        break;
                    case STOP:
                        mStorageManager.saveEnableFlag(true);
                        mStorageManager.savePausePostFlag(false);
                        mMessages.handleTrackPauseToken(getToken(), false);
                        mStorageManager.saveOptOutFlag(true);
                        mMessages.emptyMessageQueue(getToken());
                        mTrackTimer.clear();

                        synchronized (lockAccountObj) {
                            mCurrentAccountId = null;
                        }
                        synchronized (lockDistinctId) {
                            mCurrentDistinctId = getRandomID();
                        }
                        mStorageManager.clearIdentify();
                        mStorageManager.clearLoginId();
                        mStorageManager.clearSuperProperties();
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

    @Override
    @Deprecated
    public void enableTracking(final boolean enabled) {
    }

    @Override
    @Deprecated
    public void optOutTrackingAndDeleteUser() {
    }

    @Override
    @Deprecated
    public void optOutTracking() {
    }

    @Override
    @Deprecated
    public void optInTracking() {
    }

    @Override
    public ThinkingAnalyticsSDK createLightInstance() {
        return new LightThinkingAnalyticsSDK(mConfig);
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

    @Override
    public void enableThirdPartySharing(int types) {
        TRouter.getInstance().build("/thingkingdata/third/party")
                .withAction("enableThirdPartySharing")
                .withInt("type", types)
                .withObject("instance", this)
                .withString("loginId", getLoginId())
                .navigation();
    }

    public void enableThirdPartySharing(int type, Object obj) {
        TRouter.getInstance().build("/thingkingdata/third/party")
                .withAction("enableThirdPartySharingWithParams")
                .withInt("type", type)
                .withObject("instance", this)
                .withString("loginId", getLoginId())
                .withObject("params", obj)
                .navigation();
    }

}

/**
 * Lightweight instance, does not support local cache, shares APP ID with main instance.
 */
class LightThinkingAnalyticsSDK extends ThinkingAnalyticsSDK {

    private final JSONObject mSuperProperties;

    public LightThinkingAnalyticsSDK(TDConfig config) {
        mConfig = config;
        if (TextUtils.isEmpty(mConfig.getServerUrl()) || TextUtils.isEmpty(mConfig.mToken)) {
            throw new IllegalArgumentException("invalid appId or serverUrl");
        }
        mAutoTrackEventProperties = new JSONObject();
        mCalibratedTimeManager = new CalibratedTimeManager(config);
        mUserOperationHandler = new UserOperationHandler(this, config);
        mTrackTimer = new HashMap<>();
        mSuperProperties = new JSONObject();
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                mSystemInformation = SystemInformation.getInstance(mConfig.mContext, mConfig.getDefaultTimeZone());
                mMessages = DataHandle.getInstance(mConfig.mContext);
            }
        });
    }

    @Override
    public void login(final String accountId) {
        if (isTrackDisabled()) return;
        synchronized (lockAccountObj) {
            mCurrentAccountId = accountId;
        }
    }

    @Override
    public void logout() {
        if (isTrackDisabled()) return;
        synchronized (lockAccountObj) {
            mCurrentAccountId = null;
        }
    }

    @Override
    public String getLoginId() {
        synchronized (lockAccountObj) {
            return mCurrentAccountId;
        }
    }

    @Override
    public void identify(String identity) {
        if (CommonUtil.isEmpty(identity)) {
            TDLog.w(TAG, "The identity cannot be empty.");
            return;
        }
        synchronized (lockDistinctId) {
            mCurrentDistinctId = identity;
        }
    }

    @Override
    public String getDistinctId() {
        synchronized (lockDistinctId) {
            if (mCurrentDistinctId == null) {
                mCurrentDistinctId = getRandomID();
            }
            return mCurrentDistinctId;
        }
    }

    @Override
    public void setSuperProperties(final JSONObject superProperties) {
        if (isTrackDisabled()) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    @Override
    public void unsetSuperProperty(final String superPropertyName) {
        if (isTrackDisabled()) return;
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
        if (isTrackDisabled()) return;
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                synchronized (mSuperProperties) {
                    Iterator<String> keys = mSuperProperties.keys();
                    while (keys.hasNext()) {
                        keys.next();
                        keys.remove();
                    }
                }
            }
        });
    }


    @Override
    public JSONObject getSuperProperties() {
        synchronized (mSuperProperties) {
            return mSuperProperties;
        }
    }

    @Override
    public void setTrackStatus(final TATrackStatus status) {
        synchronized (lockTrackStatus) {
            mCurrentTrackStatus = status;
        }
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case PAUSE:
                    case STOP:
                        mMessages.handleTrackPauseToken(getToken(), false);
                        break;
                    case SAVE_ONLY:
                        mMessages.handleTrackPauseToken(getToken(), true);
                        break;
                    case NORMAL:
                        mMessages.handleTrackPauseToken(getToken(), false);
                        flush();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void setNetworkType(ThinkingdataNetworkType type) {

    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {

    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList, JSONObject properties) {

    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList, AutoTrackEventListener autoTrackEventListener) {

    }

    @Override
    public void setAutoTrackProperties(List<AutoTrackEventType> eventTypeList, JSONObject autoTrackEventProperties) {

    }

    @Override
    public void trackFragmentAppViewScreen() {

    }

    @Override
    public void trackAppCrashAndEndEvent(JSONObject properties) {

    }

    @Override
    public void trackAppInstall() {

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
    List<Class> getIgnoredViewTypeList() {
        return null;
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
    public void enableThirdPartySharing(int types) {

    }

    @Override
    public void enableThirdPartySharing(int type, Object obj) {

    }

    @Override
    public void appBecomeActive() {

    }

    @Override
    public void appEnterBackground() {

    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        return false;
    }

    @Override
    public boolean isAutoTrackEnabled() {
        return false;
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
    public void login(String accountId) {
        if (CommonUtil.isEmpty(accountId)) {
            TDLog.w(TAG, "The account id cannot be empty.");
            return;
        }
        synchronized (lockAccountObj) {
            mCurrentAccountId = accountId;
        }
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
    public void logout() {
        synchronized (lockAccountObj) {
            mCurrentAccountId = null;
        }
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_LOGOUT);
        if (null != mContext) {
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void identify(String distinctId) {
        if (CommonUtil.isEmpty(distinctId)) {
            TDLog.w(TAG, "The identity cannot be empty.");
            return;
        }
        synchronized (lockDistinctId) {
            mCurrentDistinctId = distinctId;
        }
        Intent intent = getIntent();
        intent.putExtra(TDConstants.TD_ACTION, TDConstants.TD_ACTION_IDENTIFY);
        if (distinctId.length() > 0) {
            intent.putExtra(TDConstants.KEY_DISTINCT_ID, distinctId);
        } else {
            intent.putExtra(TDConstants.KEY_DISTINCT_ID, "");
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

    @Override
    public void setAutoTrackProperties(final List<AutoTrackEventType> eventTypeList, final JSONObject autoTrackEventProperties) {
        if (isTrackDisabled()) {
            return;
        }
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (autoTrackEventProperties == null || !PropertyUtils.checkProperty(autoTrackEventProperties)) {
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
        });

    }

    @Override
    public void autoTrack(String eventName, JSONObject properties) {
        Intent intent = getIntent();
        intent.putExtra(TDConstants.KEY_EVENT_NAME, eventName);
        properties = properties == null ? new JSONObject() : properties;
        JSONObject realProperties = obtainProperties(eventName, properties);
        try {
            JSONObject autoTrackProperties = this.mAutoTrackEventProperties.optJSONObject(eventName);
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
        if (mDynamicSuperPropertiesTracker != null) {
            JSONObject dynamicProperties = mDynamicSuperPropertiesTracker.getDynamicSuperProperties();
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
        String mainProcessName = CommonUtil.getMainProcessName(mContext);
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
        eventTimer = mTrackTimer.get(eventName);
        mTrackTimer.remove(eventName);

        if (null != eventTimer) {
            duration = Double.parseDouble(eventTimer.duration(systemUpdateTime));
        }
        return duration;
    }

    @Override
    public void setNetworkType(ThinkingdataNetworkType type) {

    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList, AutoTrackEventListener autoTrackEventListener) {

    }

    @Override
    public void setTrackStatus(TATrackStatus status) {

    }
}
