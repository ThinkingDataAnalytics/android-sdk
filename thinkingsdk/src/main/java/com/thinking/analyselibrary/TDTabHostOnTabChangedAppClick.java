package com.thinking.analyselibrary;

import android.text.TextUtils;
import android.widget.TabHost;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.TDLog;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

public class TDTabHostOnTabChangedAppClick {
    private final static String TAG = "TDTabHostOnTabChangedAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            if (!ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            if (ThinkingAnalyticsSDK.sharedInstance().isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 1) {
                return;
            }

            if (AopUtil.isViewIgnored(TabHost.class)) {
                return;
            }

            String tabName = (String) joinPoint.getArgs()[0];
            JSONObject properties = new JSONObject();

            try {
                if (!TextUtils.isEmpty(tabName)) {
                    String[] temp = tabName.split("##");

                    switch (temp.length) {
                        case 3:
                            properties.put(AopConstants.TITLE, temp[2]);
                        case 2:
                            properties.put(AopConstants.SCREEN_NAME, temp[1]);
                        case 1:
                            properties.put(AopConstants.ELEMENT_CONTENT, temp[0]);
                            break;
                    }
                }
            } catch (Exception e) {
                properties.put(AopConstants.ELEMENT_CONTENT, tabName);
                e.printStackTrace();
            }

            properties.put(AopConstants.ELEMENT_TYPE, "TabHost");

            ThinkingAnalyticsSDK.sharedInstance().autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            TDLog.i(TAG, " onTabChanged AOP ERROR: " + e.getMessage());
        }
    }
}
