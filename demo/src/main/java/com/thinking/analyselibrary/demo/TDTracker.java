package com.thinking.analyselibrary.demo;

import android.content.Context;

import com.thinking.analyselibrary.ThinkingAnalyticsSDK;

import java.util.ArrayList;
import java.util.List;

public class TDTracker {
    /**
     * 项目APP_ID，在申请项目时会给出
     */
    private static final String TA_APP_ID = "b2a61feb9e56472c90c5bcb320dfb4ef";
    //private static final String TA_APP_ID = "debug-appid";

    /**
     * 数据上传地址
     * 如果您使用的是云服务，请输入以下URL:
     * http://receiver.ta.thinkingdata.cn:9080
     * 如果您使用的是私有化部署的版本，请输入以下URL:
     * http://数据采集地址:9080
     */
    private static final String TA_SERVER_URL = "https://sdk.tga.thinkinggame.cn";

    private static ThinkingAnalyticsSDK mInstance;
    private static ThinkingAnalyticsSDK mDebugInstance;

    public static ThinkingAnalyticsSDK getInstance() {
        return mInstance;
    }

    public static ThinkingAnalyticsSDK getDebugInstance() {
        return mDebugInstance;
    }

    /** 初始化 TA SDK */
    static void initThinkingDataSDK(Context context) {
        Context mContext = context.getApplicationContext();
        mInstance = ThinkingAnalyticsSDK.sharedInstance(mContext, TA_APP_ID, TA_SERVER_URL);
        mDebugInstance = ThinkingAnalyticsSDK.sharedInstance(mContext, "debug-appid", TA_SERVER_URL);

        //Log.d("ThinkingDataDemo","get distinct id: " + ThinkingAnalyticsSDK.sharedInstance(this).getDistinctId());

        // set distinct id
        mInstance.identify("1234567abc");

        // enable auto track
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        mInstance.enableAutoTrack(eventTypeList);
        mDebugInstance.enableAutoTrack(eventTypeList);

        // enable fragment auto track
        //ThinkingAnalyticsSDK.sharedInstance(this).trackFragmentAppViewScreen();
        //mInstance.trackFragmentAppViewScreen();
        //mDebugInstance.trackFragmentAppViewScreen();

        // 设置动态属性
        //ThinkingAnalyticsSDK.sharedInstance(this).setDynamicSuperPropertiesTracker(
        //        new ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker() {
        //    @Override
        //    public JSONObject getDynamicSuperProperties() {
        //        JSONObject dynamicSuperProperties = new JSONObject();
        //        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        //        SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern, Locale.CHINA);
        //        String timeString = sDateFormat.format(new Date());
        //        try {
        //            dynamicSuperProperties.put("dynamicTime", timeString);
        //        } catch (JSONException e) {
        //            e.printStackTrace();
        //        }
        //        return dynamicSuperProperties;
        //    }
        //});
        //ThinkingAnalyticsSDK.sharedInstance(this).track("app_started");

    }
}
