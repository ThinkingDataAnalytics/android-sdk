/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.aop.push;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import cn.thinkingdata.android.utils.TDLog;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/5/31
 * @since
 */
public class TAPushLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ThinkingAnalytics.TAPushLifecycle";

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        TAPushProcess.getInstance().onNotificationClick(activity,activity.getIntent());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        TAPushProcess.getInstance().onNotificationClick(activity,activity.getIntent());
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

}
