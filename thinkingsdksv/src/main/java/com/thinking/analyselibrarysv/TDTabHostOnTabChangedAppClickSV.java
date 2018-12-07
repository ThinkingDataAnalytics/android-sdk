package com.thinking.analyselibrarysv;

import android.text.TextUtils;
import android.widget.TabHost;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDTabHostOnTabChangedAppClickSV {
    private final static String TAG = "TDTabHostOnTabChangedAppClickSV";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDKSV.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDKSV.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 1) {
                return;
            }

            if (AopUtilSV.isViewIgnored(TabHost.class)) {
                return;
            }

            String tabName = (String) joinPoint.getArgs()[0];
            JSONObject properties = new JSONObject();

            try {
                if (!TextUtils.isEmpty(tabName)) {
                    String[] temp = tabName.split("##");

                    switch (temp.length) {
                        case 3:
                            properties.put(AopConstantsSV.TITLE, temp[2]);
                        case 2:
                            properties.put(AopConstantsSV.SCREEN_NAME, temp[1]);
                        case 1:
                            properties.put(AopConstantsSV.ELEMENT_CONTENT, temp[0]);
                            break;
                    }
                }
            } catch (Exception e) {
                properties.put(AopConstantsSV.ELEMENT_CONTENT, tabName);
                e.printStackTrace();
            }

            properties.put(AopConstantsSV.ELEMENT_TYPE, "TabHost");

            ThinkingAnalyticsSDKSV.sharedInstance().autotrack(AopConstantsSV.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLogSV.i(TAG, " onTabChanged AOP ERROR: " + e.getMessage());
        }
    }
}
