package com.thinking.analyselibrary.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.thinking.analyselibrary.TDBuildConfig;

public class SettingsUtils {
    /* 默认触发上传时间间隔，单位毫秒 */
    final static private int DEFAULT_UPLOAD_INTERVAL = 15000;
    /* 默认触发上传数据条数 */
    final static private int DEFAULT_DATA_UPLOAD_SIZE = 20;

    public static void setUploadInterval(final Context context, final int newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(TDBuildConfig.PREF_DATA_UPLOADINTERVAL, newValue).apply();
    }

    public static int getUploadInterval(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(TDBuildConfig.PREF_DATA_UPLOADINTERVAL, DEFAULT_UPLOAD_INTERVAL);
    }

    public static void setUploadSize(final Context context, final int newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(TDBuildConfig.PREF_DATA_UPLOADSIZE, newValue).apply();
    }

    public static int getUploadSize(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(TDBuildConfig.PREF_DATA_UPLOADSIZE, DEFAULT_DATA_UPLOAD_SIZE);
    }
}
