/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StoragePausePostFlag.
 *
 * @author liulongbing
 * @since 2022/3/25
 */
public class StoragePausePostFlag extends SharedPreferencesStorage<Boolean> {

    public StoragePausePostFlag(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "pausePostFlag");
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
