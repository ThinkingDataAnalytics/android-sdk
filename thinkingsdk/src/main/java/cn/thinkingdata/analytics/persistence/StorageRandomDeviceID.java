/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;

import cn.thinkingdata.analytics.utils.TDUtils;

import java.util.concurrent.Future;

/**
 * StorageRandomDeviceID.
 * */
public class StorageRandomDeviceID extends SharedPreferencesStorage<String> {

    public StorageRandomDeviceID(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "randomDeviceID");
    }

    @Override
    public String create() {
        return TDUtils.getRandomHEXValue(16);
    }

    @Override
    public void save(SharedPreferences.Editor editor, String deviceID) {
        editor.putString(this.storageKey, deviceID);
        editor.apply();
    }

    @Override
    public void load(SharedPreferences sharedPreferences) {
        this.data = sharedPreferences.getString(this.storageKey, "");
    }
}