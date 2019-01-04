package com.thinking.analyselibrary;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import org.aspectj.lang.JoinPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class TDExpandableListViewItemChildAppClick {
    private final static String TAG = "TDExpandableListViewItemChildAppClick";

    public static void onItemChildClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 5) {
                return;
            }

            ExpandableListView expandableListView = (ExpandableListView) joinPoint.getArgs()[0];
            if (expandableListView == null) {
                return;
            }

            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            Activity activity = AopUtil.getActivityFromContext(context, expandableListView);
            if (activity != null) {
                if (ThinkingAnalyticsSDK.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            View view = (View) joinPoint.getArgs()[1];

            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            int groupPosition = (int) joinPoint.getArgs()[2];
            int childPosition = (int) joinPoint.getArgs()[3];

            JSONObject properties = (JSONObject) view.getTag(R.id.thinking_analytics_tag_view_properties);

            if (properties == null) {
                properties = new JSONObject();
            }

            properties.put(AopConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));

            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof ThinkingExpandableListViewItemTrackProperties) {
                    ThinkingExpandableListViewItemTrackProperties trackProperties = (ThinkingExpandableListViewItemTrackProperties) listAdapter;
                    JSONObject jsonObject = trackProperties.getThinkingChildItemTrackProperties(groupPosition, childPosition);
                    if (jsonObject != null && CheckProperty.checkProperty(jsonObject)) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                }
            }

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
            }
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            AopUtil.getFragmentNameFromView(expandableListView, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDK.sharedInstance().autotrack(AopConstants.APP_CLICK_EVENT_NAME, properties);

        } catch (Exception e) {
            e.printStackTrace();
            TDLog.i(TAG, " ExpandableListView.OnChildClickListener.onChildClick AOP ERROR: " + e.getMessage());
        }
    }

    public static void onItemGroupClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 4) {
                return;
            }

            ExpandableListView expandableListView = (ExpandableListView) joinPoint.getArgs()[0];
            if (expandableListView == null) {
                return;
            }

            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            Activity activity = null;
            if (context instanceof Activity) {
                activity = (Activity) context;
            }

            if (activity != null) {
                if (ThinkingAnalyticsSDK.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtil.isViewIgnored(joinPoint.getArgs()[0].getClass())) {
                return;
            }

            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            View view = (View) joinPoint.getArgs()[1];

            int groupPosition = (int) joinPoint.getArgs()[2];

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

            properties.put(AopConstants.ELEMENT_TYPE, "ExpandableListView");

            AopUtil.getFragmentNameFromView(expandableListView, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof ThinkingExpandableListViewItemTrackProperties) {
                    try {
                        ThinkingExpandableListViewItemTrackProperties trackProperties = (ThinkingExpandableListViewItemTrackProperties) listAdapter;
                        JSONObject jsonObject = trackProperties.getThinkingGroupItemTrackProperties(groupPosition);
                        if (jsonObject != null && CheckProperty.checkProperty(jsonObject)) {
                            AopUtil.mergeJSONObject(jsonObject, properties);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            ThinkingAnalyticsSDK.sharedInstance().autotrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLog.i(TAG, " ExpandableListView.OnChildClickListener.onGroupClick AOP ERROR: " + e.getMessage());
        }
    }
}
