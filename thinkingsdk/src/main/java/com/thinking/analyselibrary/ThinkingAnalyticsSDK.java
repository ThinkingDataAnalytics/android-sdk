package com.thinking.analyselibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

public class ThinkingAnalyticsSDK
{

    public static ThinkingAnalyticsSDK sharedInstance() {
        synchronized (sInstanceMap) {
            if (sInstanceMap.size() > 0) {
                Iterator<ThinkingAnalyticsSDK> iterator = sInstanceMap.values().iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
            }
            return null;
        }
    }

    public static ThinkingAnalyticsSDK sharedInstance(Context context) {
        if (null == context) {
            return null;
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();
            ThinkingAnalyticsSDK instance = sInstanceMap.get(appContext);

            if (null == instance) {
                TDLog.d(TAG,"please calling method ThinkingAnalyticsSDK sharedInstance(Context context,String appKey, String serverURL, String\n" +
                        "            url) first ");
            }
            return instance;
        }
    }

    public static ThinkingAnalyticsSDK sharedInstance(Context context, String appKey, String url) {
        if (null == context) {
            return null;
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            ThinkingAnalyticsSDK instance = sInstanceMap.get(appContext);
            if (null == instance) {
                instance = new ThinkingAnalyticsSDK(appContext,
                        appKey,
                        url+"/sync",
                        url+"/config");
                sInstanceMap.put(appContext, instance);
            }

            return instance;
        }
    }

