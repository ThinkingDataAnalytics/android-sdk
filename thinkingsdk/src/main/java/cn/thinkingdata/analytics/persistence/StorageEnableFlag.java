/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StorageEnableFlag.
 * */
public class StorageEnableFlag extends SharedPreferencesStorage<Boolean> {

    public StorageEnableFlag(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "enableFlag");
    }

    @Override
    protected void save(SharedPreferences.Editor editor, Boolean data) {
        editor.putBoolean(storageKey, data);
        editor.apply();
    }

    @Override
    protected void load(SharedPreferences sharedPreferences) {
        data = sharedPreferences.getBoolean(this.storageKey, true);
    }
}
