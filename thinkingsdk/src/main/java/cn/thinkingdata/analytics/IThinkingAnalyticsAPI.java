/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONObject;

interface IThinkingAnalyticsAPI {
    /**
     * Upload a single event, containing only preset properties and set public properties.
     *
     * @param eventName event name
     */
    void track(String eventName);

    /**
     * Upload events and their associated attributes.
     *
     * @param eventName event name
     * @param properties event properties
     */
    void track(String eventName, JSONObject properties);

    /**
     * Upload event, you can set the event trigger time. SDK support began with version 1.1.5. This method is deprecated in v2.2.0+
     * time will be reported as a string serialized to the specified format according to the device's default time zone, but #zone_offset will not be set.
     *
     * @param eventName event name
     * @param properties event properties
     * @param time event time
     */
    @Deprecated
    void track(String eventName, JSONObject properties, Date time);

    /**
     * Upload the event and set the event trigger time. SDK v2.2.0 support is available.
     *
     * @param eventName event name
     * @param properties event properties
     * @param time event time
     * @param timeZone event timeZone
     */
    void track(String eventName, JSONObject properties, Date time, final TimeZone timeZone);

    /**
     * Upload a special type of event.
     *
     * @param event Event Object TDUniqueEvent / TDUpdatableEvent / TDOverWritableEvent
     */
    void track(ThinkingAnalyticsEvent event);

    /**
     * Record the event duration, call this method to start the timing, stop the timing when the target event is uploaded, and add the attribute #duration to the event properties, in seconds.
     *
     * @param eventName target event name
     */
    void timeEvent(String eventName);

    /**
     * Set the account ID. Each setting overrides the previous value. Login events will not be uploaded.
     *
     * @param loginId account ID
     */
    void login(String loginId);

    /**
     * Clearing the account ID will not upload user logout events.
     */
    void logout();

    /**
     * Set the distinct ID to replace the default UUID distinct ID.
     *
     * @param identify
     */
    void identify(String identify);

    /**
     * Sets the user property, replacing the original value with the new value if the property already exists.
     *
     * @param property user property
     */
    void user_set(JSONObject property);

    /**
     *  Sets a single user attribute, ignoring the new attribute value if the attribute already exists.
     *
     * @param property user property
     */
    void user_setOnce(JSONObject property);

    /**
     * Adds the numeric type user attributes.
     *
     * @param property user property，JSONObject
     */
    void user_add(JSONObject property);

    /**
     * Only one attribute is set when the user attributes of a numeric type are added.
     *
     * @param propertyName
     * @param propertyValue
     */
    void user_add(String propertyName, Number propertyValue);

    /**
     * Append a user attribute of the List type.
     *
     * @param property user property，JSONObject
     */
    void user_append(JSONObject property);

    /**
     *  The element appended to the library needs to be done to remove the processing, remove the support, and then import.
     *
     * @param property JSONObject
     */
    void user_uniqAppend(JSONObject property);

    /**
     * Delete the user attributes, but retain the uploaded event data. This operation is not reversible and should be performed with caution.
     */
    void user_delete();

    /**
     * Reset user properties.
     *
     * @param properties
     */
    void user_unset(String... properties);

    /**
     * Set the public event attribute, which will be included in every event uploaded after that. The public event properties are saved without setting them each time.
     *
     * @param superProperties
     */
    void setSuperProperties(JSONObject superProperties);

    /**
     *  Set dynamic public properties. Each event uploaded after that will contain a public event attribute.
     *
     * @param dynamicSuperPropertiesTracker Dynamic public attribute interface
     */
    void setDynamicSuperPropertiesTracker(ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker dynamicSuperPropertiesTracker);

    /**
     * Clears a public event attribute.
     *
     * @param superPropertyName Public event attribute key to clear
     */
    void unsetSuperProperty(String superPropertyName);

    /**
     *  Clear all public event attributes.
     */
    void clearSuperProperties();

    /**
     * Get a visitor ID: The #distinct_id value in the reported data.
     *
     * @return distinct ID
     */
    String getDistinctId();

    /**
     * Gets the public event properties that have been set.
     *
     * @return JSONObject Public event properties that have been set
     */
    JSONObject getSuperProperties();

