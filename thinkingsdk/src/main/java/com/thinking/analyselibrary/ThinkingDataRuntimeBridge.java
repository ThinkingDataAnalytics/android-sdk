package com.thinking.analyselibrary;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;

public class ThinkingDataRuntimeBridge {
    private final static String TAG = "ThinkingDataRuntimeBridge";

    public static void onFragmentOnResumeMethod(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            ThinkingDataIgnoreTrackAppViewScreen trackEvent = method.getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            Activity activity = targetFragment.getActivity();

            String methodDeclaringClass = targetMethod.getDeclaringClass().getName();

            if (targetMethod.getDeclaringClass().getAnnotation(ThinkingDataTrackFragmentAppViewScreen.class) == null) {
                return;
            }

            if (!"android.support.v4.app.Fragment".equals(methodDeclaringClass)) {
                if (!targetFragment.isHidden() && targetFragment.getUserVisibleHint()) {
                    trackFragmentViewScreen(targetFragment, fragmentName, activity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onFragmentSetUserVisibleHintMethod(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            ThinkingDataIgnoreTrackAppViewScreen trackEvent = method.getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            if (targetMethod.getDeclaringClass().getAnnotation(ThinkingDataTrackFragmentAppViewScreen.class) == null) {
                return;
            }

            Activity activity = targetFragment.getActivity();
            boolean isVisibleHint = (boolean) joinPoint.getArgs()[0];

            if (isVisibleHint) {
                if (targetFragment.isResumed()) {
                    if (!targetFragment.isHidden()) {
                        trackFragmentViewScreen(targetFragment, fragmentName, activity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onFragmentHiddenChangedMethod(JoinPoint joinPoint) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();
            String fragmentName = joinPoint.getTarget().getClass().getName();

            Method method = methodSignature.getMethod();
            ThinkingDataIgnoreTrackAppViewScreen trackEvent = method.getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class);
            if (trackEvent != null) {
                return;
            }

            android.support.v4.app.Fragment targetFragment = (android.support.v4.app.Fragment) joinPoint.getTarget();

            if (targetFragment.getClass().getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class) != null) {
                return;
            }

            if (targetMethod.getDeclaringClass().getAnnotation(ThinkingDataTrackFragmentAppViewScreen.class) == null) {
                return;
            }

            Activity activity = targetFragment.getActivity();
            boolean hidden = (boolean) joinPoint.getArgs()[0];

            if (!hidden) {
                if (targetFragment.isResumed()) {
                    if (targetFragment.getUserVisibleHint()) {
                        trackFragmentViewScreen(targetFragment, fragmentName, activity);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackFragmentView(JoinPoint joinPoint, Object result) {
        try {
            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            Method targetMethod = methodSignature.getMethod();

            if (targetMethod == null) {
                return;
            }

            String fragmentName = joinPoint.getTarget().getClass().getName();

            if (result instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) result);
            } else if (result instanceof View) {
                View view = (View) result;
                view.setTag(R.id.thinking_analytics_tag_view_fragment_name, fragmentName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void trackFragmentViewScreen(android.support.v4.app.Fragment targetFragment, String fragmentName, Activity activity) {
        try {
            if (targetFragment == null) {
                return;
            }

            if (!ThinkingAnalyticsSDK.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragmentName)) {
                return;
            }

            JSONObject properties = new JSONObject();
            if (activity != null) {
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
                properties.put(AopConstants.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName));
            } else {
                properties.put(AopConstants.SCREEN_NAME, fragmentName);
            }

            if (targetFragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) targetFragment;

                String screenUrl = screenAutoTracker.getScreenUrl();
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    TDUtil.mergeJSONObject(otherProperties, properties);
                }

                ThinkingAnalyticsSDK.sharedInstance().trackViewScreenNei(screenUrl, properties);
            } else {
                ThinkingDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = targetFragment.getClass().getAnnotation(ThinkingDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    String screenUrl = autoTrackAppViewScreenUrl.url();
                    if (TextUtils.isEmpty(screenUrl)) {
                        screenUrl = fragmentName;
                    }
                    ThinkingAnalyticsSDK.sharedInstance().trackViewScreenNei(screenUrl, properties);
                } else {
                    ThinkingAnalyticsSDK.sharedInstance().autotrack("ta_app_view", properties);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName)) {
                return;
            }

            if (root == null) {
                return;
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                if (child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup) {
                    child.setTag(R.id.thinking_analytics_tag_view_fragment_name, fragmentName);
                } else if (child instanceof ViewGroup) {
                    traverseView(fragmentName, (ViewGroup) child);
                } else {
                    child.setTag(R.id.thinking_analytics_tag_view_fragment_name, fragmentName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onAdapterViewItemClick(JoinPoint joinPoint) {
        TDAdapterViewOnItemClickListenerAppClick.onAppClick(joinPoint);
    }

    public static void onCheckBoxCheckedChanged(JoinPoint joinPoint) {
        TDCheckBoxOnCheckedChangedAppClick.onAppClick(joinPoint);
    }

    public static void onMultiChoiceClick(JoinPoint joinPoint) {
        TDDialogOnClickAppClick.onMultiChoiceAppClick(joinPoint);
    }

    public static void onDialogClick(JoinPoint joinPoint) {
        TDDialogOnClickAppClick.onAppClick(joinPoint);
    }

    public static void onExpandableListViewItemGroupClick(JoinPoint joinPoint) {
        TDExpandableListViewItemChildAppClick.onItemGroupClick(joinPoint);
    }

    public static void onExpandableListViewItemChildClick(JoinPoint joinPoint) {
        TDExpandableListViewItemChildAppClick.onItemChildClick(joinPoint);
    }

    public static void onMenuClick(JoinPoint joinPoint, int menuItemIndex) {
        TDMenuItemAppClick.onAppClick(joinPoint, menuItemIndex);
    }

    public static void onMenuClick(JoinPoint joinPoint, Integer menuItemIndex) {
        TDMenuItemAppClick.onAppClick(joinPoint, menuItemIndex);
    }

    public static void onRadioGroupCheckedChanged(JoinPoint joinPoint) {
        TDRadioGroupOnCheckedAppClick.onAppClick(joinPoint);
    }

    public static void onRatingBarChanged(JoinPoint joinPoint) {
        TDRatingBarOnRatingChangedAppClick.onAppClick(joinPoint);
    }

    public static void onSeekBarChange(JoinPoint joinPoint) {
        TDSeekBarOnSeekBarChangeAppClick.onAppClick(joinPoint);
    }

    public static void onSpinnerItemSelected(JoinPoint joinPoint) {
        TDSpinnerOnItemSelectedAppClick.onAppClick(joinPoint);
    }

    public static void onTabHostChanged(JoinPoint joinPoint) {
        TDTabHostOnTabChangedAppClick.onAppClick(joinPoint);
    }

    public static void trackEventAOP(JoinPoint joinPoint) {
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

            Method method = methodSignature.getMethod();
            ThinkingDataTrackEvent trackEvent = method.getAnnotation(ThinkingDataTrackEvent.class);
            String eventName = trackEvent.eventName();
            if (TextUtils.isEmpty(eventName)) {
                return;
            }

            String pString = trackEvent.properties();
            JSONObject properties = new JSONObject();
            if (!TextUtils.isEmpty(pString)) {
                properties = new JSONObject(pString);
            }

            ThinkingAnalyticsSDK.sharedInstance().autotrack(eventName, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLog.i(TAG, "trackEventAOP error: " + e.getMessage());
        }
    }

    public static void trackViewOnClick(JoinPoint joinPoint) {
        TDTrackViewOnAppClick.onAppClick(joinPoint);
    }

    public static void onButterknifeClick(JoinPoint joinPoint) {
        try {
            if (ThinkingAnalyticsSDK.sharedInstance().isButterknifeOnClickEnabled()) {
                onViewOnClick(joinPoint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onViewOnClick(JoinPoint joinPoint) {
        TDViewOnClickAppClick.onAppClick(joinPoint);
    }
}