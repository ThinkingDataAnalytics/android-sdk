/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.core.utils;

/**
 * Log printing class.
 */
public class TDLog {

    public static volatile boolean mEnableLog = false;

    static volatile boolean mEnableLogInner = false;

    public static void setEnableLogInner(boolean enableLogInner) {
        mEnableLogInner = enableLogInner;
    }

    public static void setEnableLog(boolean enable) {
        if (mEnableLogInner) {
            mEnableLog = true;
        } else {
            mEnableLog = enable;
        }
    }

    public static boolean getEnableLog() {
        return mEnableLog;
    }

    /**
     * debug
     *
     * @param tag TAG
     * @param msg MSG
     */
    public static void d(String tag, String msg) {
        if (mEnableLog) {
            LogUtil.d(tag, msg);
        }
    }

    /**
     * info
     *
     * @param tag       TAG
     * @param message   MSG
     * @param throwable Throwable
     */
    public static void i(String tag, String message, Throwable throwable) {
        if (mEnableLog) {
            LogUtil.i(tag, message, throwable);
        }
    }

    /**
     * info
     *
     * @param tag TAG
     * @param tr  Throwable
     */
    public static void i(String tag, Throwable tr) {
        if (mEnableLog) {
            LogUtil.i(tag, "", tr);
        }
    }

    /**
     * info
     *
     * @param tag     TAG
     * @param message MSG
     */
    public static void i(String tag, String message) {
        if (mEnableLog) {
            LogUtil.i(tag, message);
        }
    }

    /**
     * warning
     *
     * @param tag     TAG
     * @param message MSG
     */
    public static void w(String tag, String message) {
        if (mEnableLog) {
            LogUtil.w(tag, message);
        }
    }

    /**
     * warning
     *
     * @param tag       TAG
     * @param message   MSG
     * @param throwable Throwable
     */
    public static void w(String tag, String message, Throwable throwable) {
        if (mEnableLog) {
            LogUtil.w(tag, message, throwable);
        }
    }

    /**
     * error
     *
     * @param tag     TAG
     * @param message MSG
     */
    public static void e(String tag, String message) {
        if (mEnableLog) {
            LogUtil.e(tag, message);
        }
    }

    /**
     * error
     *
     * @param tag       TAG
     * @param message   MSG
     * @param throwable Throwable
     */
    public static void e(String tag, String message, Throwable throwable) {
        if (mEnableLog) {
            LogUtil.e(tag, message, throwable);
        }
    }

    /**
     * verbose
     *
     * @param tag     TAG
     * @param message MSG
     */
    public static void v(String tag, String message) {
        if (mEnableLog) {
            LogUtil.v(tag, message);
        }
    }

    /**
     * verbose
     *
     * @param tag       TAG
     * @param message   MSG
     * @param throwable Throwable
     */
    public static void v(String tag, String message, Throwable throwable) {
        if (mEnableLog) {
            LogUtil.v(tag, message, throwable);
        }
    }
}
