package com.thinking.analyselibrary;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.PropertyUtils;
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
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
                            if (!(view instanceof SeekBar) && (currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
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
                                    Method getPageTitleMethod = viewPagerAdapter.getClass().getMethod("getPageTitle", int.class);
                                    if (getPageTitleMethod != null) {
                                        viewText = (String) getPageTitleMethod.invoke(viewPagerAdapter, new Object[]{currentItem});
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (view instanceof Switch) {
                        viewType = "SwitchButton";
                        Switch switchView = (Switch) view;
                        if(switchView.isChecked()) {
                            viewText = switchView.getTextOn();
                        } else {
                            viewText = switchView.getTextOff();
                        }

                        if (TextUtils.isEmpty(viewText)) {
                            viewText = switchView.getText();
                        }

                    } else if (view instanceof  RadioGroup) {

                        viewType = "RadioGroup";
                        RadioGroup radioGroup = (RadioGroup) view;

                        //获取变更后的选中项的ID
                        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                        if (activity != null) {
                            try {
                                RadioButton radioButton = activity.findViewById(checkedRadioButtonId);
                                if (radioButton != null) {
                                    if (!TextUtils.isEmpty(radioButton.getText())) {
                                        viewText = radioButton.getText().toString();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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
                            properties.put(AopConstants.ELEMENT_POSITION, ((Spinner) view).getSelectedItemPosition());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (view instanceof TimePicker) {
                        viewType = "TimePicker";
                        viewText = ((TimePicker) view).getCurrentHour() + ":" + ((TimePicker) view).getCurrentMinute();
                    } else if (view instanceof DatePicker) {
                        viewType = "DatePicker";
                        DatePicker datePicker = (DatePicker) view;
                        viewText = datePicker.getYear() +
                                "-" +
                                datePicker.getMonth() +
                                "-" +
                                datePicker.getDayOfMonth();

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

    public static void onExpandableListViewOnGroupClick(final View expandableListView, final View view, final int groupPosition) {
        onExpandableListViewOnChildClick(expandableListView, view, groupPosition, -1);
    }

    public static void onExpandableListViewOnChildClick(final View expandableListView, final View view, final int groupPosition, final int childPosition) {
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

    public static void onDialogClick(final Object dialogInterface, final int which) {

        if (!(dialogInterface instanceof Dialog)) return;
        final Dialog dialog = (Dialog) dialogInterface;

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

                    Context context = dialog.getContext();
                    Activity activity = AopUtil.getActivityFromContext(context);

                    if (activity == null) {
                        activity = dialog.getOwnerActivity();
                    }

                    if (activity != null) {
                        if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    if (AopUtil.isViewIgnored(instance, Dialog.class)) {
                        return;
                    }

                    JSONObject properties = new JSONObject();

                    try {
                        if (dialog.getWindow() != null) {
                            String idString = (String) AopUtil.getTag(instance.getToken(), dialog.getWindow().getDecorView(),
                                    R.id.thinking_analytics_tag_view_id);
                            if (!TextUtils.isEmpty(idString)) {
                                properties.put(AopConstants.ELEMENT_ID, idString);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    properties.put(AopConstants.ELEMENT_TYPE, "Dialog");

                    Class<?> alertDialogClass = null;
                    try {
                        alertDialogClass = Class.forName("android.support.v7.app.AlertDialog)");
                    } catch (Exception e ) {
                        // ignore
                    }
                    if (null == alertDialogClass) {
                        try {
                            alertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    if (dialog instanceof android.app.AlertDialog) {
                        android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                        Button button = alertDialog.getButton(which);
                        if (button != null) {
                            if (!TextUtils.isEmpty(button.getText())) {
                                properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                            }
                        } else {
                            ListView listView = alertDialog.getListView();
                            if (listView != null) {
                                ListAdapter listAdapter = listView.getAdapter();
                                Object object = listAdapter.getItem(which);
                                if (object != null) {
                                    if (object instanceof String) {
                                        properties.put(AopConstants.ELEMENT_CONTENT, object);
                                    }
                                }
                            }
                        }

                    } else if (null != alertDialogClass && alertDialogClass.isInstance(dialog)) {
                        Button button = null;
                        try {
                            Method getButtonMethod = dialog.getClass().getMethod("getButton", int.class);
                            if (getButtonMethod != null) {
                                button = (Button) getButtonMethod.invoke(dialog, which);
                            }
                        } catch (Exception e) {
                            //ignored
                        }

                        if (button != null) {
                            if (!TextUtils.isEmpty(button.getText())) {
                                properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                            }
                        } else {
                            try {
                                Method getListViewMethod = dialog.getClass().getMethod("getListView");
                                if (getListViewMethod != null) {
                                    ListView listView = (ListView) getListViewMethod.invoke(dialog);
                                    if (listView != null) {
                                        ListAdapter listAdapter = listView.getAdapter();
                                        Object object = listAdapter.getItem(which);
                                        if (object != null) {
                                            if (object instanceof String) {
                                                properties.put(AopConstants.ELEMENT_CONTENT, object);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                //ignored
                            }
                        }
                    }

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    TDLog.i(TAG, " DialogInterface.OnClickListener.onClick AOP ERROR: " + e.getMessage());
                }

            }
        });
    }

    public static void onAdapterViewItemClick(final View adapterView, final View view, final int position) {
        if (null == adapterView || null == view) return;
        if (!(adapterView instanceof AdapterView<?>)) return;
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

                    Context context = view.getContext();
                    if (context == null) {
                        return;
                    }

                    Activity activity = AopUtil.getActivityFromContext(context);
                    if (activity != null) {
                        if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    if (AopUtil.isViewIgnored(instance, adapterView.getClass())) {
                        return;
                    }

                    JSONObject properties = new JSONObject();

                    List<Class> mIgnoredViewTypeList = instance.getIgnoredViewTypeList();
                    if (mIgnoredViewTypeList != null) {
                        if (adapterView instanceof ListView) {
                            properties.put(AopConstants.ELEMENT_TYPE, "ListView");
                            if (AopUtil.isViewIgnored(instance, ListView.class)) {
                                return;
                            }
                        } else if (adapterView instanceof GridView) {
                            properties.put(AopConstants.ELEMENT_TYPE, "GridView");
                            if (AopUtil.isViewIgnored(instance, GridView.class)) {
                                return;
                            }
                        } else if (adapterView instanceof Spinner) {
                            properties.put(AopConstants.ELEMENT_TYPE, "Spinner");
                            if (AopUtil.isViewIgnored(instance, Spinner.class)) {
                                return;
                            }
                        }
                    }

                    Adapter adapter = ((AdapterView)adapterView).getAdapter();
                    if (adapter instanceof ThinkingAdapterViewItemTrackProperties) {
                        try {
                            ThinkingAdapterViewItemTrackProperties objectProperties = (ThinkingAdapterViewItemTrackProperties) adapter;
                            JSONObject jsonObject = objectProperties.getThinkingItemTrackProperties(position);
                            if (jsonObject != null && PropertyUtils.checkProperty(jsonObject)) {
                                TDUtil.mergeJSONObject(jsonObject, properties);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    AopUtil.addViewPathProperties(activity, view, properties);

                    String idString = AopUtil.getViewId(adapterView, instance.getToken());
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

                    properties.put(AopConstants.ELEMENT_POSITION, String.valueOf(position));

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
                        viewText = ((TextView) view).getText().toString();
                    }

                    if (!TextUtils.isEmpty(viewText)) {
                        properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                    }

                    AopUtil.getFragmentNameFromView(adapterView, properties);

                    JSONObject p = (JSONObject) AopUtil.getTag(instance.getToken(), view,
                            R.id.thinking_analytics_tag_view_properties);
                    if (p != null) {
                        TDUtil.mergeJSONObject(p, properties);
                    }

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    TDLog.i(TAG, " AdapterView.OnItemClickListener.onItemClick AOP ERROR: " + e.getMessage());
                }

            }
        });
    }

    public static  void onMenuItemSelected(final Object object, final MenuItem menuItem) {
        if (null == menuItem) return;
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

                    if (AopUtil.isViewIgnored(instance, MenuItem.class)) {
                        return;
                    }

                    if (object == null) {
                        return;
                    }

                    Context context = null;
                    if (object instanceof Context) {
                        context = (Context) object;
                    }
                    if (context == null) {
                        return;
                    }

                    Activity activity = AopUtil.getActivityFromContext(context);
                    if (activity != null) {
                        if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    String idString = null;
                    try {
                        idString = context.getResources().getResourceEntryName(menuItem.getItemId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    JSONObject properties = new JSONObject();
                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AopConstants.ELEMENT_ID, idString);
                    }

                    if (!TextUtils.isEmpty(menuItem.getTitle())) {
                        properties.put(AopConstants.ELEMENT_CONTENT, menuItem.getTitle());
                    }

                    properties.put(AopConstants.ELEMENT_TYPE, "MenuItem");

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    TDLog.i(TAG, " error: " + e.getMessage());
                }

            }
        });

    }

    public static void onTabHostChanged(final String tabName) {
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

                    if (AopUtil.isViewIgnored(instance, TabHost.class)) {
                        return;
                    }

                    JSONObject properties = new JSONObject();

                    properties.put(AopConstants.ELEMENT_CONTENT, tabName);
                    properties.put(AopConstants.ELEMENT_TYPE, "TabHost");

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    TDLog.i(TAG, " onTabChanged AOP ERROR: " + e.getMessage());
                }

            }
        });
    }
}