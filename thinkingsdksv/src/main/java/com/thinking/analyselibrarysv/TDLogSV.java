package com.thinking.analyselibrarysv;

import android.util.Log;

public class TDLogSV {
    public static void d(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void i(String tag, String message, Throwable throwable) {
         Log.i(tag, message, throwable);
    }

    public static void i(String tag, String message) {
        Log.i(tag, message);
    }

    public static void i(String tag, Throwable tr) {
        try {
            Log.i(tag, "", tr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
    }

    public static void w(String tag, String message, Throwable throwable) {
        Log.w(tag, message, throwable);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }

    public static void v(String tag, String message) {
        Log.v(tag, message);
    }

    public static void v(String tag, String message, Throwable throwable) {
        Log.v(tag, message, throwable);
    }
}
