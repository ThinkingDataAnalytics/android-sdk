package com.example.thinkingdata.testandroid;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import com.thinking.analyselibrary.ThinkingAnalyticsSDK;
import com.thinking.analyselibrary.ThinkingDataTrackEvent;
import com.thinking.analyselibrarysv.ThinkingAnalyticsSDKSV;

public class MainActivity extends AppCompatActivity {
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getBaseContext();
        final String appid = "test-sdk-appid";

        ThinkingAnalyticsSDK.sharedInstance(mContext,appid,"http://receiver.ta.thinkingdata.cn:9080");

        ThinkingAnalyticsSDKSV.sharedInstance(mContext,"b8976e6cf8fb4f1b8aa761cecbb423da","http://sdk.tga.thinkinggame.cn:9080/");

        ThinkingAnalyticsSDKSV.sharedInstance().track("testsv");

        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        ThinkingAnalyticsSDK.sharedInstance(this).enableAutoTrack(eventTypeList);
        ThinkingAnalyticsSDK.sharedInstance(this).track("demo");

        text();
    }

    @ThinkingDataTrackEvent(eventName = "someEventName", properties = "{\"size\":100,\"isFirst\":true}")
    protected void text(){
        Log.i("MainActivity", "text: ");
    }
}
