/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import android.util.Log;

/**
 * @author liulongbing
 * @since 2024/6/25
 */
public class LogUtil {

    private static OnLogPrintListener mListener;

    public static void setLogPrintListener(OnLogPrintListener listener) {
        mListener = listener;
    }

    public static void i(String tag, Throwable tr) {
        try {
            Log.i(tag, "", tr);
            if (tr != null) {
                handleLogListener(tag, tr.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String tag, String message, Throwable throwable) {
        Log.i(tag, message, throwable);
        handleLogListener(tag, message);
    }

    public static void i(String tag, String message) {
        if (message.length() > 4000) {
            largeLog(tag, message);
        } else {
            Log.i(tag, message);
        }
        handleLogListener(tag, message);
    }

    private static void largeLog(String tag, String content) {
        while (content.length() > 4000) {
            Log.i(tag, content.substring(0, 4000) + "");
            content = content.substring(4000) + "";
        }
        Log.i(tag, content);
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
        handleLogListener(tag, message);
    }

    public static void w(String tag, String message, Throwable throwable) {
        Log.w(tag, message, throwable);
        handleLogListener(tag, message);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
        handleLogListener(tag, message);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        handleLogListener(tag, message);
    }

    public static void v(String tag, String message) {
        Log.v(tag, message);
        handleLogListener(tag, message);
    }

    public static void v(String tag, String message, Throwable throwable) {
        Log.v(tag, message, throwable);
        handleLogListener(tag, message);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        handleLogListener(tag, msg);
    }

    private static void handleLogListener(String tag, String msg) {
        if (mListener != null) {
            mListener.onLogPrint(tag, msg);
        }
    }

    public interface OnLogPrintListener {
        void onLogPrint(String tag, String msg);
    }


}
