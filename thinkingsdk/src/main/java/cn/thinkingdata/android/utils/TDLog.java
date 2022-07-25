/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.utils;

import android.util.Log;

/**
 * 日志打印类.
 * */
public class TDLog {

    static volatile boolean mEnableLog = false;

    public static void setEnableLog(boolean enable) {
        mEnableLog = enable;
    }

    /**
     * < debug >.
     *
     * @param tag TAG
     * @param msg MSG
     */
    public static void d(String tag, String msg) {
        if (mEnableLog) {
            Log.d(tag, msg);
        }
    }

    /**
     * < info >.
     *
     * @param tag TAG
     * @param message MSG
     * @param throwable Throwable
     */
    public static void i(String tag, String message, Throwable throwable) {
        if (mEnableLog) {
            Log.i(tag, message, throwable);
        }
    }

    /**
     * < info >.
     *
     * @param tag TAG
     * @param tr Throwable
     */
    public static void i(String tag, Throwable tr) {
        if (mEnableLog) {
            try {
                Log.i(tag, "", tr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * < info >.
     *
     * @param tag TAG
     * @param message MSG
     */
    public static void i(String tag, String message) {
        if (mEnableLog) {
            if (message.length() > 4000) {
                largeLog(tag, message);
            } else {
                Log.i(tag, message);
            }
        }
    }

    private static void largeLog(String tag, String content) {
        while (content.length() > 4000) {
            Log.i(tag, content.substring(0, 4000) + "");
            content = content.substring(4000) + "";
        }
        Log.i(tag, content);
    }

    /**
     * < warning >.
     *
     * @param tag TAG
     * @param message MSG
     */
    public static void w(String tag, String message) {
        if (mEnableLog) {
            Log.w(tag, message);
        }
    }

    /**
     * < warning >.
     *
     * @param tag TAG
     * @param message MSG
     * @param throwable Throwable
     */
    public static void w(String tag, String message, Throwable throwable) {
        if (mEnableLog) {
            Log.w(tag, message, throwable);
        }
    }

    /**
     * < error >.
     *
     * @param tag TAG
     * @param message MSG
     */
    public static void e(String tag, String message) {
        Log.e(tag, message);
    }

    /**
     * < error >.
     *
     * @param tag TAG
     * @param message MSG
     * @param throwable Throwable
     */
    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }

    /**
     * < verbose >.
     *
     * @param tag TAG
     * @param message MSG
     */
    public static void v(String tag, String message) {
        if (mEnableLog) {
            Log.v(tag, message);
        }
    }

    /**
     * < verbose >.
     *
     * @param tag TAG
     * @param message MSG
     * @param throwable Throwable
     */
    public static void v(String tag, String message, Throwable throwable) {
        if (mEnableLog) {
            Log.v(tag, message, throwable);
        }
    }
}
