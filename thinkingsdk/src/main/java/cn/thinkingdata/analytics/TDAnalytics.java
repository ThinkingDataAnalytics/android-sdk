/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import cn.thinkingdata.analytics.data.SystemInformation;
import cn.thinkingdata.core.receiver.TDAnalyticsObservable;
import cn.thinkingdata.core.router.TRouter;
import cn.thinkingdata.core.router.TRouterMap;
import cn.thinkingdata.core.sp.TDStorageEncryptPlugin;
import cn.thinkingdata.core.utils.TDLog;

/**
 * The packaging class of ThinkingAnalyticsSDK provides static methods, which is more convenient for customers to use
 *
 * @author liulongbing
 * @since 2023/6/20
 */
public class TDAnalytics {

    public static Map<String, ThinkingAnalyticsSDK> sInstances = new HashMap<>();

    static ThinkingAnalyticsSDK instance = null;

    /**
     * Get the local area/country code
     *
     * @return local area/country code
     */
    public static String getLocalRegion() {
        return Locale.getDefault().getCountry();
    }

    /**
     * time calibration with timestamp
     *
     * @param timestamp timestamp
     */
    public static void calibrateTime(long timestamp) {
        ThinkingAnalyticsSDK.calibrateTime(timestamp);
    }

    /**
     * time calibration with ntp
     *
     * @param ntpServer ntp server url
     */
    public static void calibrateTimeWithNtp(String... ntpServer) {
        ThinkingAnalyticsSDK.calibrateTimeWithNtp(ntpServer);
    }

    /**
     * enable debug logging
     *
     * @param enableLog log switch
     */
    public static void enableLog(boolean enableLog) {
        TDLog.setEnableLog(enableLog);
    }

    /**
     * Set libName and libVersion of SDK
     *
     * @param libName    sdk name
     * @param libVersion sdk version
     */
    public static void setCustomerLibInfo(String libName, String libVersion) {
        SystemInformation.setLibraryInfo(libName, libVersion);
    }

    public static void encryptLocalStorage() {
        TDStorageEncryptPlugin.getInstance().enableEncrypt();
    }

    /**
     * Initialize the SDK. The track function is not available until this interface is invoked.
     *
     * @param context   context
     * @param appId     app id
     * @param serverUrl server url
     */
    public static void init(Context context, String appId, String serverUrl) {
        TDConfig config = TDConfig.getInstance(context, appId, serverUrl);
        init(config);
    }

    /**
     * Initialize the SDK with config. The track function is not available until this interface is invoked.
     *
     * @param config init config
     */
    public synchronized static void init(TDConfig config) {
        ThinkingAnalyticsSDK sdk = ThinkingAnalyticsSDK.sharedInstance(config);
        if (sdk == null) return;
        if (null == instance) {
            instance = sdk;
        }
        sInstances.put(config.getName(), sdk);
    }

    /**
     * Create lightweight SDK instances. Lightweight SDK instances do not support
     * caching of local account ids, guest ids, public properties, etc.
     *
     * @return light instance
     */
    public static String lightInstance() {
        if (null == instance) return "";
        ThinkingAnalyticsSDK lightInstance = instance.createLightInstance();
        String uuid = UUID.randomUUID().toString();
        sInstances.put(uuid, lightInstance);
        return uuid;
    }

