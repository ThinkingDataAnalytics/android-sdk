/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * Persists the last event time of the current analytics session.
 */
public class StorageAnalyticsSessionLastEventTime extends SharedPreferencesStorage<Long> {

    private static final long DEFAULT_VALUE = -1L;

    StorageAnalyticsSessionLastEventTime(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "analyticsSessionLastEventTime");
    }

    @Override
    public void convertEncryptData(String convertData) {
        this.data = parse(convertData);
    }

    @Override
    public void saveOldData(SharedPreferences.Editor editor, Long data) {
        editor.putLong(storageKey, data);
    }

    @Override
    public void loadOldData(SharedPreferences sharedPreferences) {
        try {
            this.data = sharedPreferences.getLong(this.storageKey, DEFAULT_VALUE);
        } catch (Exception ignore) {
            this.data = DEFAULT_VALUE;
        }
    }

    private static long parse(String convertData) {
        try {
            return Long.parseLong(convertData);
        } catch (Exception ignore) {
            return DEFAULT_VALUE;
        }
    }
}
