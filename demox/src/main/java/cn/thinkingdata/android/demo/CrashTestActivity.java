/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;

/**
 * < Test Crash >.
 *
 * @author bugliee
 * @create 2022/3/20
 * @since 1.0.0
 */
public class CrashTestActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("hh", "crash test Activity onCreate");
        setContentView(R.layout.activity_crash_test);
        this.findViewById(R.id.btn_crash).setOnClickListener(v -> {
            throw new RuntimeException("this is a test RuntimeException!");
        });
        this.findViewById(R.id.btn_anr).setOnClickListener(v -> SystemClock.sleep(20000));
        this.findViewById(R.id.btn_native).setOnClickListener(v -> {
            try {
                Class<?> clazzNativeHandler
                        = Class.forName("cn.thinkingdata.android.crash.NativeHandler");
                Method methodTestNativeCrash
                        = clazzNativeHandler.getMethod("testNativeCrash", int.class);
                methodTestNativeCrash.invoke(null, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("hh", "crash test Activity onResume");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("hh", "crash test Activity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("hh", "crash test Activity onStop");
    }
}
