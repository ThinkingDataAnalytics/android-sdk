package cn.thinkingdata.android.demo;

import android.app.Application;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import cn.thinkingdata.android.ThinkingAnalyticsSDK;

public class DemoApplication extends Application {
    private static Context mContext;


    @Override
    public void onCreate() {
        super.onCreate();
        TDTracker.initThinkingDataSDK(getApplicationContext());
//        TDTracker.getInstance().enableTracking(true);
//        List<ThinkingAnalyticsSDK.AutoTrackEventType> types = new ArrayList<>();
//        types.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
//        TDTracker.getInstance().enableAutoTrack(types);
        NtpTime.startCalibrateTime();
    }
}
