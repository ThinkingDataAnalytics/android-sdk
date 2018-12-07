package com.thinking.analyselibrarysv;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.MenuItem;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDMenuItemAppClickSV {
    private final static String TAG = "TDMenuItemAppClickSV";

    public static void onAppClick(JoinPoint joinPoint, int menuItemIndex) {
        try {
            if (!ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length == 0) {
                return;
            }

            MenuItem menuItem = (MenuItem) joinPoint.getArgs()[menuItemIndex];
            if (menuItem == null) {
                return;
            }

            if (AopUtilSV.isViewIgnored(MenuItem.class)) {
                return;
            }

            Object object = joinPoint.getTarget();
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

            Activity activity = AopUtilSV.getActivityFromContext(context, null);
            if (activity != null) {
                if (ThinkingAnalyticsSDKSV.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
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
                properties.put(AopConstantsSV.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtilSV.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstantsSV.TITLE, activityTitle);
                }
            }

            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstantsSV.ELEMENT_ID, idString);
            }

            if (!TextUtils.isEmpty(menuItem.getTitle())) {
                properties.put(AopConstantsSV.ELEMENT_CONTENT, menuItem.getTitle());
            }

            properties.put(AopConstantsSV.ELEMENT_TYPE, "MenuItem");

            ThinkingAnalyticsSDKSV.sharedInstance().autotrack(AopConstantsSV.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLogSV.i(TAG, " error: " + e.getMessage());
        }
    }
}
