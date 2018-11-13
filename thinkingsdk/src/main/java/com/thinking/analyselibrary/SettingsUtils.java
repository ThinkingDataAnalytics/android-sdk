package com.thinking.analyselibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingsUtils {
    public static void setUploadInterval(final Context context, final int newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(TDBuildConfig.PREF_DATA_UPLOADINTERVAL, newValue).apply();
    }

    public static int getUploadInterval(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(TDBuildConfig.PREF_DATA_UPLOADINTERVAL, 15000);
    }

    public static void setUpladSize(final Context context, final int newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(TDBuildConfig.PREF_DATA_UPLOADSIZE, newValue).apply();
    }

    public static int getUpladSize(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(TDBuildConfig.PREF_DATA_UPLOADSIZE, 20);
    }
}
