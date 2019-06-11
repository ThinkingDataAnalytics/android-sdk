package com.thinking.analyselibrary.demo;

import android.app.Application;
import android.content.Context;

public class DemoApplication extends Application {
    private static Context mContext;



    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        TDTracker.initThinkingDataSDK(mContext);
    }


}
