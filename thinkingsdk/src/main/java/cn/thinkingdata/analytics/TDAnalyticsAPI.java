/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import cn.thinkingdata.analytics.TDAnalytics.*;

/**
 * The packaging class of multi ThinkingAnalyticsSDK instance provides static methods, which is more convenient for customers to use
 *
 * @author liulongbing
 * @since 2023/6/28
 */
public class TDAnalyticsAPI {


    /**
     * Set the network conditions for uploading. By default, the SDK will upload data on 3G, 4G and Wifi.
     *
     * @param networkType Type of the network on which data is uploaded
     * @param appId       app id
     */
    public static void setNetworkType(TDNetworkType networkType, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            switch (networkType) {
                case WIFI:
                    instance.setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType.NETWORKTYPE_WIFI);
                    break;
                case ALL:
                    instance.setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType.NETWORKTYPE_ALL);
                    break;
            }
        }
    }

    /**
     * Create lightweight SDK instances. Lightweight SDK instances do not support
     * caching of local account ids, guest ids, public properties, etc.
     *
     * @param appId app id
     * @return light instance
     */
    public static String lightInstance(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null == instance) return "";
        ThinkingAnalyticsSDK lightInstance = instance.createLightInstance();
        String uuid = UUID.randomUUID().toString();
        TDAnalytics.sInstances.put(uuid, lightInstance);
        return uuid;
    }

    /**
     * Upload a single event, containing only preset properties and set public properties.
     *
     * @param eventName event name
     * @param appId     app id
     */
    public static void track(String eventName, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.track(eventName);
        }
    }

    /**
     * Upload events and their associated attributes.
     *
     * @param eventName  event name
     * @param properties event properties
     * @param appId      app id
     */
    public static void track(String eventName, JSONObject properties, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.track(eventName, properties);
        }
    }

    /**
     * Upload the event and set the event trigger time. SDK v2.2.0 support is available.
     *
     * @param eventName  event name
     * @param properties event properties
     * @param time       event time
     * @param timeZone   event timeZone
     * @param appId      app id
     */
    public static void track(String eventName, JSONObject properties, Date time, final TimeZone timeZone, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.track(eventName, properties, time, timeZone);
        }
    }

    /**
     * Upload a special type of event.
     *
     * @param eventModel Event Object TDFirstEventModel / TDOverWritableEventModel / TDUpdatableEventModel
     * @param appId      app id
     */
    public static void track(TDEventModel eventModel, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.track(eventModel);
        }
    }

    /**
     * Enable the auto tracking function.
     *
     * @param autoTrackEventType Indicates the type of the automatic collection event to be enabled
     * @param appId              app id
     */
    public static void enableAutoTrack(int autoTrackEventType, String appId) {
        enableAutoTrack(autoTrackEventType, new JSONObject(), appId);
    }

    /**
     * Enable the auto tracking function with properties
     *
     * @param autoTrackEventType Indicates the type of the automatic collection event to be enabled
     * @param properties         properties
     * @param appId              app id
     */
    public static void enableAutoTrack(int autoTrackEventType, JSONObject properties, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
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
    }

    /**
     * Enable the auto tracking function with properties
     *
     * @param autoTrackEventType Indicates the type of the automatic collection event to be enabled
     * @param eventHandler       callback
     * @param appId              app id
     */
    public static void enableAutoTrack(int autoTrackEventType, final TDAutoTrackEventHandler eventHandler, String appId) {
        if (null == eventHandler) return;
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
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
    }

    /**
     * Record the event duration, call this method to start the timing, stop the timing when the target event is uploaded, and add the attribute #duration to the event properties, in seconds.
     *
     * @param eventName target event name
     * @param appId     app id
     */
    public static void timeEvent(String eventName, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.timeEvent(eventName);
        }
    }

    /**
     * Sets the user property, replacing the original value with the new value if the property already exists.
     *
     * @param properties user property
     * @param appId      app id
     */
    public static void userSet(JSONObject properties, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.user_set(properties);
        }
    }

    /**
     * Sets a single user attribute, ignoring the new attribute value if the attribute already exists.
     *
     * @param properties user property
     * @param appId      app id
     */
    public static void userSetOnce(JSONObject properties, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.user_setOnce(properties);
        }
    }

    /**
     * Reset user properties.
     *
     * @param properties user property and last is app id
     */
    public static void userUnset(String... properties) {
        if (properties.length > 0) {
            ThinkingAnalyticsSDK instance = getInstance(properties[properties.length - 1]);
            if (null != instance) {
                for (int i = 0; i < properties.length - 1; i++) {
                    instance.user_unset(properties[i]);
                }
            }
        }
    }

    /**
     * Only one attribute is set when the user attributes of a numeric type are added.
     *
     * @param properties user property
     * @param appId      app id
     */
    public static void userAdd(JSONObject properties, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.user_add(properties);
        }
    }

    /**
     * Append a user attribute of the List type.
     *
     * @param properties user property
     * @param appId      app id
     */
    public static void userAppend(JSONObject properties, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.user_append(properties);
        }
    }

    /**
     * The element appended to the library needs to be done to remove the processing, remove the support, and then import.
     *
     * @param properties user property
     * @param appId      app id
     */
    public static void userUniqAppend(JSONObject properties, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.user_uniqAppend(properties);
        }
    }

    /**
     * Delete the user attributes, but retain the uploaded event data. This operation is not reversible and should be performed with caution.
     *
     * @param appId app id
     */
    public static void userDelete(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.user_delete();
        }
    }

    /**
     * Set the public event attribute, which will be included in every event uploaded after that. The public event properties are saved without setting them each time.
     *
     * @param superProperties super properties
     * @param appId           app id
     */
    public static void setSuperProperties(JSONObject superProperties, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.setSuperProperties(superProperties);
        }
    }

    /**
     * Set dynamic public properties. Each event uploaded after that will contain a public event attribute.
     *
     * @param dynamicSuperPropertiesTracker Dynamic public attribute interface
     * @param appId                         app id
     */
    public static void setDynamicSuperProperties(final TDDynamicSuperPropertiesHandler dynamicSuperPropertiesTracker, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance && null != dynamicSuperPropertiesTracker) {
            ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker tracker = new ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker() {
                @Override
                public JSONObject getDynamicSuperProperties() {
                    return dynamicSuperPropertiesTracker.getDynamicSuperProperties();
                }
            };
            instance.setDynamicSuperPropertiesTracker(tracker);
        }
    }

    /**
     * Clears a public event attribute.
     *
     * @param superPropertyName Public event attribute key to clear
     * @param appId             app id
     */
    public static void unsetSuperProperty(String superPropertyName, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.unsetSuperProperty(superPropertyName);
        }
    }

    /**
     * Clear all public event attributes.
     *
     * @param appId app id
     */
    public static void clearSuperProperties(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.clearSuperProperties();
        }
    }

    /**
     * Gets the public event properties that have been set.
     *
     * @param appId app id
     * @return Public event properties that have been set
     */
    public static JSONObject getSuperProperties(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            return instance.getSuperProperties();
        }
        return new JSONObject();
    }

    /**
     * Gets prefabricated properties for all events.
     *
     * @param appId app id
     * @return TDPresetProperties
     */
    public static TDPresetProperties getPresetProperties(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            return instance.getPresetProperties();
        }
        return new TDPresetProperties();
    }

    /**
     * Set the account ID. Each setting overrides the previous value. Login events will not be uploaded.
     *
     * @param loginId account id
     * @param appId   app id
     */
    public static void login(String loginId, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.login(loginId);
        }
    }

    /**
     * Clearing the account ID will not upload user logout events.
     *
     * @param appId app id
     */
    public static void logout(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.logout();
        }
    }

    /**
     * Set the distinct ID to replace the default UUID distinct ID.
     *
     * @param identity distinct id
     * @param appId    app id
     */
    public static void setDistinctId(String identity, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.identify(identity);
        }
    }

    /**
     * Get a visitor ID: The #distinct_id value in the reported data.
     *
     * @param appId app id
     * @return distinct ID
     */
    public static String getDistinctId(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            return instance.getDistinctId();
        }
        return "";
    }

    /**
     * Obtain the device ID.
     *
     * @param appId app id
     * @return device ID
     */
    public static String getDeviceId(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            return instance.getDeviceId();
        }
        return "";
    }

    /**
     * Empty the cache queue. When this function is called, the data in the current cache queue will attempt to be reported.
     * If the report succeeds, local cache data will be deleted.
     *
     * @param appId app id
     */
    public static void flush(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.flush();
        }
    }

    /**
     * The switch reporting status is suspended and restored.
     *
     * @param status TDTrackStatus
     * @param appId  app id
     */
    public static void setTrackStatus(TDAnalytics.TDTrackStatus status, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
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
    }

    /**
     * Enable three-party data synchronization.
     *
     * @param types third types
     * @param appId app id
     */
    public static void enableThirdPartySharing(int types, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.enableThirdPartySharing(types);
        }
    }

    /**
     * Enable three-party data synchronization.
     *
     * @param type  third types
     * @param obj   extras
     * @param appId app id
     */
    public static void enableThirdPartySharing(int type, Object obj, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.enableThirdPartySharing(type, obj);
        }
    }

    /**
     * Ignore automatic collection events in the extension pack
     *
     * @param appId app id
     */
    public static void ignoreAppViewEventInExtPackage(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.ignoreAppViewEventInExtPackage();
        }
    }

    /**
     * Manually trigger the page browsing event upload.
     *
     * @param activity activity
     * @param appId    app id
     */
    public static void trackViewScreen(Activity activity, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.trackViewScreen(activity);
        }
    }

    /**
     * Manually trigger the page browsing event upload.
     *
     * @param fragment Indicates the Fragment instance to be collected
     * @param appId    app id
     */
    public static void trackViewScreen(Fragment fragment, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.trackViewScreen(fragment);
        }
    }

    /**
     * Manually trigger the page browsing event upload.
     *
     * @param fragment Indicates the Fragment instance to be collected. Supports fragments of support and androidx libraries.
     * @param appId    app id
     */
    public static void trackViewScreen(Object fragment, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.trackViewScreen(fragment);
        }
    }

    /**
     * Ignores control click events of the specified type.
     *
     * @param viewType view type
     * @param appId    app id
     */
    public static void ignoreViewType(Class<?> viewType, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.ignoreViewType(viewType);
        }
    }

    /**
     * Enable the automatic collection Fragment browsing event.
     *
     * @param appId app id
     */
    public static void trackFragmentAppViewScreen(String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.trackFragmentAppViewScreen();
        }
    }

    /**
     * Ignore automatic collection events for multiple pages, including page browsing and control clicks.
     *
     * @param activity single activity
     * @param appId    app id
     */
    public static void ignoreAutoTrackActivity(Class<?> activity, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.ignoreAutoTrackActivity(activity);
        }
    }

    /**
     * Ignore automatic collection events for multiple pages, including page browsing and control clicks.
     *
     * @param activitiesList list of activity
     * @param appId          app id
     */
    public static void ignoreAutoTrackActivities(List<Class<?>> activitiesList, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.ignoreAutoTrackActivities(activitiesList);
        }
    }

    /**
     * Custom control ID. If this parameter is not specified, android:id is used by default.
     *
     * @param view   widget
     * @param viewID widget ID
     * @param appId  app id
     */
    public static void setViewID(View view, String viewID, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.setViewID(view, viewID);
        }
    }

    /**
     * Custom control ID. If this parameter is not specified, android:id is used by default.
     *
     * @param view   dialog view
     * @param viewID widget ID
     * @param appId  app id
     */
    public static void setViewID(Dialog view, String viewID, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.setViewID(view, viewID);
        }
    }

    /**
     * Customize the properties of the control click event.
     *
     * @param view       Control that you want to set custom properties
     * @param properties properties
     * @param appId      app id
     */
    public static void setViewProperties(View view, JSONObject properties, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.setViewProperties(view, properties);
        }
    }

    /**
     * Ignores control click events of the specified view.
     *
     * @param view  widget
     * @param appId app id
     */
    public static void ignoreView(View view, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.ignoreView(view);
        }
    }

    /**
     * Support H5 to connect with native APP SDK.
     * This function is called when the WebView is initialized.
     *
     * @param webView WebView instance
     * @param appId   app id
     */
    public static void setJsBridge(WebView webView, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.setJsBridge(webView);
        }
    }

    /**
     * Tencent X5 WebView gets through with native APP SDK.
     *
     * @param x5WebView WebView instance
     * @param appId     app id
     */
    public static void setJsBridgeForX5WebView(Object x5WebView, String appId) {
        ThinkingAnalyticsSDK instance = getInstance(appId);
        if (null != instance) {
            instance.setJsBridgeForX5WebView(x5WebView);
        }
    }

    public static ThinkingAnalyticsSDK getInstance(String appId) {
        if (null == appId) return null;
        appId = appId.replace(" ", "");
        if (TextUtils.isEmpty(appId)) {
            return TDAnalytics.instance;
        }
        return TDAnalytics.sInstances.get(appId);
    }

}
