package com.thinking.analyselibrary;

import org.json.JSONException;
import org.json.JSONObject;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "ThinkingAnalyticsSDK.Exception";

    private static final int SLEEP_TIMEOUT_MS = 3000;

    private static ExceptionHandler sInstance;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    public ExceptionHandler() {
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static void init() {
        if (sInstance == null) {
            synchronized (ExceptionHandler.class) {
                if (sInstance == null) {
                    sInstance = new ExceptionHandler();
                }
            }
        }
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        // Only one worker thread - giving priority to storing the event first and then flush
        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK instance) {
                if (instance.shouldTrackCrash()) {
                    try {
                        final JSONObject messageProp = new JSONObject();
                        messageProp.put("app_crashed_reason", e.toString());
                        instance.track("ta_app_crash", messageProp);
                    } catch (JSONException e) {
                    }
                }
            }
        });

        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK thinkingAnalyticsSDK) {
                thinkingAnalyticsSDK.mMessages.setUncaughtExceptionStatus(true);
                thinkingAnalyticsSDK.flush();
            }
        });

        //waitFlush();
        synchronized (this.getClass()) {
            try {
                this.getClass().wait(SLEEP_TIMEOUT_MS);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        if (mDefaultExceptionHandler != null) {
            mDefaultExceptionHandler.uncaughtException(t, e);
        } else {
            killProcessAndExit();
        }
    }

    private void killProcessAndExit() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
