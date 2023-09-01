/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.persistence;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/11/29
 * @since
 */
public class StorageSessionIdIndex extends SharedPreferencesStorage<Integer> {

    StorageSessionIdIndex(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "sessionId");
    }

    @Override
    void save(SharedPreferences.Editor editor, Integer data) {
        editor.putInt(storageKey, data);
        editor.apply();
    }

    @Override
    void load(SharedPreferences sharedPreferences) {
        data = sharedPreferences.getInt(this.storageKey, 0);
    }
}
