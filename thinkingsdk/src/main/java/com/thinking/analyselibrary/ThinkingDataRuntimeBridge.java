package com.thinking.analyselibrary;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;

public class ThinkingDataRuntimeBridge {
    private final static String TAG = "ThinkingDataRuntimeBridge";

    // Called when onCreateView is executed.
    public static void onFragmentCreateView(Object fragment, Object result) {
        try {
            if (isNotFragment(fragment)) {
                return;
            }

            String fragmentName = fragment.getClass().getName();
            if (result instanceof View) {
                ((View) result).setTag(R.id.thinking_analytics_tag_view_fragment_name, fragmentName);
            }

            if (result instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void onFragmentOnResume(Object fragment) {
        if (isNotFragment(fragment)) {
            return;
        }

        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                parentFragment = getParentFragmentMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }

        if (parentFragment == null) {
            if (fragmentIsNotHidden(fragment) && fragmentGetUserVisibleHint(fragment)) {
                trackFragmentViewScreen(fragment);
            }
        } else {
            if (fragmentIsNotHidden(fragment) && fragmentGetUserVisibleHint(fragment) &&
                    fragmentIsNotHidden(parentFragment) && fragmentGetUserVisibleHint(parentFragment)) {
                trackFragmentViewScreen(fragment);
            }
        }
    }

    public static void onFragmentHiddenChanged(Object fragment, boolean hidden) {
        if (isNotFragment(fragment)) {
            return;
        }

        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                parentFragment = getParentFragmentMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }

        if (!hidden) {
            if (null == parentFragment && fragmentIsResumed(fragment) &&
                    fragmentIsNotHidden(fragment)) {
                trackFragmentViewScreen(fragment);
            } else if (fragmentIsResumed(fragment) && fragmentIsNotHidden(fragment) &&
                    fragmentGetUserVisibleHint(fragment)) {
                trackFragmentViewScreen(fragment);
            }
        }
    }

    public static void onFragmentSetUserVisibleHint(Object fragment, boolean isVisibleHint) {
        if (isNotFragment(fragment)) {
            return;
        }

        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                parentFragment = getParentFragmentMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }

        if (isVisibleHint) {
            if (null == parentFragment && fragmentIsResumed(fragment) &&
                    fragmentIsNotHidden(fragment)) {
                trackFragmentViewScreen(fragment);
            } else if (fragmentIsResumed(fragment) && fragmentIsNotHidden(fragment) &&
                    fragmentGetUserVisibleHint(fragment)) {
                trackFragmentViewScreen(fragment);
            }
        }

    }

    private static boolean isNotFragment(Object object) {
        try {
            Class<?> supportFragmentClass = null;
            Class<?> androidXFragmentClass = null;
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            if (supportFragmentClass == null && androidXFragmentClass == null) {
                return true;
            }

            if ((supportFragmentClass != null && supportFragmentClass.isInstance(object)) ||
                    (androidXFragmentClass != null && androidXFragmentClass.isInstance(object))) {
                return false;
            }
        } catch (Exception e) {
            //ignored
        }
        return true;
    }