    /**
     * Set the network conditions for uploading. By default, the SDK will upload data on 3G, 4G and Wifi.
     *
     * @param networkType Type of the network on which data is uploaded
     */
    public static void setNetworkType(TDNetworkType networkType) {
        if (null == instance) return;
        switch (networkType) {
            case WIFI:
                instance.setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType.NETWORKTYPE_WIFI);
                break;
            case ALL:
                instance.setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType.NETWORKTYPE_ALL);
                break;
        }
    }

    public static void registerErrorCallback(final TDSendDataErrorCallback callback) {
        if (null == instance) return;
        instance.registerErrorCallback(new ThinkingAnalyticsSDK.ThinkingSDKErrorCallback() {
            @Override
            public void onSDKErrorCallback(int code, String errorMsg, String ext) {
                callback.onSDKErrorCallback(code, errorMsg, ext);
            }
        });
    }

    /**
     * Upload a single event, containing only preset properties and set public properties.
     *
     * @param eventName event name
     */
    public static void track(String eventName) {
        if (null == instance) return;
        instance.track(eventName);
    }


    /**
     * Upload events and their associated attributes.
     *
     * @param eventName  event name
     * @param properties event properties
     */
    public static void track(String eventName, JSONObject properties) {
        if (null == instance) return;
        instance.track(eventName, properties);
    }

    /**
     * Upload the event and set the event trigger time. SDK v2.2.0 support is available.
     *
     * @param eventName  event name
     * @param properties event properties
     * @param time       event time
     * @param timeZone   event timeZone
     */
    public static void track(String eventName, JSONObject properties, Date time, final TimeZone timeZone) {
        if (null == instance) return;
        instance.track(eventName, properties, time, timeZone);
    }

    /**
     * Upload a special type of event.
     *
     * @param eventModel Event Object TDFirstEventModel / TDOverWritableEventModel / TDUpdatableEventModel
     */
    public static void track(TDEventModel eventModel) {
        if (null == instance) return;
        instance.track(eventModel);
    }

    /**
     * Enable the auto tracking function.
     *
     * @param autoTrackEventType Indicates the type of the automatic collection event to be enabled
     */
    public static void enableAutoTrack(int autoTrackEventType) {
        if (null == instance) return;
        List<ThinkingAnalyticsSDK.AutoTrackEventType> types = new ArrayList<>();
        if ((autoTrackEventType & TDAutoTrackEventType.APP_START) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_END) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_CLICK) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_VIEW_SCREEN) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_CRASH) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_INSTALL) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        }
        instance.enableAutoTrack(types);
    }

    /**
     * Enable the auto tracking function with properties
     *
     * @param autoTrackEventType Indicates the type of the automatic collection event to be enabled
     * @param properties         properties
     */
    public static void enableAutoTrack(int autoTrackEventType, JSONObject properties) {
        if (null == instance) return;
        List<ThinkingAnalyticsSDK.AutoTrackEventType> types = new ArrayList<>();
        if ((autoTrackEventType & TDAutoTrackEventType.APP_START) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_END) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_CLICK) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_VIEW_SCREEN) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_CRASH) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_INSTALL) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        }
        instance.enableAutoTrack(types, properties);
    }

    /**
     * Enable the auto tracking function with properties
     *
     * @param autoTrackEventType Indicates the type of the automatic collection event to be enabled
     * @param eventHandler       callback
     */
    public static void enableAutoTrack(int autoTrackEventType, final TDAutoTrackEventHandler eventHandler) {
        if (null == instance || null == eventHandler) return;
        List<ThinkingAnalyticsSDK.AutoTrackEventType> types = new ArrayList<>();
        if ((autoTrackEventType & TDAutoTrackEventType.APP_START) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_END) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_CLICK) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_VIEW_SCREEN) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_CRASH) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        }
        if ((autoTrackEventType & TDAutoTrackEventType.APP_INSTALL) > 0) {
            types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        }
        ThinkingAnalyticsSDK.AutoTrackEventListener listener = new ThinkingAnalyticsSDK.AutoTrackEventListener() {
            @Override
            public JSONObject eventCallback(ThinkingAnalyticsSDK.AutoTrackEventType eventType, JSONObject properties) {
                int type = 0;
                if (eventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_START) {
                    type = TDAutoTrackEventType.APP_START;
                } else if (eventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_END) {
                    type = TDAutoTrackEventType.APP_END;
                } else if (eventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK) {
                    type = TDAutoTrackEventType.APP_CLICK;
                } else if (eventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN) {
                    type = TDAutoTrackEventType.APP_VIEW_SCREEN;
                } else if (eventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH) {
                    type = TDAutoTrackEventType.APP_CRASH;
                } else if (eventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL) {
                    type = TDAutoTrackEventType.APP_INSTALL;
                }
                return eventHandler.getAutoTrackEventProperties(type, properties);
            }
        };
        instance.enableAutoTrack(types, listener);
    }

    /**
     * Record the event duration, call this method to start the timing, stop the timing when the target event is uploaded, and add the attribute #duration to the event properties, in seconds.
     *
     * @param eventName target event name
     */
    public static void timeEvent(String eventName) {
        if (null == instance) return;
        instance.timeEvent(eventName);
    }

    /**
     * Sets the user property, replacing the original value with the new value if the property already exists.
     *
     * @param properties user property
     */
    public static void userSet(JSONObject properties) {
        if (null == instance) return;
        instance.user_set(properties);
    }

    /**
     * Sets a single user attribute, ignoring the new attribute value if the attribute already exists.
     *
     * @param properties user property
     */
    public static void userSetOnce(JSONObject properties) {
        if (null == instance) return;
        instance.user_setOnce(properties);
    }

    /**
     * Reset user properties.
     *
     * @param properties user property
     */
    public static void userUnset(String... properties) {
        if (null == instance) return;
        instance.user_unset(properties);
    }

    /**
     * Only one attribute is set when the user attributes of a numeric type are added.
     *
     * @param properties user property
     */
    public static void userAdd(JSONObject properties) {
        if (null == instance) return;
        instance.user_add(properties);
    }

    /**
     * Append a user attribute of the List type.
     *
     * @param properties user property
     */
    public static void userAppend(JSONObject properties) {
        if (null == instance) return;
        instance.user_append(properties);
    }

    /**
     * The element appended to the library needs to be done to remove the processing, remove the support, and then import.
     *
     * @param properties user property
     */
    public static void userUniqAppend(JSONObject properties) {
        if (null == instance) return;
        instance.user_uniqAppend(properties);
    }

    /**
     * Delete the user attributes, but retain the uploaded event data. This operation is not reversible and should be performed with caution.
     */
    public static void userDelete() {
        if (null == instance) return;
        instance.user_delete();
    }

    /**
     * Set the public event attribute, which will be included in every event uploaded after that. The public event properties are saved without setting them each time.
     *
     * @param superProperties super properties
     */
    public static void setSuperProperties(JSONObject superProperties) {
        if (null == instance) return;
        instance.setSuperProperties(superProperties);
    }

    /**
     * Set dynamic public properties. Each event uploaded after that will contain a public event attribute.
     *
     * @param dynamicSuperPropertiesTracker Dynamic public attribute interface
     */
    public static void setDynamicSuperProperties(final TDDynamicSuperPropertiesHandler dynamicSuperPropertiesTracker) {
        if (null == instance || null == dynamicSuperPropertiesTracker) return;
        ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker tracker = new ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                return dynamicSuperPropertiesTracker.getDynamicSuperProperties();
            }
        };
        instance.setDynamicSuperPropertiesTracker(tracker);
    }

    /**
     * Clears a public event attribute.
     *
     * @param superPropertyName Public event attribute key to clear
     */
    public static void unsetSuperProperty(String superPropertyName) {
        if (null == instance) return;
        instance.unsetSuperProperty(superPropertyName);
    }

    /**
     * Clear all public event attributes.
     */
    public static void clearSuperProperties() {
        if (null == instance) return;
        instance.clearSuperProperties();
    }

    /**
     * Gets the public event properties that have been set.
     *
     * @return Public event properties that have been set
     */
    public static JSONObject getSuperProperties() {
        if (null == instance) return new JSONObject();
        return instance.getSuperProperties();
    }


    /**
     * Gets prefabricated properties for all events.
     *
     * @return TDPresetProperties
     */
    public static TDPresetProperties getPresetProperties() {
        if (null == instance) return new TDPresetProperties();
        return instance.getPresetProperties();
    }

    /**
     * Set the account ID. Each setting overrides the previous value. Login events will not be uploaded.
     *
     * @param loginId account id
     */
    public static void login(String loginId) {
        if (null == instance) return;
        instance.login(loginId);
    }

    /**
     * Clearing the account ID will not upload user logout events.
     */
    public static void logout() {
        if (null == instance) return;
        instance.logout();
    }

    /**
     * Set the distinct ID to replace the default UUID distinct ID.
     *
     * @param identity distinct id
     */
    public static void setDistinctId(String identity) {
        if (null == instance) return;
        instance.identify(identity);
    }

    /**
     * Get a visitor ID: The #distinct_id value in the reported data.
     *
     * @return distinct ID
     */
    public static String getDistinctId() {
        if (null == instance) return "";
        return instance.getDistinctId();
    }

    public static String getAccountId() {
        if (null == instance) return "";
        return instance.getLoginId(false);
    }

    /**
     * Obtain the device ID.
     *
     * @return device ID
     */
    public static String getDeviceId() {
        if (null == instance) return "";
        return instance.getDeviceId();
    }

    /**
     * Empty the cache queue. When this function is called, the data in the current cache queue will attempt to be reported.
     * If the report succeeds, local cache data will be deleted.
     */
    public static void flush() {
        if (null == instance) return;
        instance.flush();
    }

    /**
     * The switch reporting status is suspended and restored.
     *
     * @param status TDTrackStatus
     */
    public static void setTrackStatus(TDTrackStatus status) {
        if (null == instance) return;
        switch (status) {
            case PAUSE:
                instance.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.PAUSE);
                break;
            case STOP:
                instance.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.STOP);
                break;
            case SAVE_ONLY:
                instance.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.SAVE_ONLY);
                break;
            case NORMAL:
                instance.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
                break;
        }
    }

    /**
     * Enable three-party data synchronization.
     *
     * @param types third types
     */
    public static void enableThirdPartySharing(int types) {
        if (null == instance) return;
        instance.enableThirdPartySharing(types);
    }

    /**
     * Enable three-party data synchronization.
     *
     * @param type third types
     * @param obj  extras
     */
    public static void enableThirdPartySharing(int type, Object obj) {
        if (null == instance) return;
        instance.enableThirdPartySharing(type, obj);
    }

    /**
     * Ignore automatic collection events in the extension pack
     */
    public static void ignoreAppViewEventInExtPackage() {
        if (null == instance) return;
        instance.ignoreAppViewEventInExtPackage();
    }

    /**
     * Manually trigger the page browsing event upload.
     *
     * @param activity activity
     */
    public static void trackViewScreen(Activity activity) {
        if (null == instance) return;
        instance.trackViewScreen(activity);
    }

    /**
     * Manually trigger the page browsing event upload.
     *
     * @param fragment Indicates the Fragment instance to be collected
     */
    public static void trackViewScreen(Fragment fragment) {
        if (null == instance) return;
        instance.trackViewScreen(fragment);
    }

    /**
     * Manually trigger the page browsing event upload.
     *
     * @param fragment Indicates the Fragment instance to be collected. Supports fragments of support and androidx libraries.
     */
    public static void trackViewScreen(Object fragment) {
        if (null == instance) return;
        instance.trackViewScreen(fragment);
    }

    /**
     * Ignores control click events of the specified type.
     *
     * @param viewType view type
     */
    public static void ignoreViewType(Class<?> viewType) {
        if (null == instance) return;
        instance.ignoreViewType(viewType);
    }

    /**
     * Enable the automatic collection Fragment browsing event.
     */
    public static void trackFragmentAppViewScreen() {
        if (null == instance) return;
        instance.trackFragmentAppViewScreen();
    }

    /**
     * Ignore automatic collection events for multiple pages, including page browsing and control clicks.
     *
     * @param activity single activity
     */
    public static void ignoreAutoTrackActivity(Class<?> activity) {
        if (null == instance) return;
        instance.ignoreAutoTrackActivity(activity);
    }

    /**
     * Ignore automatic collection events for multiple pages, including page browsing and control clicks.
     *
     * @param activitiesList list of activity
     */
    public static void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        if (null == instance) return;
        instance.ignoreAutoTrackActivities(activitiesList);
    }

    /**
     * Custom control ID. If this parameter is not specified, android:id is used by default.
     *
     * @param view   widget
     * @param viewID widget ID
     */
    public static void setViewID(View view, String viewID) {
        if (null == instance) return;
        instance.setViewID(view, viewID);
    }

    /**
     * Custom control ID. If this parameter is not specified, android:id is used by default.
     *
     * @param view   dialog view
     * @param viewID widget ID
     */
    public static void setViewID(Dialog view, String viewID) {
        if (null == instance) return;
        instance.setViewID(view, viewID);
    }

    /**
     * Customize the properties of the control click event.
     *
     * @param view       Control that you want to set custom properties
     * @param properties properties
     */
    public static void setViewProperties(View view, JSONObject properties) {
        if (null == instance) return;
        instance.setViewProperties(view, properties);
    }

    /**
     * Ignores control click events of the specified view.
     *
     * @param view widget
     */
    public static void ignoreView(View view) {
        if (null == instance) return;
        instance.ignoreView(view);
    }

    /**
     * Support H5 to connect with native APP SDK.
     * This function is called when the WebView is initialized.
     *
     * @param webView WebView instance
     */
    public static void setJsBridge(WebView webView) {
        if (null == instance) return;
        instance.setJsBridge(webView);
    }

    /**
     * Tencent X5 WebView gets through with native APP SDK.
     *
     * @param x5WebView WebView instance
     */
    public static void setJsBridgeForX5WebView(Object x5WebView) {
        if (null == instance) return;
        instance.setJsBridgeForX5WebView(x5WebView);
    }

    /**
     * Automatic collection of event types
     */
    public interface TDAutoTrackEventType {
        /**
         * Start event: including opening the APP and opening the APP from the background
         */
        int APP_START = 1;
        /**
         * Including closing the APP and the App entering the background, and collecting the duration of the startup
         */
        int APP_END = 1 << 1;
        /**
         * The user taps a control in the APP
         */
        int APP_CLICK = 1 << 2;
        /**
         * The user browses the page in the APP (Activity)
         */
        int APP_VIEW_SCREEN = 1 << 3;
        /**
         * Record crash information when APP crashes
         */
        int APP_CRASH = 1 << 4;
        /**
         * Record the behavior of APP being installed
         */
        int APP_INSTALL = 1 << 5;
    }

    /**
     * Automatic collection event callback
     */
    public interface TDAutoTrackEventHandler {

        /**
         * Get properties based on event type
         *
         * @param eventType  Current event type
         * @param properties Current Event properties
         * @return User Added Properties
         */
        JSONObject getAutoTrackEventProperties(int eventType, JSONObject properties);
    }

    /**
     * Data sending status
     */
    public enum TDTrackStatus {
        /**
         * Stop SDK data tracking
         */
        PAUSE,
        /**
         * Stop SDK data tracking to clear the cache
         */
        STOP,
        /**
         * Stop SDK data reporting
         */
        SAVE_ONLY,
        /**
         * resume all status
         */
        NORMAL,
    }

    /**
     * Indicates the network type that can be reported
     */
    public enum TDNetworkType {
        /**
         * Data is reported only in the WiFi environment
         */
        WIFI,
        /**
         * This parameter is reported on all network types
         */
        ALL
    }

    /**
     * This parameter is reported on all network types
     */
    public interface TDDynamicSuperPropertiesHandler {
        /**
         * Get dynamic public properties
         *
         * @return dynamic public properties
         */
        JSONObject getDynamicSuperProperties();
    }

    public interface TDSendDataErrorCallback {
        void onSDKErrorCallback(int code, String errorMsg, String ext);
    }
}
