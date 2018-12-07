package com.thinking.analyselibrarysv;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDRadioGroupOnCheckedAppClickSV {
    private final static String TAG = "TDRadioGroupOnCheckedAppClickSV";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_CLICK)) {
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

            Activity activity = AopUtilSV.getActivityFromContext(context, view);
            if (activity != null) {
                if (ThinkingAnalyticsSDKSV.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtilSV.isViewIgnored(view)) {
                return;
            }

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

            if (view instanceof RadioGroup) {
                properties.put(AopConstantsSV.ELEMENT_TYPE, "RadioGroup");
                RadioGroup radioGroup = (RadioGroup) view;

                //获取变更后的选中项的ID
                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                if (activity != null) {
                    try {
                        RadioButton radioButton = (RadioButton) activity.findViewById(checkedRadioButtonId);
                        if (radioButton != null) {
                            if (!TextUtils.isEmpty(radioButton.getText())) {
                                String viewText = radioButton.getText().toString();
                                if (!TextUtils.isEmpty(viewText)) {
                                    properties.put(AopConstantsSV.ELEMENT_CONTENT, viewText);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                properties.put(AopConstantsSV.ELEMENT_TYPE, view.getClass().getCanonicalName());
            }

            AopUtilSV.getFragmentNameFromView(view, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_sv_tag_view_properties);
            if (p != null) {
                AopUtilSV.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDKSV.sharedInstance().autotrack(AopConstantsSV.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLogSV.i(TAG, "RadioGroup.OnCheckedChangeListener.onCheckedChanged AOP ERROR: " + e.getMessage());
        }
    }
}
