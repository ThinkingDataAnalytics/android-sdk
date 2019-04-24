package com.thinking.analyselibrary;

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

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDTrackViewOnAppClick {
    private final static String TAG = "TDTrackViewOnAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
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

            Activity activity = AopUtil.getActivityFromContext(context, view);

            if (activity != null) {
                if (ThinkingAnalyticsSDK.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            long currentOnClickTimestamp = System.currentTimeMillis();
            String tag = (String) view.getTag(R.id.thinking_analytics_tag_view_onclick_timestamp);
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
            view.setTag(R.id.thinking_analytics_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));

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

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_tag_view_properties);
            if (p != null) {
                TDUtil.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDK.sharedInstance().autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLog.i(TAG, "TrackViewOnClick error: " + e.getMessage());
        }
    }
}

