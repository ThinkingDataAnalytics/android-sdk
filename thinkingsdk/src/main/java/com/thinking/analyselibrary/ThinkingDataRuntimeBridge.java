package com.thinking.analyselibrary;

import android.app.Activity;
import android.content.Context;
import android.text.Annotation;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.PropertyUtils;
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
    public static void onFragmentCreateView(Object fragment, View rootView) {
        try {
            if (isNotFragment(fragment)) {
                return;
            }

            String fragmentName = fragment.getClass().getName();
            rootView.setTag(R.id.thinking_analytics_tag_view_fragment_name, fragmentName);

            if (rootView instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) rootView);
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

    public static void onFragmentHiddenChanged(Object fragment, Boolean hidden) {
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

    public static void onFragmentSetUserVisibleHint(Object fragment, Boolean isVisibleHint) {
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

    public static void onViewOnClick(final View view, final Object annotation) {
        if (null == view) return;

        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK instance) {
                try {
                    if (!instance.isAutoTrackEnabled()) {
                        return;
                    }

                    if (instance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                        return;
                    }


                    if (null != annotation) {
                        if (annotation instanceof ThinkingDataIgnoreTrackOnClick) {
                            ThinkingDataIgnoreTrackOnClick ignoreTrackOnClick = (ThinkingDataIgnoreTrackOnClick) annotation;
                            if (TextUtils.isEmpty(ignoreTrackOnClick.appId()) || instance.getToken().equals(ignoreTrackOnClick.appId())) {
                                return;
                            }
                        } else if (annotation instanceof  ThinkingDataTrackViewOnClick) {
                            ThinkingDataTrackViewOnClick trackViewOnClick = (ThinkingDataTrackViewOnClick) annotation;
                            if (!(TextUtils.isEmpty(trackViewOnClick.appId()) || instance.getToken().equals(trackViewOnClick.appId()))) {
                                return;
                            }
                        }
                    }

                    long currentOnClickTimestamp = System.currentTimeMillis();
                    String tag = (String) AopUtil.getTag(instance.getToken(), view, R.id.thinking_analytics_tag_view_onclick_timestamp);
                    if (!TextUtils.isEmpty(tag)) {
                        try {
                            long lastOnClickTimestamp = Long.parseLong(tag);
                            if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                                TDLog.i(TAG, "This onClick maybe extends from super, IGNORE");
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    AopUtil.setTag(instance.getToken(), view, R.id.thinking_analytics_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));

                    Context context = view.getContext();
                    Activity activity = AopUtil.getActivityFromContext(context);
                    if (activity != null) {
                        if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    if (AopUtil.isViewIgnored(instance, view)) {
                        return;
                    }

                    JSONObject properties = new JSONObject();
                    AopUtil.addViewPathProperties(activity, view, properties);

                    String idString = AopUtil.getViewId(view, instance.getToken());
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AopConstants.ELEMENT_ID, idString);
                    }

                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    Class<?> switchCompatClass = null;
                    try {
                        switchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
                    } catch (Exception e) {
                        //ignored
                    }
                    if (switchCompatClass == null) {
                        try {
                            switchCompatClass = Class.forName("androidx.appcompat.widget.SwitchCompat");
                        } catch (Exception e) {
                            //ignored
                        }
                    }

                    Class<?> viewPagerClass = null;
                    try {
                        viewPagerClass = Class.forName("android.support.v4.view.ViewPager");
                    } catch (Exception e) {
                        //ignored
                    }
                    if (null == viewPagerClass) {
                        try {
                            viewPagerClass = Class.forName("androidx.viewpager.widget.ViewPager");
                        } catch (Exception e) {
                            //ignored
                        }
                    }

                    String viewType = view.getClass().getCanonicalName();
                    CharSequence viewText = null;
                    if (view instanceof CheckBox) {
                        viewType = "CheckBox";
                        CheckBox checkBox = (CheckBox) view;
                        viewText = checkBox.getText();
                    } else if (switchCompatClass != null && switchCompatClass.isInstance(view)) {
                        viewType = "SwitchCompat";
                        CompoundButton switchCompat = (CompoundButton) view;

                        Method getTextMethod;
                        if (switchCompat.isChecked()) {
                            getTextMethod = view.getClass().getMethod("getTextOn");
                        } else {
                            getTextMethod = view.getClass().getMethod("getTextOff");
                        }
                        viewText = (String) getTextMethod.invoke(view);
                    } else if (viewPagerClass != null && viewPagerClass.isInstance(view)) {
                        viewType = "ViewPager";
                        try {
                            Method getAdapterMethod = view.getClass().getMethod("getAdapter");
                            if (getAdapterMethod != null) {
                                Object viewPagerAdapter = getAdapterMethod.invoke(view);
                                Method getCurrentItemMethod = view.getClass().getMethod("getCurrentItem");
                                if (getCurrentItemMethod != null) {
                                    int currentItem = (int) getCurrentItemMethod.invoke(view);
                                    properties.put(AopConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d", currentItem));
                                    Method getPageTitleMethod = viewPagerAdapter.getClass().getMethod("getPageTitle", new Class[]{int.class});
                                    if (getPageTitleMethod != null) {
                                        viewText = (String) getPageTitleMethod.invoke(viewPagerAdapter, new Object[]{currentItem});
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (view instanceof RadioButton) {
                        viewType = "RadioButton";
                        RadioButton radioButton = (RadioButton) view;
                        viewText = radioButton.getText();
                    } else if (view instanceof ToggleButton) {
                        viewType = "ToggleButton";
                        ToggleButton toggleButton = (ToggleButton) view;
                        boolean isChecked = toggleButton.isChecked();
                        if (isChecked) {
                            viewText = toggleButton.getTextOn();
                        } else {
                            viewText = toggleButton.getTextOff();
                        }
                    } else if (view instanceof Button) {
                        viewType = "Button";
                        Button button = (Button) view;
                        viewText = button.getText();
                    } else if (view instanceof CheckedTextView) {
                        viewType = "CheckedTextView";
                        CheckedTextView textView = (CheckedTextView) view;
                        viewText = textView.getText();
                    } else if (view instanceof TextView) {
                        viewType = "TextView";
                        TextView textView = (TextView) view;
                        viewText = textView.getText();
                    } else if (view instanceof ImageButton) {
                        viewType = "ImageButton";
                        ImageButton imageButton = (ImageButton) view;
                        if (!TextUtils.isEmpty(imageButton.getContentDescription())) {
                            viewText = imageButton.getContentDescription().toString();
                        }
                    } else if (view instanceof ImageView) {
                        viewType = "ImageView";
                        ImageView imageView = (ImageView) view;
                        if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                            viewText = imageView.getContentDescription().toString();
                        }
                    } else if (view instanceof RatingBar) {
                        viewType = "RatingBar";
                        RatingBar ratingBar = (RatingBar) view;
                        viewText = String.valueOf(ratingBar.getRating());
                    } else if (view instanceof SeekBar) {
                        viewType = "SeekBar";
                        SeekBar seekBar = (SeekBar) view;
                        viewText = String.valueOf(seekBar.getProgress());
                    } else if (view instanceof Spinner) {
                        viewType = "Spinner";
                        try {
                            StringBuilder stringBuilder = new StringBuilder();
                            viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                            if (!TextUtils.isEmpty(viewText)) {
                                viewText = viewText.toString().substring(0, viewText.length() - 1);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (view instanceof ViewGroup) {
                        try {
                            StringBuilder stringBuilder = new StringBuilder();
                            viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                            if (!TextUtils.isEmpty(viewText)) {
                                viewText = viewText.toString().substring(0, viewText.length() - 1);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (!TextUtils.isEmpty(viewText)) {
                        properties.put(AopConstants.ELEMENT_CONTENT, viewText.toString());
                    }

                    properties.put(AopConstants.ELEMENT_TYPE, viewType);
                    AopUtil.getFragmentNameFromView(view, properties);

                    JSONObject p = (JSONObject) AopUtil.getTag(instance.getToken(), view,
                            R.id.thinking_analytics_tag_view_properties);
                    if (p != null) {
                        TDUtil.mergeJSONObject(p, properties);
                    }

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    TDLog.e(TAG, "onViewClickMethod error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    public static void onExpandableListViewOnGroupClick(final View expandableListView, final View view, final Integer groupPosition) {
        onExpandableListViewOnChildClick(expandableListView, view, groupPosition, -1);
    }

    public static void onExpandableListViewOnChildClick(final View expandableListView, final View view, final Integer groupPosition, final Integer childPosition) {
        if (null == expandableListView) return;
        final Context context = expandableListView.getContext();
        if (null == context) return;

        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK instance) {
                try {
                    if (!instance.isAutoTrackEnabled()) {
                        return;
                    }

                    if (instance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                        return;
                    }

                    Activity activity = AopUtil.getActivityFromContext(context);
                    if (activity != null) {
                        if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    if (AopUtil.isViewIgnored(instance, ExpandableListView.class)) {
                        return;
                    }

                    if (AopUtil.isViewIgnored(instance, expandableListView)) {
                        return;
                    }

                    if (AopUtil.isViewIgnored(instance, view)) {
                        return;
                    }

                    JSONObject properties = new JSONObject();

                    AopUtil.addViewPathProperties(activity, view, properties);

                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    String idString = AopUtil.getViewId(expandableListView);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AopConstants.ELEMENT_ID, idString);
                    }

                    if (childPosition < 0) {
                        properties.put(AopConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d", groupPosition));
                    } else {
                        properties.put(AopConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));
                    }
                    properties.put(AopConstants.ELEMENT_TYPE, "ExpandableListView");

                    String viewText = null;
                    if (view instanceof ViewGroup) {
                        try {
                            StringBuilder stringBuilder = new StringBuilder();
                            viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                            if (!TextUtils.isEmpty(viewText)) {
                                viewText = viewText.substring(0, viewText.length() - 1);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (view instanceof TextView) {
                        viewText = (String) ((TextView) view).getText();
                    }

                    //element_content
                    if (!TextUtils.isEmpty(viewText)) {
                        properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                    }

                    AopUtil.getFragmentNameFromView(expandableListView, properties);

                    JSONObject p = (JSONObject) AopUtil.getTag(instance.getToken(), view,
                            R.id.thinking_analytics_tag_view_properties);
                    if (p != null) {
                        TDUtil.mergeJSONObject(p, properties);
                    }


                    ExpandableListAdapter listAdapter = ((ExpandableListView)expandableListView).getExpandableListAdapter();
                    if (listAdapter != null) {
                        if (listAdapter instanceof ThinkingExpandableListViewItemTrackProperties) {
                            try {
                                ThinkingExpandableListViewItemTrackProperties trackProperties = (ThinkingExpandableListViewItemTrackProperties) listAdapter;
                                JSONObject jsonObject = null;
                                if (childPosition < 0) {
                                    jsonObject = trackProperties.getThinkingGroupItemTrackProperties(groupPosition);
                                } else {
                                    jsonObject = trackProperties.getThinkingChildItemTrackProperties(groupPosition, childPosition);
                                }
                                if (jsonObject != null && PropertyUtils.checkProperty(jsonObject)) {
                                    TDUtil.mergeJSONObject(jsonObject, properties);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    TDLog.i(TAG, " ExpandableListView.OnChildClickListener.onGroupClick AOP ERROR: " + e.getMessage());
                }
            }
        });
    }

    public static void onDialogClick(JoinPoint joinPoint) {
        TDDialogOnClickAppClick.onAppClick(joinPoint);
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

    public static void onViewOnClick(final JoinPoint joinPoint) {
        TDViewOnClickAppClick.onAppClick(joinPoint);
    }

}