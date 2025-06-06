/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StorageFlushBulkSize.
 * */
public class StorageFlushBulkSize extends SharedPreferencesStorage<Integer> {
    private final int mDefaultBulkSize;

    public StorageFlushBulkSize(Future<SharedPreferences> loadStoredPreferences, int defaultBulkSize) {
        super(loadStoredPreferences, "flushBulkSize");
        mDefaultBulkSize = defaultBulkSize;
    }

    @Override
    protected void saveOldData(SharedPreferences.Editor editor, Integer interval) {
        editor.putInt(storageKey, interval);
    }

    @Override
    protected void loadOldData(SharedPreferences sharedPreferences) {
        try {
            this.data = sharedPreferences.getInt(this.storageKey, mDefaultBulkSize);
        } catch (Exception e) {
            this.data = mDefaultBulkSize;
        }
    }

    @Override
    protected void convertEncryptData(String convertData) {
        this.data = Integer.parseInt(convertData);
    }
}
