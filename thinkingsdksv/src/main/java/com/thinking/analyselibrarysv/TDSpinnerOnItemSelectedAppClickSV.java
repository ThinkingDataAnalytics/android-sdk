package com.thinking.analyselibrarysv;


import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Spinner;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;


public class TDSpinnerOnItemSelectedAppClickSV {
    private final static String TAG = "TDSpinnerOnItemSelectedAppClickSV";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 4) {
                return;
            }

            android.widget.AdapterView adapterView = (android.widget.AdapterView) joinPoint.getArgs()[0];
            if (adapterView == null) {
                return;
            }

            Context context = adapterView.getContext();
            if (context == null) {
                return;
            }

            Activity activity = AopUtilSV.getActivityFromContext(context, adapterView);

            if (activity != null) {
                if (ThinkingAnalyticsSDKSV.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtilSV.isViewIgnored(adapterView)) {
                return;
            }

            View view = (View) joinPoint.getArgs()[1];
            int position = (int) joinPoint.getArgs()[2];
            JSONObject properties = new JSONObject();
            AopUtilSV.addViewPathProperties(activity, view, properties);

            String idString = AopUtilSV.getViewId(adapterView);
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

            if (adapterView instanceof Spinner) {
                properties.put(AopConstantsSV.ELEMENT_TYPE, "Spinner");
                Object item = adapterView.getItemAtPosition(position);
                properties.put(AopConstantsSV.ELEMENT_POSITION, String.valueOf(position));
                if (item != null) {
                    if (item instanceof String) {
                        properties.put(AopConstantsSV.ELEMENT_CONTENT, item);
                    }
                }
            } else {
                properties.put(AopConstantsSV.ELEMENT_TYPE, adapterView.getClass().getCanonicalName());
            }

            AopUtilSV.getFragmentNameFromView(adapterView, properties);
            JSONObject p = (JSONObject) adapterView.getTag(R.id.thinking_analytics_sv_tag_view_properties);
            if (p != null) {
                AopUtilSV.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDKSV.sharedInstance().autotrack(AopConstantsSV.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLogSV.i(TAG, " AdapterView.OnItemSelectedListener.onItemSelected AOP ERROR: " + e.getMessage());
        }
    }
}

