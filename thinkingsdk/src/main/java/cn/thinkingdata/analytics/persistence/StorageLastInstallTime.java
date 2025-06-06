/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StorageLastInstallTime.
 */
public class StorageLastInstallTime extends SharedPreferencesStorage<Long> {

    public StorageLastInstallTime(String prefix, Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, prefix + "_lastInstallTime");
    }

    @Override
    protected void saveOldData(SharedPreferences.Editor editor, Long data) {
        editor.putLong(this.storageKey, data);
    }

    @Override
    protected void loadOldData(SharedPreferences sharedPreferences) {
        try {
            this.data = sharedPreferences.getLong(this.storageKey, 0L);
        } catch (Exception ignore) {
            this.data = 0L;
        }
    }

    @Override
    protected void convertEncryptData(String convertData) {
        this.data = Long.parseLong(convertData);
    }

}