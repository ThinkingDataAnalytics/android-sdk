/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

/**
 * StorageOptOutFlag.
 * */
public class StorageOptOutFlag extends SharedPreferencesStorage<Boolean> {

    public StorageOptOutFlag(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "optOutFlag");
    }

    @Override
    protected void save(SharedPreferences.Editor editor, Boolean data) {
        editor.putBoolean(storageKey, data);
        editor.apply();
    }

    @Override
    protected void load(SharedPreferences sharedPreferences) {
        data = sharedPreferences.getBoolean(this.storageKey, false);
    }
}
