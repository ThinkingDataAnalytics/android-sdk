package com.thinking.analyselibrary;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import org.aspectj.lang.JoinPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class TDAdapterViewOnItemClickListenerAppClick {
    private final static String TAG = "TDAdapterViewOnItemClickListenerAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
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

            Object object = joinPoint.getArgs()[0];
            if (object == null) {
                return;
            }

            View view = (View) joinPoint.getArgs()[1];
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

            if (AopUtil.isViewIgnored(object.getClass())) {
                return;
            }

            int position = (int) joinPoint.getArgs()[2];
            JSONObject properties = new JSONObject();
            AdapterView adapterView = (AdapterView) object;

            List<Class> mIgnoredViewTypeList = ThinkingAnalyticsSDK.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                if (adapterView instanceof ListView) {
                    properties.put(AopConstants.ELEMENT_TYPE, "ListView");
                    if (AopUtil.isViewIgnored(ListView.class)) {
                        return;
                    }
                } else if (adapterView instanceof GridView) {
                    properties.put(AopConstants.ELEMENT_TYPE, "GridView");
                    if (AopUtil.isViewIgnored(GridView.class)) {
                        return;
                    }
                }
            }

            Adapter adapter = adapterView.getAdapter();
            if (adapter != null && adapter instanceof ThinkingAdapterViewItemTrackProperties) {
                try {
                    ThinkingAdapterViewItemTrackProperties objectProperties = (ThinkingAdapterViewItemTrackProperties) adapter;
                    JSONObject jsonObject = objectProperties.getThinkingItemTrackProperties(position);
                    if (jsonObject != null && CheckProperty.checkProperty(jsonObject)) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
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

            properties.put(AopConstants.ELEMENT_POSITION, String.valueOf(position));

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

            AopUtil.getFragmentNameFromView(adapterView, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDK.sharedInstance().autotrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLog.i(TAG, " AdapterView.OnItemClickListener.onItemClick AOP ERROR: " + e.getMessage());
        }
    }
}
