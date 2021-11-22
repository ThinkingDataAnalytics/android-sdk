package cn.thinkingdata.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.runner.AndroidJUnitRunner;

import cn.thinkingdata.android.utils.TDUtils;


public class TestRunner extends AndroidJUnitRunner {
    private static final String TAG = "TA_TEST.TestRunner";


    @Override
    public void callApplicationOnCreate(Application app) {
        Log.d(TAG, "--- callApplicationOnCreate ---" + TDUtils.getMainProcessName(app.getApplicationContext()));

    }


}
