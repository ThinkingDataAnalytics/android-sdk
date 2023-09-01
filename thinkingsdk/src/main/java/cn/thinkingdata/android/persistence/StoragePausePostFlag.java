/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

/**
 * StoragePausePostFlag.
 *
 * @author liulongbing
 * @create 2022/3/25
 */
public class StoragePausePostFlag extends SharedPreferencesStorage<Boolean> {

    public StoragePausePostFlag(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "pausePostFlag");
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
