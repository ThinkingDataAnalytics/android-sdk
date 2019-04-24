package com.thinking.analyselibrary.demo;

import android.app.Application;
import android.content.Context;

import com.thinking.analyselibrary.ThinkingAnalyticsSDK;

import java.util.ArrayList;
import java.util.List;

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
    //private static final String TA_SERVER_URL = "https://tdcollector.uwinltd.com";

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        initThinkingDataSDK();
    }

    /** 初始化 TA SDK */
    private void initThinkingDataSDK() {
        ThinkingAnalyticsSDK.sharedInstance(mContext, TA_APP_ID, TA_SERVER_URL);

        // enable auto track
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        ThinkingAnalyticsSDK.sharedInstance(this).enableAutoTrack(eventTypeList);

        // enable fragment auto track
        ThinkingAnalyticsSDK.sharedInstance(this).trackFragmentAppViewScreen();

        // track a basic event
        ThinkingAnalyticsSDK.sharedInstance(this).track("app_started");

    }
}
