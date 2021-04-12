package cn.thinkingdata.android;

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
import java.util.Map;
import java.util.TimeZone;

public class ThinkingAnalyticsCocosAPI {

    static Map<String,ThinkingAnalyticsSDK> sInstances = new HashMap<>();
    static ThinkingAnalyticsSDK sCurrentInstance;
    static final String LIB_NAME = "Cocos2d-x";
    static final String LIB_VERSION = "1.0";
    static void syncInstances(){
        for(Map<String,ThinkingAnalyticsSDK> map:ThinkingAnalyticsSDK.instances().values())
        {
            sInstances.putAll(map);
        }
    }
    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appId, String url)
    {
        ThinkingAnalyticsSDK.setCustomerLibInfo(LIB_NAME,LIB_VERSION);
        return ThinkingAnalyticsSDK.sharedInstance(context,appId,url);
    }
    public static ThinkingAnalyticsSDK sharedInstance(TDConfig config)
    {
        ThinkingAnalyticsSDK.setCustomerLibInfo(LIB_NAME,LIB_VERSION);
        return ThinkingAnalyticsSDK.sharedInstance(config);
    }
    public static  ThinkingAnalyticsSDK currentInstance()
    {
        if(sCurrentInstance == null)
        {
            syncInstances();
            for(ThinkingAnalyticsSDK instance:sInstances.values())
            {
                sCurrentInstance = instance;
            }
        }

        return  sCurrentInstance;
    }

    public static  void track(String eventName) {
        currentInstance().track(eventName);
    }


    public static void track(String eventName, JSONObject properties) {
        currentInstance().track(eventName,properties);
    }


    public static  void track(String eventName, JSONObject properties, Date time) {
        currentInstance().track(eventName,properties,time);
    }


    public static  void track(String eventName, JSONObject properties, Date time, TimeZone timeZone) {
        currentInstance().track(eventName, properties, time,timeZone);
    }


    public static  void track(ThinkingAnalyticsEvent event) {
        currentInstance().track(event);
    }

    public static  void track(String eventName,JSONObject properties,String extraId,int type)
    {
        ThinkingAnalyticsEvent event = null;
        switch (type)
        {
            case 1://首次事件
            {
                event = new TDFirstEvent(eventName,properties);
                if(extraId != null && extraId.length() != 0)
                {
                    ((TDFirstEvent)event).setFirstCheckId(extraId);
                }
            }
            break;
            case 2://可更新事件
            {
                event = new TDUpdatableEvent(eventName,properties,extraId);
            }
            break;
            case 3://可重写事件
            {
                event = new TDOverWritableEvent(eventName,properties,extraId);
            }
            break;
        }
        if(event != null)
        {
            track(event);
        }
    }


    public static  void timeEvent(String eventName) {
        currentInstance().timeEvent(eventName);
    }


    public static  void login(String loginId) {
        currentInstance().login(loginId);
    }


    public static  void logout() {
        currentInstance().logout();
    }


    public static  void identify(String identify) {
        currentInstance().identify(identify);
    }


    public static  void user_set(JSONObject property) {
        currentInstance().user_set(property);
    }


    public static  void user_setOnce(JSONObject property) {
        currentInstance().user_setOnce(property);
    }


    public static  void user_add(JSONObject property) {
        currentInstance().user_add(property);
    }


    public static  void user_append(JSONObject property) {
        currentInstance().user_append(property);
    }


    public static  void user_add(String propertyName, Number propertyValue) {
        currentInstance().user_add(propertyName,propertyValue);
    }


    public static  void user_delete() {
        currentInstance().user_delete();
    }


    public static  void user_unset(String... properties) {
        currentInstance().user_unset(properties);
    }
    public static  void user_unset(String property) {
        currentInstance().user_unset(property);
    }


    public static  void setSuperProperties(JSONObject superProperties) {
        currentInstance().setSuperProperties(superProperties);
    }


    public static  void setDynamicSuperPropertiesTracker(ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker dynamicSuperPropertiesTracker) {
        currentInstance().setDynamicSuperPropertiesTracker(dynamicSuperPropertiesTracker);
    }


    public static  void unsetSuperProperty(String superPropertyName) {
        currentInstance().unsetSuperProperty(superPropertyName);
    }


    public static  void clearSuperProperties() {
        currentInstance().clearSuperProperties();
    }


    public static String getDistinctId() {
        return currentInstance().getDistinctId();
    }


    public static JSONObject getSuperProperties() {
        return currentInstance().getSuperProperties();
    }



    public static  void enableAutoTrack() {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> typeList = new ArrayList<>();
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        currentInstance().enableAutoTrack(typeList);
    }
    public static  void flush() {
        currentInstance().flush();
    }

    public static  String getDeviceId() {
        return currentInstance().getDeviceId();
    }

    public static  void enableTracking(boolean enabled) {
        currentInstance().enableTracking(enabled);
    }
    public static  void enableTrackLog(boolean enabled)
    {
        ThinkingAnalyticsSDK.enableTrackLog(enabled);
    }


    public static  void optOutTrackingAndDeleteUser() {
        currentInstance().optOutTrackingAndDeleteUser();
    }

    public static  void optOutTracking() {
        currentInstance().optOutTracking();
    }


    public static  void optInTracking() {
        currentInstance().optInTracking();
    }
}
