package cn.thinkingdata.android.utils;

import android.util.Log;

public class TDLog {
    volatile static boolean mEnableLog = false;

    public static void setEnableLog(boolean enable) {
        mEnableLog = enable;
    }

    public static void d(String tag, String msg) {
        if(mEnableLog){
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String message, Throwable throwable) {
        if(mEnableLog) {
            Log.i(tag, message, throwable);
        }
    }

    public static void i(String tag, String message) {
        if(mEnableLog) {
            if (message.length() > 4000) {
                largeLog(tag, message);
            } else {
                Log.i(tag, message);
            }
        }
    }

    private static void largeLog(String tag, String content) {
        if (content.length() > 4000) {
            Log.i(tag, content.substring(0, 4000));
            largeLog(tag, content.substring(4000));
        } else {
            Log.i(tag, content);
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
        Log.e(tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
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
