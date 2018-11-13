package com.thinking.analyselibrary;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDRatingBarOnRatingChangedAppClick {
    private final static String TAG = "TDRatingBarOnRatingChangedAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 3) {
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

            float rating = (float) joinPoint.getArgs()[1];
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

            properties.put(AopConstants.ELEMENT_TYPE, "RatingBar");
            properties.put(AopConstants.ELEMENT_CONTENT, String.valueOf(rating));
            AopUtil.getFragmentNameFromView(view, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDK.sharedInstance().autotrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLog.i(TAG, "RatingBar.OnRatingBarChangeListener.onRatingChanged AOP ERROR: " + e.getMessage());
        }
    }
}
