package com.thinking.analyselibrary;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.ToggleButton;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;


public class TDCheckBoxOnCheckedChangedAppClick {
    private final static String TAG = "TDCheckBoxOnCheckedChangedAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 2) {
                return;
            }

            View view = (View) joinPoint.getArgs()[0];
            if (view == null) {
                return;
            }

            Context context = view.getContext();
            if (context == null) {
                return;
            }

            Activity activity = AopUtil.getActivityFromContext(context, view);
            if (activity != null) {
                if (ThinkingAnalyticsSDK.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            boolean isChecked = (boolean) joinPoint.getArgs()[1];

            JSONObject properties = new JSONObject();

            AopUtil.addViewPathProperties(activity, view, properties);

            String idString = AopUtil.getViewId(view);
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

            String viewText = null;
            if (view instanceof CheckBox) { // CheckBox
                properties.put(AopConstants.ELEMENT_TYPE, "CheckBox");
                CompoundButton compoundButton = (CompoundButton) view;
                if (!TextUtils.isEmpty(compoundButton.getText())) {
                    viewText = compoundButton.getText().toString();
                }
            } else if (view instanceof SwitchCompat) {
                properties.put(AopConstants.ELEMENT_TYPE, "SwitchCompat");
                SwitchCompat switchCompat = (SwitchCompat) view;
                if (!TextUtils.isEmpty(switchCompat.getTextOn())) {
                    viewText = switchCompat.getTextOn().toString();
                }
            } else if (view instanceof ToggleButton) { // ToggleButton
                properties.put(AopConstants.ELEMENT_TYPE, "ToggleButton");
                ToggleButton toggleButton = (ToggleButton) view;
                if (isChecked) {
                    if (!TextUtils.isEmpty(toggleButton.getTextOn())) {
                        viewText = toggleButton.getTextOn().toString();
                    }
                } else {
                    if (!TextUtils.isEmpty(toggleButton.getTextOff())) {
                        viewText = toggleButton.getTextOff().toString();
                    }
                }
            } else if (view instanceof RadioButton) { // RadioButton
                properties.put(AopConstants.ELEMENT_TYPE, "RadioButton");
                RadioButton radioButton = (RadioButton) view;
                if (!TextUtils.isEmpty(radioButton.getText())) {
                    viewText = radioButton.getText().toString();
                }
            } else {
                properties.put(AopConstants.ELEMENT_TYPE, view.getClass().getCanonicalName());
            }

            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            AopUtil.getFragmentNameFromView(view, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDK.sharedInstance().autotrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLog.i(TAG, " onCheckedChanged AOP ERROR: " + e.getMessage());
        }
    }
}
