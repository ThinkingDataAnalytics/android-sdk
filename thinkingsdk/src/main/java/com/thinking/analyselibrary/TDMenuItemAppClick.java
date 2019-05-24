package com.thinking.analyselibrary;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.MenuItem;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.TDLog;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDMenuItemAppClick {
    private final static String TAG = "TDMenuItemAppClick";

    public static void onAppClick(final JoinPoint joinPoint, final int menuItemIndex) {
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

                    if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length == 0) {
                        return;
                    }

                    MenuItem menuItem = (MenuItem) joinPoint.getArgs()[menuItemIndex];
                    if (menuItem == null) {
                        return;
                    }

                    if (AopUtil.isViewIgnored(instance, MenuItem.class)) {
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

                    Activity activity = AopUtil.getActivityFromContext(context, null);
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
}
