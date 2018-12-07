package com.thinking.analyselibrarysv;

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


public class TDExpandableListViewItemChildAppClickSV {
    private final static String TAG = "TDExpandableListViewItemChildAppClickSV";

    public static void onItemChildClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_CLICK)) {
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

            Activity activity = AopUtilSV.getActivityFromContext(context, expandableListView);
            if (activity != null) {
                if (ThinkingAnalyticsSDKSV.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtilSV.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            if (AopUtilSV.isViewIgnored(expandableListView)) {
                return;
            }

            View view = (View) joinPoint.getArgs()[1];

            if (AopUtilSV.isViewIgnored(view)) {
                return;
            }

            int groupPosition = (int) joinPoint.getArgs()[2];
            int childPosition = (int) joinPoint.getArgs()[3];

            JSONObject properties = (JSONObject) view.getTag(R.id.thinking_analytics_sv_tag_view_properties);

            if (properties == null) {
                properties = new JSONObject();
            }

            properties.put(AopConstantsSV.ELEMENT_POSITION, String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));

            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof ThinkingExpandableListViewItemTrackPropertiesSV) {
                    ThinkingExpandableListViewItemTrackPropertiesSV trackProperties = (ThinkingExpandableListViewItemTrackPropertiesSV) listAdapter;
                    JSONObject jsonObject = trackProperties.getThinkingChildItemTrackProperties(groupPosition, childPosition);
                    if (jsonObject != null && CheckPropertySV.checkProperty(jsonObject)) {
                        AopUtilSV.mergeJSONObject(jsonObject, properties);
                    }
                }
            }

            AopUtilSV.addViewPathProperties(activity, view, properties);

            if (activity != null) {
                properties.put(AopConstantsSV.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtilSV.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstantsSV.TITLE, activityTitle);
                }
            }

            String idString = AopUtilSV.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstantsSV.ELEMENT_ID, idString);
            }

            properties.put(AopConstantsSV.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtilSV.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstantsSV.ELEMENT_CONTENT, viewText);
            }

            AopUtilSV.getFragmentNameFromView(expandableListView, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_sv_tag_view_properties);
            if (p != null) {
                AopUtilSV.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDKSV.sharedInstance().autotrack(AopConstantsSV.APP_CLICK_EVENT_NAME, properties);

        } catch (Exception e) {
            e.printStackTrace();
            TDLogSV.i(TAG, " ExpandableListView.OnChildClickListener.onChildClick AOP ERROR: " + e.getMessage());
        }
    }

    public static void onItemGroupClick(JoinPoint joinPoint) {
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
                if (ThinkingAnalyticsSDKSV.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtilSV.isViewIgnored(joinPoint.getArgs()[0].getClass())) {
                return;
            }

            if (AopUtilSV.isViewIgnored(expandableListView)) {
                return;
            }

            View view = (View) joinPoint.getArgs()[1];

            int groupPosition = (int) joinPoint.getArgs()[2];

            JSONObject properties = new JSONObject();

			AopUtilSV.addViewPathProperties(activity, view, properties);

            if (activity != null) {
                properties.put(AopConstantsSV.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtilSV.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstantsSV.TITLE, activityTitle);
                }
            }

            String idString = AopUtilSV.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstantsSV.ELEMENT_ID, idString);
            }

            properties.put(AopConstantsSV.ELEMENT_TYPE, "ExpandableListView");

            AopUtilSV.getFragmentNameFromView(expandableListView, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_sv_tag_view_properties);
            if (p != null) {
                AopUtilSV.mergeJSONObject(p, properties);
            }

            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof ThinkingExpandableListViewItemTrackPropertiesSV) {
                    try {
                        ThinkingExpandableListViewItemTrackPropertiesSV trackProperties = (ThinkingExpandableListViewItemTrackPropertiesSV) listAdapter;
                        JSONObject jsonObject = trackProperties.getThinkingGroupItemTrackProperties(groupPosition);
                        if (jsonObject != null && CheckPropertySV.checkProperty(jsonObject)) {
                            AopUtilSV.mergeJSONObject(jsonObject, properties);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            ThinkingAnalyticsSDKSV.sharedInstance().autotrack(AopConstantsSV.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLogSV.i(TAG, " ExpandableListView.OnChildClickListener.onGroupClick AOP ERROR: " + e.getMessage());
        }
    }
}
