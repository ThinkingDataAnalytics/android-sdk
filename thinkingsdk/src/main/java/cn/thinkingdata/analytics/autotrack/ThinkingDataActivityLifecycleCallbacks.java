/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.autotrack;

import static cn.thinkingdata.analytics.utils.TDConstants.KEY_BACKGROUND_DURATION;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;

import cn.thinkingdata.analytics.data.EventTimer;
import cn.thinkingdata.analytics.ScreenAutoTracker;
import cn.thinkingdata.analytics.TDPresetProperties;
import cn.thinkingdata.analytics.ThinkingAnalyticsSDK;
import cn.thinkingdata.analytics.ThinkingDataAutoTrackAppViewScreenUrl;
import cn.thinkingdata.analytics.data.DataDescription;
import cn.thinkingdata.analytics.tasks.TrackTaskManager;
import cn.thinkingdata.analytics.utils.ITime;
import cn.thinkingdata.analytics.utils.PropertyUtils;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.core.utils.TDLog;
import cn.thinkingdata.analytics.utils.TDUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ThinkingDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ThinkingAnalytics.ThinkingDataActivityLifecycleCallbacks";
    private boolean resumeFromBackground = false;
    private final Object mActivityLifecycleCallbacksLock = new Object();
    private final ThinkingAnalyticsSDK mThinkingDataInstance;
    private Boolean isLaunch = true;
    private EventTimer startTimer;
    private volatile WeakReference<Activity> mCurrentActivity;
    private final List<WeakReference<Activity>> mStartedActivityList = new ArrayList<>();
    // Indicates whether the end event is collected
    private boolean shouldTrackEndEvent = false;

    public ThinkingDataActivityLifecycleCallbacks(ThinkingAnalyticsSDK instance) {
        this.mThinkingDataInstance = instance;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        TDLog.i(TAG, "onActivityCreated");
        mCurrentActivity = new WeakReference<>(activity);
    }

    private boolean notStartedActivity(Activity activity, boolean remove) {
        synchronized (mActivityLifecycleCallbacksLock) {
            Iterator<WeakReference<Activity>> it = mStartedActivityList.iterator();
            while (it.hasNext()) {
                WeakReference<Activity> current = it.next();
                if (current.get() == activity) {
                    if (remove) {
                        it.remove();
                    }
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        TDLog.i(TAG, "onActivityStarted");
        mCurrentActivity = new WeakReference<>(activity);
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                if (mStartedActivityList.size() == 0) {
                    trackAppStartEvent(null, true);
                }
                if (notStartedActivity(activity, false)) {
                    mStartedActivityList.add(new WeakReference<>(activity));
                } else {
                    TDLog.w(TAG, "Unexpected state. The activity might not be stopped correctly: " + activity);
                }
            }
        } catch (Exception ignore) {
        }
    }

    //延迟上报app启动事件，为了获取start_reason的值
    public void trackAppStartDelay(){
        try{
            ITime appStartEnableTime = mThinkingDataInstance.mCalibratedTimeManager.getTime();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    trackAppStartEvent(appStartEnableTime, false);
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 100);
        }catch (Exception ignore){
        }
    }

    private void trackAppStartEvent(ITime time,boolean isStartLifeCycle){
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                if (isLaunch || resumeFromBackground) {
                    if (mThinkingDataInstance.isAutoTrackEnabled()) {
                        try {
                            if (!mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START)) {
                                if(time != null){
                                    //冷启动场景 判断是否需要在后台发送app启动事件 解决推送拉活导致的大量app启动事件
                                    boolean isConditionMet = TDUtils.isForeground(mThinkingDataInstance.mConfig.mContext)
                                            || isBackgroundStartEventEnabled(mThinkingDataInstance.mConfig.mContext);
                                    if(!isConditionMet) return;
                                }
                                isLaunch = false;
                                final JSONObject properties = new JSONObject();
                                if (!TDPresetProperties.disableList.contains(TDConstants.KEY_RESUME_FROM_BACKGROUND)) {
                                    properties.put(TDConstants.KEY_RESUME_FROM_BACKGROUND, resumeFromBackground);
                                }
                                if (!TDPresetProperties.disableList.contains(TDConstants.KEY_START_REASON)) {
                                    String startReason = getStartReason();
                                    if (!startReason.equals(new JSONObject().toString())) {
                                        properties.put(TDConstants.KEY_START_REASON, startReason);
                                    }
                                }
                                if (mCurrentActivity != null) {
                                    TDUtils.getScreenNameAndTitleFromActivity(properties, mCurrentActivity.get());
                                }
                                if (startTimer != null) {
                                    final long systemUpdateTime = SystemClock.elapsedRealtime();
                                    double backgroundDuration = Double.parseDouble(startTimer.duration(systemUpdateTime));
                                    if (backgroundDuration > 0 && !TDPresetProperties.disableList.contains(TDConstants.KEY_BACKGROUND_DURATION)) {
                                        properties.put(KEY_BACKGROUND_DURATION, backgroundDuration);
                                    }
                                }
                                if (null == time) {
                                    mThinkingDataInstance.autoTrack(TDConstants.APP_START_EVENT_NAME, properties);
                                }else{
                                    mThinkingDataInstance.autoTrack(TDConstants.APP_START_EVENT_NAME, properties, time);
                                }
                            }
                        } catch (Exception e) {
                            TDLog.i(TAG, e);
                        }
                    }
                    if (!mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END)) {
                        mThinkingDataInstance.timeEvent(TDConstants.APP_END_EVENT_NAME);
                        shouldTrackEndEvent = true;
                    }
                    try {
                        if (isStartLifeCycle) {
                            mThinkingDataInstance.appBecomeActive();
                            startTimer = null;
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        });
    }

    private void trackAppEndEvent(){
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mThinkingDataInstance.appEnterBackground();
                    resumeFromBackground = true;
                } catch (Exception ignore) {
                }
                if (shouldTrackEndEvent) {
                    if (mThinkingDataInstance.isAutoTrackEnabled()) {
                        JSONObject properties = new JSONObject();
                        if (!mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END)) {
                            try {
                                if (mCurrentActivity != null) {
                                    TDUtils.getScreenNameAndTitleFromActivity(properties, mCurrentActivity.get());
                                }
                            } catch (Exception e) {
                                TDLog.i(TAG, e);
                            } finally {
                                mThinkingDataInstance.autoTrack(TDConstants.APP_END_EVENT_NAME, properties);
                                shouldTrackEndEvent = false;
                            }
                        }
                    }
                }
                try {
                    final long systemUpdateTime = SystemClock.elapsedRealtime();
                    startTimer = new EventTimer(TimeUnit.SECONDS, systemUpdateTime);
                } catch (Exception ignore) {
                }
                mThinkingDataInstance.flush();
            }
        });

    }

    private void trackAppViewEvent(String screenUrl,JSONObject extProperties){
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                try{
                    JSONObject properties = new JSONObject();
                    if (mCurrentActivity != null) {
                        TDUtils.getScreenNameAndTitleFromActivity(properties, mCurrentActivity.get());
                    }
                    if (extProperties != null && PropertyUtils.checkProperty(extProperties)) {
                        TDUtils.mergeJSONObject(extProperties, properties, mThinkingDataInstance.mConfig.getDefaultTimeZone());
                    }
                    if(TextUtils.isEmpty(screenUrl)){
                        mThinkingDataInstance.autoTrack(TDConstants.APP_VIEW_EVENT_NAME, properties);
                    }else {
                        mThinkingDataInstance.trackViewScreenInternal(screenUrl, properties);
                    }
                }catch (Exception ignore){
                }
            }
        });
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // 有可能执行到onActivityResumed 才开始初始化SDK
        synchronized (mActivityLifecycleCallbacksLock) {
            if (notStartedActivity(activity, false)) {
                TDLog.i(TAG, "onActivityResumed: the SDK was initialized after the onActivityStart of " + activity);
                mStartedActivityList.add(new WeakReference<>(activity));
                if (mStartedActivityList.size() == 1) {
                    trackAppStartEvent(null, false);
                }
            }
        }
        //执行activity的页面浏览事件
        try {
            if (mThinkingDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())) {
                return;
            }
            if (mThinkingDataInstance.isAutoTrackEnabled()
                    && !mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN)) {
                if (activity instanceof ScreenAutoTracker) {
                    ScreenAutoTracker screenAutoTracker = ( ScreenAutoTracker ) activity;

                    String screenUrl = screenAutoTracker.getScreenUrl();
                    JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                    trackAppViewEvent(screenUrl,otherProperties);
                }else{
                    ThinkingDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = activity.getClass().getAnnotation(ThinkingDataAutoTrackAppViewScreenUrl.class);
                    if (autoTrackAppViewScreenUrl != null && (TextUtils.isEmpty(autoTrackAppViewScreenUrl.appId())
                            || mThinkingDataInstance.getToken().equals(autoTrackAppViewScreenUrl.appId()))) {
                        String screenUrl = autoTrackAppViewScreenUrl.url();
                        if (TextUtils.isEmpty(screenUrl)) {
                            screenUrl = activity.getClass().getCanonicalName();
                        }
                        trackAppViewEvent(screenUrl,null);
                    } else {
                        if (!mThinkingDataInstance.isIgnoreAppViewInExtPackage()) {
                            trackAppViewEvent(null,null);
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        synchronized (mActivityLifecycleCallbacksLock) {
            if (notStartedActivity(activity, false)) {
                TDLog.i(TAG, "onActivityPaused: the SDK was initialized after the onActivityStart of " + activity);
                mStartedActivityList.add(new WeakReference<>(activity));
                if (mStartedActivityList.size() == 1) {
                    trackAppStartEvent(null, false);
                }
            }
        }
    }

    boolean isBackgroundStartEventEnabled(Context context) {
        boolean enabled = false;
        try {
            Resources resources = context.getResources();
            enabled = resources.getBoolean(resources.getIdentifier("TAEnableBackgroundStartEvent", "bool", context.getPackageName()));
        } catch (Exception e) {
            //ignored
        }
        return enabled;
    }

    public static Object wrap(Object o) {
        if (o == null) {
            return JSONObject.NULL;
        }
        if (o instanceof JSONArray || o instanceof JSONObject) {
            return o;
        }

        if (o.equals(JSONObject.NULL)) {
            return o;
        }
        try {
            if (o instanceof Collection) {
                return new JSONArray(( Collection ) o);
            } else if (o.getClass().isArray()) {
                return toJSONArray(o);
            }
            if (o instanceof Map) {
                return new JSONObject(( Map ) o);
            }
            if (o instanceof Boolean
                    || o instanceof Byte
                    || o instanceof Character
                    || o instanceof Double
                    || o instanceof Float
                    || o instanceof Integer
                    || o instanceof Long
                    || o instanceof Short
                    || o instanceof String) {
                return o;
            }
            if (o.getClass().getPackage().getName().startsWith("java.")) {
                return o.toString();
            }
        } catch (Exception ignored) {
            //ignored
        }

        return null;

    }

    public static JSONArray toJSONArray(Object array) throws JSONException {
        JSONArray result = new JSONArray();
        if (!array.getClass().isArray()) {
            throw new JSONException("Not a primitive array: " + array.getClass());
        }
        final int length = Array.getLength(array);
        for (int i = 0; i < length; ++i) {
            result.put(wrap(Array.get(array, i)));
        }
        return result;
    }

    String getStartReason() {
        JSONObject object = new JSONObject();
        JSONObject data = new JSONObject();
        if (mCurrentActivity != null) {
            try {
                Activity activity = mCurrentActivity.get();
                Intent intent = activity.getIntent();
                if (intent != null) {
                    String uri = intent.getDataString();
                    if (!TextUtils.isEmpty(uri)) {
                        object.put("url", uri);
                    }
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Set<String> keys = bundle.keySet();
                        for (String key : keys) {
                            Object value = bundle.get(key);
                            Object supportValue = wrap(value);
                            if (supportValue != null && supportValue != JSONObject.NULL) {
                                data.put(key, wrap(value));
                            }
                        }
                        object.put("data", data);
                    }
                }
            } catch (Exception exception) {
                //exception.printStackTrace();
                return object.toString();
            }
        }
        return object.toString();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        TDLog.i(TAG, "onActivityStopped");
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                if (notStartedActivity(activity, true)) {
                    TDLog.i(TAG, "onActivityStopped: the SDK might be initialized after the onActivityStart of " + activity);
                    return;
                }
                if (mStartedActivityList.size() == 0) {
                    trackAppEndEvent();
                }
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    /**
     * < Report the crash and end events when a crash occurs >.
     *
     * @param properties crash_reason
     * @author bugliee
     * @create 2022/3/9
     */
    public void trackAppCrashAndEndEvent(JSONObject properties) {
        mThinkingDataInstance.autoTrack(TDConstants.APP_CRASH_EVENT_NAME, properties);
        mThinkingDataInstance.autoTrack(TDConstants.APP_END_EVENT_NAME, new JSONObject());
        mThinkingDataInstance.flush();
    }

}
