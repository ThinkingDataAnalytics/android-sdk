package com.thinking.analyselibrary;

import android.util.Log;

public class TDLog {
    private static boolean mEnableLog = false;

    public static void setEnableLog(boolean enable) {
        mEnableLog = enable;
    }

    public static void d(String tag, String msg) {
        if(mEnableLog){
            Log.i(tag, msg);
        }
    }

    public static void i(String tag, String message, Throwable throwable) {
        if(mEnableLog) {
            Log.i(tag, message, throwable);
        }
    }

    public static void i(String tag, String message) {
        if(mEnableLog) {
            Log.i(tag, message);
        }
    }

    public static void i(String tag, Throwable tr) {
        if(mEnableLog) {
            try {
                Log.i(tag, "", tr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void w(String tag, String message) {
        if(mEnableLog) {
            Log.w(tag, message);
        }
    }

    public static void w(String tag, String message, Throwable throwable) {
        if(mEnableLog) {
            Log.w(tag, message, throwable);
        }
    }

    public static void e(String tag, String message) {
        if(mEnableLog) {
            Log.e(tag, message);
        }
    }

    public static void e(String tag, String message, Throwable throwable) {
        if(mEnableLog) {
            Log.e(tag, message, throwable);
        }
    }

    public static void v(String tag, String message) {
        if(mEnableLog) {
            Log.v(tag, message);
        }
    }

    public static void v(String tag, String message, Throwable throwable) {
        if(mEnableLog) {
            Log.v(tag, message, throwable);
        }
    }
}
