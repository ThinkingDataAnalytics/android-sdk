package com.thinking.analyselibrarysv;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDTrackViewOnAppClickSV {
    private final static String TAG = "TDTrackViewOnAppClickSV";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 1) {
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

            Activity activity = AopUtilSV.getActivityFromContext(context, view);

            if (activity != null) {
                if (ThinkingAnalyticsSDKSV.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtilSV.isViewIgnored(view)) {
                return;
            }

            long currentOnClickTimestamp = System.currentTimeMillis();
            String tag = (String) view.getTag(R.id.thinking_analytics_sv_tag_view_onclick_timestamp);
            if (!TextUtils.isEmpty(tag)) {
                try {
                    long lastOnClickTimestamp = Long.parseLong(tag);
                    if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                        TDLogSV.i(TAG, "This onClick maybe extends from super, IGNORE");
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            view.setTag(R.id.thinking_analytics_sv_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));

            JSONObject properties = new JSONObject();

            AopUtilSV.addViewPathProperties(activity, view, properties);

            String idString = AopUtilSV.getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstantsSV.ELEMENT_ID, idString);
            }

            if (activity != null) {
                properties.put(AopConstantsSV.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtilSV.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstantsSV.TITLE, activityTitle);
                }
            }

            String viewType = view.getClass().getCanonicalName();
            CharSequence viewText = null;
            if (view instanceof CheckBox) {
                viewType = "CheckBox";
                CheckBox checkBox = (CheckBox) view;
                viewText = checkBox.getText();
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
            } else if (view instanceof ImageView) {
                viewType = "ImageView";
            } else if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtilSV.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstantsSV.ELEMENT_CONTENT, viewText.toString());
            }

            properties.put(AopConstantsSV.ELEMENT_TYPE, viewType);
            AopUtilSV.getFragmentNameFromView(view, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_sv_tag_view_properties);
            if (p != null) {
                AopUtilSV.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDKSV.sharedInstance().autotrack(AopConstantsSV.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLogSV.i(TAG, "TrackViewOnClick error: " + e.getMessage());
        }
    }
}

