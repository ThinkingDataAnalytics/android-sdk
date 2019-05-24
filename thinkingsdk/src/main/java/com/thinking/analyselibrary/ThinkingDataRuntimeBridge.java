package com.thinking.analyselibrary;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;

public class ThinkingDataRuntimeBridge {
    private final static String TAG = "ThinkingDataRuntimeBridge";


    private static boolean fragmentGetTDAnnotation(JoinPoint joinPoint) {
       try {
           Signature signature = joinPoint.getSignature();
           MethodSignature methodSignature = (MethodSignature) signature;
           Method targetMethod = methodSignature.getMethod();
           if (null != targetMethod.getDeclaringClass().getAnnotation(ThinkingDataTrackFragmentAppViewScreen.class)) {
               return true;
           }
       } catch (Exception e) {
           e.printStackTrace();
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

    private static boolean fragmentIsHidden(Object fragment) {
        try {
            Method isHiddenMethod = fragment.getClass().getMethod("isHidden");
            if (isHiddenMethod != null) {
                return (boolean) isHiddenMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    public static void onFragmentOnResumeMethod(JoinPoint joinPoint) {
        try {
            if (!fragmentGetTDAnnotation(joinPoint)) {
                return;
            }

            Object fragment = joinPoint.getTarget();
            try {
                Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
                if (getParentFragmentMethod != null) {
                    Object parentFragment = getParentFragmentMethod.invoke(fragment);
                    if (parentFragment == null) {
                        if (!fragmentIsHidden(fragment) && fragmentGetUserVisibleHint(fragment)) {
                            trackFragmentViewScreen(fragment);
                        }
                    } else {
                        if (!fragmentIsHidden(fragment) && fragmentGetUserVisibleHint(fragment) &&
                                !fragmentIsHidden(parentFragment) && fragmentGetUserVisibleHint(parentFragment)) {
                            trackFragmentViewScreen(fragment);
                        }
                    }
                }
            } catch (Exception e) {
                //ignored
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public static void onFragmentSetUserVisibleHintMethod(JoinPoint joinPoint) {
        try {
            if (!fragmentGetTDAnnotation(joinPoint)) {return;}

            Object fragment = joinPoint.getTarget();
            Object parentFragment = null;
            try {
                Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
                if (getParentFragmentMethod != null) {
                    parentFragment = getParentFragmentMethod.invoke(fragment);
                }
            } catch (Exception e) {
                //ignored
            }

            boolean isVisibleHint = (boolean) joinPoint.getArgs()[0];
            if (isVisibleHint) {
                if (null == parentFragment && fragmentIsResumed(fragment) &&
                        !fragmentIsHidden(fragment)) {
                        trackFragmentViewScreen(fragment);
                } else if (fragmentIsResumed(fragment) && !fragmentIsHidden(fragment) &&
                        fragmentGetUserVisibleHint(fragment)) {
                    trackFragmentViewScreen(fragment);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onFragmentHiddenChangedMethod(JoinPoint joinPoint) {
        try {
            if (!fragmentGetTDAnnotation(joinPoint)) {return;}
            Object fragment = joinPoint.getTarget();
            Object parentFragment = null;
            try {
                Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
                if (getParentFragmentMethod != null) {
                    parentFragment = getParentFragmentMethod.invoke(fragment);
                }
            } catch (Exception e) {
                //ignored
            }

            boolean hidden = (boolean) joinPoint.getArgs()[0];
            if (!hidden) {
                if (null == parentFragment && fragmentIsResumed(fragment) &&
                        !fragmentIsHidden(fragment)) {
                    trackFragmentViewScreen(fragment);
                } else if (fragmentIsResumed(fragment) && !fragmentIsHidden(fragment) &&
                        fragmentGetUserVisibleHint(fragment)) {
                    trackFragmentViewScreen(fragment);
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

    private static boolean isFragment(Object object) {
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
                return false;
            }

            if ((supportFragmentClass != null && supportFragmentClass.isInstance(object)) ||
                    (androidXFragmentClass != null && androidXFragmentClass.isInstance(object))) {
                return true;
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    private static void trackFragmentViewScreen(Object fragment) {
        try {
            if (!ThinkingAnalyticsSDK.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if (!isFragment(fragment)) {return;}

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragment.getClass().getCanonicalName())) {
                return;
            }

            if (fragment.getClass().getAnnotation(ThinkingDataIgnoreTrackAppViewScreen.class) != null) {
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


            String fragmentTitle = AopUtil.getTitleFromFragment(fragment);
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

                ThinkingAnalyticsSDK.sharedInstance().trackViewScreenInternal(screenUrl, properties, false);
            } else {
                ThinkingDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = fragment.getClass().getAnnotation(ThinkingDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    String screenUrl = autoTrackAppViewScreenUrl.url();
                    if (TextUtils.isEmpty(screenUrl)) {
                        screenUrl = fragmentName;
                    }
                    ThinkingAnalyticsSDK.sharedInstance().trackViewScreenInternal(screenUrl, properties, false);
                } else {
                    ThinkingAnalyticsSDK.sharedInstance().autoTrack("ta_app_view", properties);
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

            ThinkingAnalyticsSDK.sharedInstance().autoTrack(eventName, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLog.i(TAG, "trackEventAOP error: " + e.getMessage());
        }
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