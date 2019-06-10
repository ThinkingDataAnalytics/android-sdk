package com.thinking.analyselibrary;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Spinner;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDSpinnerOnItemSelectedAppClick {
    private final static String TAG = "TDSpinnerOnItemSelectedAppClick";

    public static void onAppClick(final JoinPoint joinPoint) {
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

                    Activity activity = AopUtil.getActivityFromContext(context);

                    if (activity != null) {
                        if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    if (AopUtil.isViewIgnored(instance, adapterView)) {
                        return;
                    }

                    View view = (View) joinPoint.getArgs()[1];
                    int position = (int) joinPoint.getArgs()[2];
                    JSONObject properties = new JSONObject();
                    AopUtil.addViewPathProperties(activity, view, properties);

                    String idString = AopUtil.getViewId(adapterView);
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

                    if (adapterView instanceof Spinner) {
                        properties.put(AopConstants.ELEMENT_TYPE, "Spinner");
                        Object item = adapterView.getItemAtPosition(position);
                        properties.put(AopConstants.ELEMENT_POSITION, String.valueOf(position));
                        if (item != null) {
                            if (item instanceof String) {
                                properties.put(AopConstants.ELEMENT_CONTENT, item);
                            }
                        }
                    } else {
                        properties.put(AopConstants.ELEMENT_TYPE, adapterView.getClass().getCanonicalName());
                    }

                    AopUtil.getFragmentNameFromView(adapterView, properties);
                    JSONObject p = (JSONObject) AopUtil.getTag(instance.getToken(), adapterView,
                            R.id.thinking_analytics_tag_view_properties);
                    if (p != null) {
                        TDUtil.mergeJSONObject(p, properties);
                    }

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    TDLog.i(TAG, " AdapterView.OnItemSelectedListener.onItemSelected AOP ERROR: " + e.getMessage());
                }

            }
        });
    }
}

