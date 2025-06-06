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
    protected void convertEncryptData(String convertData) {
        this.data = Boolean.parseBoolean(convertData);
    }

    @Override
    protected void saveOldData(SharedPreferences.Editor editor, Boolean data) {
        editor.putBoolean(storageKey, data);
    }

    @Override
    protected void loadOldData(SharedPreferences sharedPreferences) {
        try {
            this.data = sharedPreferences.getBoolean(this.storageKey, true);
        } catch (Exception ignore) {
            this.data = true;
        }
    }
}
