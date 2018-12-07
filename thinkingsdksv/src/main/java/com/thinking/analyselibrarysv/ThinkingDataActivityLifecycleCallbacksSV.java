package com.thinking.analyselibrarysv;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONObject;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class ThinkingDataActivityLifecycleCallbacksSV implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "TD.LifecycleCallbacks";
    private boolean resumeFromBackground = false;
    private Integer startedActivityCount = 0;
    private final Object mActivityLifecycleCallbacksLock = new Object();
    private final ThinkingAnalyticsSDKSV mThinkingDataInstance;
    private final String mMainProcessName;

    public ThinkingDataActivityLifecycleCallbacksSV(ThinkingAnalyticsSDKSV instance, String mainProcessName) {
        this.mThinkingDataInstance = instance;
        this.mMainProcessName = mainProcessName;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                if (startedActivityCount == 0) {

                    try {
                        mThinkingDataInstance.appBecomeActive();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (TDUtilSV.isMainProcess(activity, mMainProcessName)) {
                        if (mThinkingDataInstance.isAutoTrackEnabled()) {
                            try {
                                if (!mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_START)) {

                                    JSONObject properties = new JSONObject();
                                    properties.put("#resume_from_background", resumeFromBackground);
                                    TDUtilSV.getScreenNameAndTitleFromActivity(properties, activity);

                                    mThinkingDataInstance.autotrack("ta_app_start", properties);
                                }

                                if (!mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_END)) {
                                    mThinkingDataInstance.timeEvent("ta_app_end");
                                }
                            } catch (Exception e) {
                                TDLogSV.i(TAG, e);
                            }
                        }

                        resumeFromBackground = true;
                    }
                }

                startedActivityCount = startedActivityCount + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            boolean mShowAutoTrack = true;
            if (mThinkingDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())) {
                mShowAutoTrack = false;
            }

            if (mThinkingDataInstance.isAutoTrackEnabled() && mShowAutoTrack && !mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_VIEW_SCREEN)) {
                try {
                    JSONObject properties = new JSONObject();
                    properties.put("#screen_name", activity.getClass().getCanonicalName());
                    TDUtilSV.getScreenNameAndTitleFromActivity(properties, activity);

                    if (activity instanceof ScreenAutoTrackerSV) {
                        ScreenAutoTrackerSV screenAutoTracker = (ScreenAutoTrackerSV) activity;

                        String screenUrl = screenAutoTracker.getScreenUrl();
                        JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                        if (otherProperties != null) {
                            TDUtilSV.mergeJSONObject(otherProperties, properties);
                        }

                        mThinkingDataInstance.trackViewScreenNei(screenUrl, properties);
                    } else {
                        ThinkingDataAutoTrackAppViewScreenUrlSV autoTrackAppViewScreenUrl = activity.getClass().getAnnotation(ThinkingDataAutoTrackAppViewScreenUrlSV.class);
                        if (autoTrackAppViewScreenUrl != null) {
                            String screenUrl = autoTrackAppViewScreenUrl.url();
                            if (TextUtils.isEmpty(screenUrl)) {
                                screenUrl = activity.getClass().getCanonicalName();
                            }
                            mThinkingDataInstance.trackViewScreenNei(screenUrl, properties);
                        } else {
                            mThinkingDataInstance.autotrack("ta_app_view", properties);
                        }
                    }
                } catch (Exception e) {
                    TDLogSV.i(TAG, e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                startedActivityCount = startedActivityCount - 1;

                if (startedActivityCount == 0) {
                    try {
                        mThinkingDataInstance.appEnterBackground();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (TDUtilSV.isMainProcess(activity, mMainProcessName)) {
                        if (mThinkingDataInstance.isAutoTrackEnabled()) {
                            try {
                                if (!mThinkingDataInstance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_END)) {
                                    JSONObject properties = new JSONObject();
                                    TDUtilSV.getScreenNameAndTitleFromActivity(properties, activity);
                                    mThinkingDataInstance.clearLastScreenUrl();
                                    mThinkingDataInstance.autotrack("ta_app_end", properties);
                                }
                            } catch (Exception e) {
                                TDLogSV.i(TAG, e);
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

}
