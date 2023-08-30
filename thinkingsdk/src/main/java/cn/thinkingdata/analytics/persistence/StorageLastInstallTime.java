/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

/**
 * StorageLastInstallTime.
 * */
public class StorageLastInstallTime extends SharedPreferencesStorage<Long> {

    public StorageLastInstallTime(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "lastInstallTime");
    }

    @Override
    public Long create() {
        return 0L;
    }

    @Override
    public void save(SharedPreferences.Editor editor, Long installTime) {
        editor.putLong(this.storageKey, installTime);
        editor.apply();
    }

    @Override
    public void load(SharedPreferences sharedPreferences) {
        this.data = sharedPreferences.getLong(this.storageKey, 0L);
    }
}