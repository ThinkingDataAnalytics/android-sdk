package com.thinking.analyselibrary.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.thinking.analyselibrary.ThinkingAnalyticsSDK;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DemoApplication extends Application {
    private static Context mContext;


    @Override
    public void onCreate() {
        super.onCreate();
        TDTracker.initThinkingDataSDK(getApplicationContext());
        NtpTime.startCalibrateTime();
    }
}
