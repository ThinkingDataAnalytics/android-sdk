package cn.thinkingdata.android.demo;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

import cn.thinkingdata.android.ThinkingAnalyticsSDK;

public class DemoApplication extends Application {
    private static Context mContext;


    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreate() {
        super.onCreate();

        TDTracker.initThinkingDataSDK(getApplicationContext());
        NtpTime.startCalibrateTime();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i("hh","onTerminate");
    }
}