    ThinkingAnalyticsSDK(Context context, String appKey, String serverURL, String configureURL) {

        TDLog.d(TAG, "Thank you very much for using Thinking Data. We will do our best to provide you with the best service.");
        TDLog.d(TAG, String.format("Thinking Data SDK version:%s", VERSION));

        mContext = context;
        final String packageName = context.getApplicationContext().getPackageName();
        mappKey = appKey;

        mServerUrl = serverURL;
        mConfigureUrl = configureURL;

        final String prefsName = "com.thinkingdata.analyse";
        final SharedPreferencesFuture sPrefsLoader = new SharedPreferencesFuture();
        Future<SharedPreferences> storedPreferences =
                sPrefsLoader.loadPreferences(context, prefsName);

        mFlushInterval = SettingsUtils.getUploadInterval(mContext);
        mFlushBulkSize = SettingsUtils.getUpladSize(mContext);

        mLoginId = new StorageLoginID(storedPreferences);
        mIdentifyId = new StorageIdentifyId(storedPreferences);
        mRandomID = new StorageRandomID(storedPreferences);
        mSuperProperties = new StorageSuperProperties(storedPreferences);

        mMessages = DataHandle.getInstance(mContext, packageName);
        final Map<String, Object> deviceInfo = TDUtil.getDeviceInfo(mContext);
        mDeviceInfo = Collections.unmodifiableMap(deviceInfo);

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

            mAutoTrackEventTypeList = new ArrayList<>();


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                final Application app = (Application) context.getApplicationContext();
                app.registerActivityLifecycleCallbacks(new ThinkingDataActivityLifecycleCallbacks(this, mMainProcessName));
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        getConfig();
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            JSONObject json = (JSONObject)msg.obj;
            try {
                json = json.getJSONObject("data");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            int newUploadInterval = 0;
            try {
                newUploadInterval = json.getInt("sync_interval");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            int newUplodaSize = 0;
            try {
                newUplodaSize = json.getInt("sync_batch_size");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            SettingsUtils.setUploadInterval(mContext,newUploadInterval*1000);
            SettingsUtils.setUpladSize(mContext,newUplodaSize);

            mFlushInterval = newUploadInterval*1000;
            mFlushBulkSize = newUplodaSize;
        }
    };

    private final class NetworkType {
        public static final int TYPE_NONE = 0;//NULL
        public static final int TYPE_2G = 1;//2G
        public static final int TYPE_3G = 1 << 1;//3G
        public static final int TYPE_4G = 1 << 2;//4G
        public static final int TYPE_WIFI = 1 << 3;//WIFI
        public static final int TYPE_ALL = 0xFF;//ALL
    }

    public enum ThinkingdataNetworkType {
        NETWORKTYPE_DEFAULT,
        NETWORKTYPE_WIFI,
        NETWORKTYPE_ALL
    }

    public void setNetworkType(ThinkingdataNetworkType type){
        if(type == ThinkingdataNetworkType.NETWORKTYPE_DEFAULT)
        {
            mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI;
        }
        else if(type == ThinkingdataNetworkType.NETWORKTYPE_WIFI)
        {
            mNetworkType = NetworkType.TYPE_WIFI;
        }
        else if(type == ThinkingdataNetworkType.NETWORKTYPE_ALL)
        {
            mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI | NetworkType.TYPE_2G;
        }
    }

    public boolean isShouldFlush(String networkType) {
        return (convertToNetworkType(networkType) & mNetworkType) != 0;
    }

    private int convertToNetworkType(String networkType) {
        if ("NULL".equals(networkType)) {
            return NetworkType.TYPE_ALL;
        } else if ("WIFI".equals(networkType)) {
            return NetworkType.TYPE_WIFI;
        } else if ("2G".equals(networkType)) {
            return NetworkType.TYPE_2G;
        } else if ("3G".equals(networkType)) {
            return NetworkType.TYPE_3G;
        } else if ("4G".equals(networkType)) {
            return NetworkType.TYPE_4G;
        }
        return NetworkType.TYPE_ALL;
    }

    private void getConfig(){

        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                HttpURLConnection connection = null;
                InputStream in = null;

                try {
                    URL url = new URL(mConfigureUrl + "?appid="+mappKey);

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    String str;
                    if (connection.getResponseCode() == 200){
                        in = connection.getInputStream();

                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        StringBuffer buffer = new StringBuffer();
                        while((str = br.readLine())!=null){
                            buffer.append(str);
                        }
                        in.close();
                        br.close();
                        JSONObject rjson = new JSONObject(buffer.toString());

                        if (rjson.getString("code").equals("0")) {
                            Message message = new Message();
                            message.obj = rjson;
                            handler.sendMessage(message);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (null != in)
                        try {
                            in.close();
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    if (null != connection)
                        connection.disconnect();
                }
            }
        });
        thread.start();
    }

    protected void autotrack(String eventName, JSONObject properties) {
        clickEvent("track", eventName, properties);
    }

    public void track(String eventName, JSONObject properties) {
        if(!CheckProperty.checkProperty(properties))
            return;

        clickEvent("track", eventName, properties);
    }

    public void track(String eventName, JSONObject properties, Date time) {
        if(!CheckProperty.checkProperty(properties))
            return;

        clickEvent("track", eventName, properties, time);
    }

    public void track(String eventName) {
        try {
            clickEvent("track", eventName, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clickEvent(String eventType, String eventName, JSONObject properties){
        clickEvent(eventType, eventName, properties, new Date());
    }

    private void clickEvent(String eventType, String eventName, JSONObject properties, Date time){
        if(eventType != null && eventType.equals("track")) {
            if(!CheckProperty.checkString(eventName)) {
                TDLog.d(TAG, "property name[" + eventName + "] is not valid");
                return;
            }
        }

        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern, Locale.CHINA);

        String timeString;
        if(time != null) {
            timeString = sDateFormat.format(time);
        }
        else
        {
            timeString = sDateFormat.format(new Date());
        }

        try {
            String networkType = TDUtil.networkType(mContext);

            final JSONObject dataObj = new JSONObject();

            dataObj.put("#time", timeString);
            dataObj.put("#type", eventType);

            final JSONObject sendProps = new JSONObject();

            if (null != properties) {
                final Iterator<?> propIter = properties.keys();
                while (propIter.hasNext()) {
                    final String key = (String) propIter.next();
                    if (!properties.isNull(key)) {
                        sendProps.put(key, properties.get(key));
                    }
                }
            }

            JSONObject finalProperties = new JSONObject();
            if(eventType.equals("track") || eventType.equals("user_signup")) {
                finalProperties.put("#network_type", networkType);
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

            if (!TextUtils.isEmpty(getLoginId())) {
                dataObj.put("#account_id", getLoginId());
            }

            if(eventType.equals("track") || eventType.equals("user_signup")) {
                synchronized (mSuperProperties) {
                    JSONObject superProperties = mSuperProperties.get();
                    DataHandle.mergeJSONObject(superProperties, finalProperties);
                }
            }

            DataHandle.mergeJSONObject(sendProps, finalProperties);

            if(eventName != null && eventName.length() > 0)
                dataObj.put("#event_name", eventName);
            if(finalProperties != null)
                dataObj.put("properties", finalProperties);

            String identifyId = getIdentifyID();
            if(identifyId == null) {
                dataObj.put("#distinct_id", getRandomID());
            }
            else
            {
                dataObj.put("#distinct_id", identifyId);
            }

//            Log.i(TAG, "clickEvent: " + dataObj);

            mMessages.saveClickData(dataObj);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void user_add(String propertyName, Number propertyValue) {
        try {
            if (!(propertyValue instanceof Number)) {
                TDLog.d(TAG, "user_add value must be Number");
            }
            else {
                JSONObject json = new JSONObject();
                json.put(propertyName, propertyValue);
                clickEvent("user_add", null, json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void user_add(JSONObject property) {
        try {

            if(!CheckProperty.checkProperty(property))
                return;

            clickEvent("user_add", null, property);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void user_setOnce(JSONObject property) {
        try {
            if(!CheckProperty.checkProperty(property))
                return;

            clickEvent("user_setOnce", null, property);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void user_set(JSONObject property) {
        try {
            if(!CheckProperty.checkProperty(property))
                return;

            clickEvent("user_set", null, property);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void user_delete() {
        try {
            clickEvent("user_del", null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void identify(String identify) {
        if (identify == null) {
            TDLog.d(TAG,"The identify cannot empty.");
            return;
        }

        synchronized (mIdentifyId) {
            mIdentifyId.put(identify);
        }
    }

    public void login(String loginId) {
        try {
            if(TDUtil.checkNull(loginId)) {
                TDLog.d(TAG,"login_id cannot empty.");
                return;
            }

            synchronized (mLoginId) {
                if (!loginId.equals(mLoginId.get())) {
                    mLoginId.put(loginId);
//                    clickEvent("user_signup", "#signup", null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        try {
            synchronized (mLoginId) {
                mLoginId.put(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLoginId() {
        synchronized (mLoginId) {
            return mLoginId.get();
        }
    }

    private String getRandomID() {
        synchronized (mRandomID) {
            return mRandomID.get();
        }
    }

    private String getIdentifyID() {
        synchronized (mIdentifyId) {
            return mIdentifyId.get();
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

    public JSONObject getSuperProperties(){
        synchronized (mSuperProperties) {
            return mSuperProperties.get();
        }
    }

    public void setSuperProperties(JSONObject superProperties) {
        try {
            if (superProperties == null) {
                return;
            }
            if(!CheckProperty.checkProperty(superProperties))
                return;

            synchronized (mSuperProperties) {
                JSONObject properties = mSuperProperties.get();
                DataHandle.mergeJSONObject(superProperties, properties);
                mSuperProperties.put(properties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unsetSuperProperty(String superPropertyName) {
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

    public void clearSuperProperties() {
        synchronized (mSuperProperties) {
            mSuperProperties.put(new JSONObject());
        }
    }

    public void timeEvent(final String eventName) {
        try {
            if(!CheckProperty.checkString(eventName)) {
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
        APP_VIEW_SCREEN("ta_app_view");
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

    public void trackViewScreenNei(String url, JSONObject properties) {
        try {
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
                autotrack("ta_app_view", trackProperties);
            }
        } catch (JSONException e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }

    public void trackViewScreen(String url, JSONObject properties) {
        try {
            if ((!TextUtils.isEmpty(url) || properties != null) && CheckProperty.checkProperty(properties)) {
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
                autotrack("ta_app_view", trackProperties);
            }
        } catch (JSONException e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }

    /**
     * Track Activity 进入页面事件($AppViewScreen)
     * @param activity activity Activity，当前 Activity
     */
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

                trackViewScreenNei(screenUrl, properties);
            } else {
                autotrack("ta_app_view", properties);
            }
        } catch (Exception e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }

    public void trackViewScreen(android.app.Fragment fragment) {
        try {
            if (fragment == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            String fragmentName = fragment.getClass().getCanonicalName();
            String screenName = fragmentName;

            if (Build.VERSION.SDK_INT >= 11) {
                Activity activity = fragment.getActivity();
                if (activity != null) {
                    String activityTitle = TDUtil.getActivityTitle(activity);
                    if (!TextUtils.isEmpty(activityTitle)) {
                        properties.put("#title", activityTitle);
                    }
                    screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName);
                }
            }

            properties.put("#screen_name", screenName);
            autotrack("ta_app_view", properties);
        } catch (Exception e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }

    public void trackViewScreen(android.support.v4.app.Fragment fragment) {
        try {
            if (fragment == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            String fragmentName = fragment.getClass().getCanonicalName();
            String screenName = fragmentName;

            Activity activity = fragment.getActivity();
            if (activity != null) {
                String activityTitle = TDUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put("#title", activityTitle);
                }
                screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName);
            }

            properties.put("#screen_name", screenName);
            autotrack("ta_app_view", properties);
        } catch (Exception e) {
            TDLog.i(TAG, "trackViewScreen:" + e);
        }
    }


    protected void appEnterBackground() {
        synchronized (mTrackTimer) {
            try {
                Iterator iter = mTrackTimer.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
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

    /**
     * app从后台恢复
     * 遍历mTrackTimer
     * startTime = System.currentTimeMillis()
     */
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

    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        mAutoTrack = true;
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
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

    public void flush() {
        mMessages.flush();
    }

    private List<Class> mIgnoredViewTypeList = new ArrayList<>();

    public List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

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

    public void trackFragmentAppViewScreen() {
        this.mTrackFragmentAppViewScreen = true;
    }

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

    public void setViewID(View view, String viewID) {
        if (view != null && !TextUtils.isEmpty(viewID)) {
            view.setTag(R.id.thinking_analytics_tag_view_id, viewID);
        }
    }

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

    public void setViewID(android.support.v7.app.AlertDialog view, String viewID) {
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

    public void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }

        view.setTag(R.id.thinking_analytics_tag_view_properties, properties);
    }

    public void ignoreView(View view) {
        if (view != null) {
            view.setTag(R.id.thinking_analytics_tag_view_ignored, "1");
        }
    }

    private final Context mContext;
    private final DataHandle mMessages;

    private int mFlushBulkSize;
    private int mFlushInterval;

    private final String mappKey;
    private final String mServerUrl;
    private final String mConfigureUrl;
    private final Map<String, Object> mDeviceInfo;

    static final String VERSION = "1.1.6";
    private static final String TAG = "ThinkingAnalyticsSDK";

    private boolean mAutoTrack;
    private List<AutoTrackEventType> mAutoTrackEventTypeList;

    protected int getFlushBulkSize() {
        return mFlushBulkSize;
    }

    protected int getFlushInterval() {
        return mFlushInterval;
    }

    protected String getServerUrl() {
        return mServerUrl;
    }

    protected String getAppid() {
        return mappKey;
    }

    protected Map<String, Object> getDeviceInfo() {
        return mDeviceInfo;
    }
    private final StorageLoginID mLoginId;
    private final StorageIdentifyId mIdentifyId;
    private final StorageRandomID mRandomID;
    private final StorageSuperProperties mSuperProperties;
    private final Map<String, EventTimer> mTrackTimer;
    private int mNetworkType = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI;

    private List<Integer> mAutoTrackIgnoredActivities;
    private JSONObject mLastScreenTrackProperties;
    private String mLastScreenUrl;
    private boolean mClearReferrerWhenAppEnd = false;
    private String mMainProcessName;

    private static volatile ThinkingAnalyticsSDK instance = null;
    private static final Map<Context, ThinkingAnalyticsSDK> sInstanceMap = new HashMap<>();
    private boolean mEnableButterknifeOnClick;
    private boolean mTrackFragmentAppViewScreen;
}

