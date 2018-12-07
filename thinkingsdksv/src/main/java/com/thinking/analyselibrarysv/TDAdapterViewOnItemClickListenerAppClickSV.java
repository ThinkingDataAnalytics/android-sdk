package com.thinking.analyselibrarysv;

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

public class TDAdapterViewOnItemClickListenerAppClickSV {
    private final static String TAG = "TDAdapterViewOnItemClickListenerAppClickSV";

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

            Activity activity = AopUtilSV.getActivityFromContext(context, view);
            if (activity != null) {
                if (ThinkingAnalyticsSDKSV.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (AopUtilSV.isViewIgnored(object.getClass())) {
                return;
            }

            int position = (int) joinPoint.getArgs()[2];
            JSONObject properties = new JSONObject();
            AdapterView adapterView = (AdapterView) object;

            List<Class> mIgnoredViewTypeList = ThinkingAnalyticsSDKSV.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                if (adapterView instanceof ListView) {
                    properties.put(AopConstantsSV.ELEMENT_TYPE, "ListView");
                    if (AopUtilSV.isViewIgnored(ListView.class)) {
                        return;
                    }
                } else if (adapterView instanceof GridView) {
                    properties.put(AopConstantsSV.ELEMENT_TYPE, "GridView");
                    if (AopUtilSV.isViewIgnored(GridView.class)) {
                        return;
                    }
                }
            }

            Adapter adapter = adapterView.getAdapter();
            if (adapter != null && adapter instanceof ThinkingAdapterViewItemTrackPropertiesSV) {
                try {
                    ThinkingAdapterViewItemTrackPropertiesSV objectProperties = (ThinkingAdapterViewItemTrackPropertiesSV) adapter;
                    JSONObject jsonObject = objectProperties.getThinkingItemTrackProperties(position);
                    if (jsonObject != null && CheckPropertySV.checkProperty(jsonObject)) {
                        AopUtilSV.mergeJSONObject(jsonObject, properties);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
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

            properties.put(AopConstantsSV.ELEMENT_POSITION, String.valueOf(position));

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

            AopUtilSV.getFragmentNameFromView(adapterView, properties);

            JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_sv_tag_view_properties);
            if (p != null) {
                AopUtilSV.mergeJSONObject(p, properties);
            }

            ThinkingAnalyticsSDKSV.sharedInstance().autotrack(AopConstantsSV.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLogSV.i(TAG, " AdapterView.OnItemClickListener.onItemClick AOP ERROR: " + e.getMessage());
        }
    }
}