    private static boolean fragmentIsResumed(Object fragment) {
        try {
            Method isResumedMethod = fragment.getClass().getMethod("isResumed");
            if (isResumedMethod != null) {
                return (boolean) isResumedMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    private static boolean fragmentGetUserVisibleHint(Object fragment) {
        try {
            Method getUserVisibleHintMethod = fragment.getClass().getMethod("getUserVisibleHint");
            if (getUserVisibleHintMethod != null) {
                return (boolean) getUserVisibleHintMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    private static boolean fragmentIsNotHidden(Object fragment) {
        try {
            Method isHiddenMethod = fragment.getClass().getMethod("isHidden");
            if (isHiddenMethod != null) {
                return !((boolean) isHiddenMethod.invoke(fragment));
            }
        } catch (Exception e) {
            //ignored
        }
        return true;
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
                child.setTag(R.id.thinking_analytics_tag_view_fragment_name, fragmentName);
                if (child instanceof ViewGroup) {
                    traverseView(fragmentName, (ViewGroup) child);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void trackFragmentViewScreen(final Object fragment) {
        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK instance) {
                if (!instance.isTrackFragmentAppViewScreenEnabled()) {
                    return;
                }

                ThinkingDataIgnoreTrackAppViewScreen thinkingDataIgnoreTrackAppViewScreen =
                        fragment.getClass().getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class);
                if (thinkingDataIgnoreTrackAppViewScreen != null &&
                        (TextUtils.isEmpty(thinkingDataIgnoreTrackAppViewScreen.appId()) ||
                                instance.getToken().equals(thinkingDataIgnoreTrackAppViewScreen.appId()))) {
                    return;
                }

                JSONObject properties = new JSONObject();

                String fragmentName = fragment.getClass().getCanonicalName();
                Activity activity = null;
                try {
                    Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                    if (getActivityMethod != null) {
                        activity = (Activity) getActivityMethod.invoke(fragment);
                    }
                } catch (Exception e) {
                    //ignored
                }


                try {
                    String fragmentTitle = AopUtil.getTitleFromFragment(fragment, instance.getToken());
                    if (!TextUtils.isEmpty(fragmentTitle)) {
                        properties.put(AopConstants.TITLE, fragmentTitle);
                    } else if (null != activity) {
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName));
                    } else {
                        properties.put(AopConstants.SCREEN_NAME, fragmentName);
                    }


                    if (fragment instanceof ScreenAutoTracker) {
                        ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                        String screenUrl = screenAutoTracker.getScreenUrl();
                        JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                        if (otherProperties != null) {
                            TDUtil.mergeJSONObject(otherProperties, properties);
                        }

                        instance.trackViewScreenInternal(screenUrl, properties, false);
                    } else {
                        ThinkingDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl =
                                fragment.getClass().getAnnotation(ThinkingDataAutoTrackAppViewScreenUrl.class);
                        if (autoTrackAppViewScreenUrl != null && (TextUtils.isEmpty(autoTrackAppViewScreenUrl.appId()) ||
                                instance.getToken().equals(autoTrackAppViewScreenUrl.appId()) )) {
                            String screenUrl = autoTrackAppViewScreenUrl.url();
                            if (TextUtils.isEmpty(screenUrl)) {
                                screenUrl = fragmentName;
                            }
                            instance.trackViewScreenInternal(screenUrl, properties, false);
                        } else {
                            instance.autoTrack("ta_app_view", properties);
                        }
                    }
                } catch (JSONException e) {
                    TDLog.d(TAG, "JSONException occurred when track fragment events");
                }
            }
        });
    }

    public static void trackEvent(final Object trackEvent) {
        if (!(trackEvent instanceof ThinkingDataTrackEvent)) {
            return;
        }

        final String eventName = ((ThinkingDataTrackEvent) trackEvent).eventName();
        final String propertiesString = ((ThinkingDataTrackEvent) trackEvent).properties();
        final String token = ((ThinkingDataTrackEvent) trackEvent).appId();
        if (TextUtils.isEmpty(eventName)) {
            return;
        }

        final JSONObject properties = new JSONObject();
        if (!TextUtils.isEmpty(propertiesString)) {
            try {
                TDUtil.mergeJSONObject(new JSONObject(propertiesString), properties);
            } catch (JSONException e) {
                TDLog.e(TAG, "Exception occurred in trackEvent");
                e.printStackTrace();
            }
        }

        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK instance) {
                if (instance.isAutoTrackEnabled()) {
                    if (TextUtils.isEmpty(token) || instance.getToken().equals(token)) {
                        instance.track(eventName, properties);
                    }
                }
            }
        });
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


    public static void trackViewOnClick(JoinPoint joinPoint) {
        onViewOnClick(joinPoint);
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