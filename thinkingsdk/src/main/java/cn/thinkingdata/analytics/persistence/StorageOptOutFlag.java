/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StorageOptOutFlag.
 * */
public class StorageOptOutFlag extends SharedPreferencesStorage<Boolean> {

    public StorageOptOutFlag(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "optOutFlag");
    }

    @Override
    public void saveOldData(SharedPreferences.Editor editor, Boolean data) {
        editor.putBoolean(storageKey, data);
    }

    @Override
    public void loadOldData(SharedPreferences sharedPreferences) {
        try {
            this.data = sharedPreferences.getBoolean(this.storageKey, false);
        } catch (Exception ignore) {
            this.data = false;
        }
    }

    @Override
    public void convertEncryptData(String convertData) {
        this.data = Boolean.parseBoolean(convertData);
    }

}
