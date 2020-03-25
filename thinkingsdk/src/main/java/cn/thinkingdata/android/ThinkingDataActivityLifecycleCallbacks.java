package cn.thinkingdata.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import cn.thinkingdata.android.utils.ITime;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDUtils;
import cn.thinkingdata.android.utils.PropertyUtils;
import cn.thinkingdata.android.utils.TDLog;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class ThinkingDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ThinkingAnalytics.ThinkingDataActivityLifecycleCallbacks";
    private boolean resumeFromBackground = false;
    private final Object mActivityLifecycleCallbacksLock = new Object();
    private final ThinkingAnalyticsSDK mThinkingDataInstance;
    private final String mMainProcessName;

    private final List<WeakReference<Activity>> mStartedActivityList = new ArrayList<>();

    public ThinkingDataActivityLifecycleCallbacks(ThinkingAnalyticsSDK instance, String mainProcessName) {
        this.mThinkingDataInstance = instance;
        this.mMainProcessName = mainProcessName;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    private boolean notStartedActivity(Activity activity, boolean remove) {
        synchronized (mActivityLifecycleCallbacksLock) {
            Iterator<WeakReference<Activity>> it = mStartedActivityList.iterator();
            while (it.hasNext()) {
                WeakReference<Activity> current = it.next();
                if (current.get() == activity) {
                    if (remove) it.remove();
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                if (mStartedActivityList.size() == 0) {
                    try {
                        mThinkingDataInstance.appBecomeActive();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    trackAppStart(activity, null);
                }

                if (notStartedActivity(activity, false)) {
                    mStartedActivityList.add(new WeakReference<>(activity));
                } else {
                    TDLog.w(TAG, "Unexpected state. The activity might not be stopped correctly: " + activity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void trackAppStart(Activity activity, ITime time) {
        if (isMainProcess(activity)) {
            if (mThinkingDataInstance.isAutoTrackEnabled()) {
                try {
                    if (!mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START)) {

                        JSONObject properties = new JSONObject();
                        properties.put(TDConstants.KEY_RESUME_FROM_BACKGROUND, resumeFromBackground);
                        TDUtils.getScreenNameAndTitleFromActivity(properties, activity);

                        if (null == time) {
                            mThinkingDataInstance.autoTrack(TDConstants.APP_START_EVENT_NAME, properties);
                        } else {
                            if (!mThinkingDataInstance.hasDisabled()) {
                                // track APP_START with cached time and properties.
                                JSONObject finalProperties = mThinkingDataInstance.getAutoTrackStartProperties();

                                TDUtils.mergeJSONObject(properties, finalProperties, mThinkingDataInstance.mConfig.getDefaultTimeZone());

                                DataDescription dataDescription = new DataDescription(mThinkingDataInstance, TDConstants.DataType.TRACK, finalProperties, time);
                                dataDescription.eventName = TDConstants.APP_START_EVENT_NAME;

                                mThinkingDataInstance.trackInternal(dataDescription);
                            }
                        }
                    }

                    if (time == null && !mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END)) {
                        mThinkingDataInstance.timeEvent(TDConstants.APP_END_EVENT_NAME);
                    }
                } catch (Exception e) {
                    TDLog.i(TAG, e);
                }
            }
            resumeFromBackground = true;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        synchronized (mActivityLifecycleCallbacksLock) {
            if (notStartedActivity(activity, false)) {
                TDLog.i(TAG, "onActivityResumed: the SDK was initialized after the onActivityStart of " + activity);
                mStartedActivityList.add(new WeakReference<>(activity));
                if (mStartedActivityList.size() == 1) {
                    trackAppStart(activity, mThinkingDataInstance.getAutoTrackStartTime());
                }
            }
        }

        try {
            boolean mShowAutoTrack = true;
            if (mThinkingDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())) {
                mShowAutoTrack = false;
            }

            if (mThinkingDataInstance.isAutoTrackEnabled() && mShowAutoTrack && !mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN)) {
                try {
                    JSONObject properties = new JSONObject();
                    properties.put(TDConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                    TDUtils.getScreenNameAndTitleFromActivity(properties, activity);

                    if (activity instanceof ScreenAutoTracker) {
                        ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;

                        String screenUrl = screenAutoTracker.getScreenUrl();
                        JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                        if (otherProperties != null && PropertyUtils.checkProperty(otherProperties)) {
                            TDUtils.mergeJSONObject(otherProperties, properties, mThinkingDataInstance.mConfig.getDefaultTimeZone());
                        } else {
                            TDLog.d(TAG, "invalid properties: " + otherProperties);
                        }
                        mThinkingDataInstance.trackViewScreenInternal(screenUrl, properties);
                    } else {
                        ThinkingDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = activity.getClass().getAnnotation(ThinkingDataAutoTrackAppViewScreenUrl.class);
                        if (autoTrackAppViewScreenUrl != null && (TextUtils.isEmpty(autoTrackAppViewScreenUrl.appId()) ||
                                        mThinkingDataInstance.getToken().equals(autoTrackAppViewScreenUrl.appId()))) {
                            String screenUrl = autoTrackAppViewScreenUrl.url();
                            if (TextUtils.isEmpty(screenUrl)) {
                                screenUrl = activity.getClass().getCanonicalName();
                            }
                            mThinkingDataInstance.trackViewScreenInternal(screenUrl, properties);
                        } else {
                            mThinkingDataInstance.autoTrack(TDConstants.APP_VIEW_EVENT_NAME, properties);
                        }
                    }
                } catch (Exception e) {
                    TDLog.i(TAG, e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        synchronized (mActivityLifecycleCallbacksLock) {
            if (notStartedActivity(activity, false)) {
                TDLog.i(TAG, "onActivityPaused: the SDK was initialized after the onActivityStart of " + activity);
                mStartedActivityList.add(new WeakReference<>(activity));
                if (mStartedActivityList.size() == 1) {
                    trackAppStart(activity, mThinkingDataInstance.getAutoTrackStartTime());
                }
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                if (notStartedActivity(activity, true)) {
                    TDLog.i(TAG, "onActivityStopped: the SDK might be initialized after the onActivityStart of " + activity);
                    return;
                }
                if (mStartedActivityList.size() == 0) {
                    try {
                        mThinkingDataInstance.appEnterBackground();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (isMainProcess(activity)) {
                        if (mThinkingDataInstance.isAutoTrackEnabled()) {
                            try {
                                if (!mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END)) {
                                    JSONObject properties = new JSONObject();
                                    TDUtils.getScreenNameAndTitleFromActivity(properties, activity);
                                    mThinkingDataInstance.autoTrack(TDConstants.APP_END_EVENT_NAME, properties);
                                }
                            } catch (Exception e) {
                                TDLog.i(TAG, e);
                            }
                        }
                    }
                    try {
                        mThinkingDataInstance.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }


    private String getCurrentProcessName(Context context) {

        try {
            int pid = android.os.Process.myPid();

            ActivityManager activityManager = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);


            if (activityManager == null) {
                return null;
            }

            List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList = activityManager.getRunningAppProcesses();
            if (runningAppProcessInfoList != null) {
                for (ActivityManager.RunningAppProcessInfo appProcess : runningAppProcessInfoList) {

                    if (appProcess != null) {
                        if (appProcess.pid == pid) {
                            return appProcess.processName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private boolean isMainProcess(Context context) {
        if (TextUtils.isEmpty(mMainProcessName)) {
            return true;
        }

        String currentProcess = getCurrentProcessName(context.getApplicationContext());
        if (TextUtils.isEmpty(currentProcess) || mMainProcessName.equals(currentProcess)) {
            return true;
        }

        return false;
    }
}
