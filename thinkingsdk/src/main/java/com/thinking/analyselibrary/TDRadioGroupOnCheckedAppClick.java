package com.thinking.analyselibrary;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.TDLog;
import com.thinking.analyselibrary.utils.TDUtil;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

import java.nio.charset.IllegalCharsetNameException;

public class TDRadioGroupOnCheckedAppClick {
    private final static String TAG = "TDRadioGroupOnCheckedAppClick";

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

                    if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 2) {
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
                        if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    if (AopUtil.isViewIgnored(instance, view)) {
                        return;
                    }

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

                    if (view instanceof RadioGroup) {
                        properties.put(AopConstants.ELEMENT_TYPE, "RadioGroup");
                        RadioGroup radioGroup = (RadioGroup) view;

                        //获取变更后的选中项的ID
                        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                        if (activity != null) {
                            try {
                                RadioButton radioButton = (RadioButton) activity.findViewById(checkedRadioButtonId);
                                if (radioButton != null) {
                                    if (!TextUtils.isEmpty(radioButton.getText())) {
                                        String viewText = radioButton.getText().toString();
                                        if (!TextUtils.isEmpty(viewText)) {
                                            properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        properties.put(AopConstants.ELEMENT_TYPE, view.getClass().getCanonicalName());
                    }

                    AopUtil.getFragmentNameFromView(view, properties);

                    JSONObject p = (JSONObject) view.getTag(R.id.thinking_analytics_tag_view_properties);
                    if (p != null) {
                        TDUtil.mergeJSONObject(p, properties);
                    }

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    TDLog.i(TAG, "RadioGroup.OnCheckedChangeListener.onCheckedChanged AOP ERROR: " + e.getMessage());
                }

            }
        });
    }
}
