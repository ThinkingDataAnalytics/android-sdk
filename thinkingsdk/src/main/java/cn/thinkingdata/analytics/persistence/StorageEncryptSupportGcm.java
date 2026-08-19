/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * Whether the receiver supports AES-GCM, resolved from the remote config field
 * {@code secret_key.mode}. Tri-state: -1 unknown, 0 unsupported, 1 supported.
 * */
public class StorageEncryptSupportGcm extends SharedPreferencesStorage<Integer> {

    private final int mDefaultValue;

    public StorageEncryptSupportGcm(Future<SharedPreferences> loadStoredPreferences, int defaultValue) {
        super(loadStoredPreferences, "encryptSupportGcm");
        mDefaultValue = defaultValue;
    }

    @Override
    public void saveOldData(SharedPreferences.Editor editor, Integer value) {
        editor.putInt(storageKey, value);
    }

    @Override
    public void loadOldData(SharedPreferences sharedPreferences) {
        try {
            this.data = sharedPreferences.getInt(this.storageKey, mDefaultValue);
        } catch (Exception e) {
            this.data = mDefaultValue;
        }
    }

    @Override
    public void convertEncryptData(String convertData) {
        try {
            this.data = Integer.parseInt(convertData);
        } catch (Exception e) {
            this.data = mDefaultValue;
        }
    }
}