    /**
     * Set the network conditions for uploading. By default, the SDK will upload data on 3G, 4G and Wifi.
     *
     * @param type Type of the network on which data is uploaded
     */
    void setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType type);

    /**
     * Enable the collection of installation events. added for unity3D.
     */
    void trackAppInstall();

    /**
     * Enable the auto tracking function.
     *
     * @param eventTypeList  Indicates the type of the automatic collection event to be enabled
     */
    void enableAutoTrack(List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList);

    /**
     *  Enable the automatic collection Fragment browsing event.
     */
    void trackFragmentAppViewScreen();

    /**
     *  Ignore automatic collection events in the extension pack
     */
    void ignoreAppViewEventInExtPackage();

    /**
     * Manually trigger the page browsing event upload.
     *
     * @param activity Activity
     */
    void trackViewScreen(Activity activity);

    /**
     * Manually trigger the page browsing event upload.
     *
     * @param fragment  Indicates the Fragment instance to be collected
     */
    void trackViewScreen(android.app.Fragment fragment);

    /**
     * Manually trigger the page browsing event upload.
     *
     * @param fragment Indicates the Fragment instance to be collected. Supports fragments of support and androidx libraries.
     */
    void trackViewScreen(Object fragment);

    /**
     * Custom control ID. If this parameter is not specified, android:id is used by default.
     *
     * @param view widget
     * @param viewID widget ID
     */
    void setViewID(View view, String viewID);

    /**
     * Custom Dialog control ID. If not set, android:id is used by default.
     *
     * @param view widget
     * @param viewID widget ID
     */
    void setViewID(android.app.Dialog view, String viewID);

    /**
     * Customize the properties of the control click event.
     *
     * @param view Control that you want to set custom properties
     * @param properties
     */
    void setViewProperties(View view, JSONObject properties);

    /**
     * Ignores automatic collection events for the specified page, including page browsing and control clicking events.
     *
     * @param activity
     */
    void ignoreAutoTrackActivity(Class<?> activity);

    /**
     *  Ignore automatic collection events for multiple pages, including page browsing and control clicks.
     *
     * @param activitiesList
     */
    void ignoreAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * Empty the cache queue. When this function is called, the data in the current cache queue will attempt to be reported.
     * If the report succeeds, local cache data will be deleted.
     */
    void flush();

    /**
     *  Ignores control click events of the specified type.
     *
     * @param viewType
     */
    void ignoreViewType(Class viewType);

    /**
     *  Ignores the click event for the specified element.
     *
     * @param view
     */
    void ignoreView(View view);

    /**
     * Support H5 to connect with native APP SDK.
     * This function is called when the WebView is initialized.
     *
     * @param webView
     */
    void setJsBridge(WebView webView);

    /**
     * Tencent X5 WebView gets through with native APP SDK.
     *
     * @param x5WebView WebView instance
     */
    void setJsBridgeForX5WebView(Object x5WebView);

    /**
     * Obtain the device ID.
     *
     * @return device ID
     */
    String getDeviceId();


    /**
     * The switch reporting status is suspended and restored.
     *
     * @param status TATrackStatus
     */
    void setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus status);

    /**
     * Enable or disable the instance function. When the SDK function is disabled, the cached data is retained and continued to be reported. But it doesn't track subsequent data and changes.
     *
     * @param enabled true Opening the Report; false Disable reporting
     */
    void enableTracking(boolean enabled);

    /**
     * Stop reporting the user data. After this interface is invoked, the local cache data and previous Settings are deleted.
     * Subsequent reports and Settings are invalid. And send user_del (no retry)
     */
    void optOutTrackingAndDeleteUser();

    /**
     * Example Stop reporting the user data. After calling this interface,
     * the local cache data and previous Settings are deleted; Subsequent reports and Settings are invalid.
     */
    void optOutTracking();

    /**
     * Enable the reporting of the instance.
     */
    void optInTracking();

    /**
     * Create lightweight SDK instances. Lightweight SDK instances do not support
     * caching of local account ids, guest ids, public properties, etc.
     *
     * @return SDK instance
     */
    IThinkingAnalyticsAPI createLightInstance();

    /**
     * Gets prefabricated properties for all events.
     */
    TDPresetProperties getPresetProperties();

    /**
     * Set the custom properties of the automatic collection event.
     * The uploaded automatic collection event will contain the event properties that have been set for the event
     *
     * @param autoTrackEventProperties
     */
    void setAutoTrackProperties(List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList, JSONObject autoTrackEventProperties);

    /**
     * Gets the custom property for automatically collecting events that has been set.
     *
     * @return JSONObejct
     */
    JSONObject getAutoTrackProperties();

    /**
     * Enable three-party data synchronization.
     *
     * @param types
     */
    void enableThirdPartySharing(int types);

}
