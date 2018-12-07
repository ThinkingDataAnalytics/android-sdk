package com.thinking.analyselibrarysv;


import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDSeekBarOnSeekBarChangeAppClickSV {
    private final static String TAG = "TDSeekBarOnSeekBarChangeAppClickSV";

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

            SeekBar seekBar = (SeekBar) view;

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

            properties.put(AopConstantsSV.ELEMENT_TYPE, "SeekBar");
            properties.put(AopConstantsSV.ELEMENT_CONTENT, String.valueOf(seekBar.getProgress()));
            AopUtilSV.getFragmentNameFromView(seekBar, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_sv_tag_view_properties);
            if (p != null) {
                AopUtilSV.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDKSV.sharedInstance().autotrack(AopConstantsSV.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLogSV.i(TAG, " AOP ERROR: " + e.getMessage());
        }
    }
}

