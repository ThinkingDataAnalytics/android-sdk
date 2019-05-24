package com.thinking.analyselibrary.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.thinking.analyselibrary.ThinkingAnalyticsSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DemoApplication extends Application {
    private static Context mContext;

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

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        initThinkingDataSDK();
    }

    /** 初始化 TA SDK */
    private void initThinkingDataSDK() {
        ThinkingAnalyticsSDK.sharedInstance(mContext, TA_APP_ID, TA_SERVER_URL);

        Log.d("ThinkingDataDemo","get distinct id: " + ThinkingAnalyticsSDK.sharedInstance(this).getDistinctId());

        // set distinct id
        ThinkingAnalyticsSDK.sharedInstance(this).identify("1234567abc");

        // enable auto track
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        ThinkingAnalyticsSDK.sharedInstance(this).enableAutoTrack(eventTypeList);

        // enable fragment auto track
        ThinkingAnalyticsSDK.sharedInstance(this).trackFragmentAppViewScreen();

        // 设置动态属性
        ThinkingAnalyticsSDK.sharedInstance(this).setDynamicSuperPropertiesTracker(
                new ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                JSONObject dynamicSuperProperties = new JSONObject();
                String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
                SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern, Locale.CHINA);
                String timeString = sDateFormat.format(new Date());
                try {
                    dynamicSuperProperties.put("dynamicTime", timeString);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return dynamicSuperProperties;
            }
        });
        ThinkingAnalyticsSDK.sharedInstance(this).track("app_started");

    }
}
