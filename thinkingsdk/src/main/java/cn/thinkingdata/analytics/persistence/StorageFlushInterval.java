/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

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
    void save(SharedPreferences.Editor editor, Integer interval) {
        editor.putInt(storageKey, interval);
        editor.apply();
    }

    void load(SharedPreferences sharedPreferences) {
        data = sharedPreferences.getInt(this.storageKey, mDefaultFlushInterval);
    }
}
