package com.thinking.analyselibrary.utils;

import android.util.Log;

public class TDLog {
    private static boolean mEnableLog = false;

    public static void setEnableLog(boolean enable) {
        mEnableLog = enable;
    }

    public static void d(String tag, String msg) {
        if(mEnableLog){
            Log.i(tag, getStackTrace() + msg);
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

    // 获取当前调用路径
    private static String getStackTrace() {
        StringBuffer sb = new StringBuffer();
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];
        sb.append(stackTraceElement.getClassName());
        sb.append(".");
        sb.append(stackTraceElement.getMethodName());
        sb.append("() line ");
        sb.append(stackTraceElement.getLineNumber());
        sb.append(": ");
        return sb.toString();
    }
}