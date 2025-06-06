/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StorageFlushInterval.
 * */
public class StorageFlushInterval extends SharedPreferencesStorage<Integer> {
    private final int mDefaultFlushInterval;

    public StorageFlushInterval(Future<SharedPreferences> loadStoredPreferences, int defaultFlushInterval) {
        super(loadStoredPreferences, "flushInterval");
        mDefaultFlushInterval = defaultFlushInterval;
    }

    @Override
    protected void saveOldData(SharedPreferences.Editor editor, Integer interval) {
        editor.putInt(storageKey, interval);
    }

    @Override
    protected void loadOldData(SharedPreferences sharedPreferences) {
        try {
            this.data = sharedPreferences.getInt(this.storageKey, mDefaultFlushInterval);
        } catch (Exception e) {
            this.data = mDefaultFlushInterval;
        }
    }

    @Override
    protected void convertEncryptData(String convertData) {
        this.data = Integer.parseInt(convertData);
    }
}
