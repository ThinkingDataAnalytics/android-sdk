/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StorageIdentifyId.
 */
public class StorageIdentifyId extends SharedPreferencesStorage<String> {

    public StorageIdentifyId(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "identifyID");
    }

    @Override
    protected void saveOldData(SharedPreferences.Editor editor, String data) {
        editor.putString(storageKey, data);
    }

    @Override
    protected void loadOldData(SharedPreferences sharedPreferences) {
        String identifyId = sharedPreferences.getString(this.storageKey, null);
        if (!TextUtils.isEmpty(identifyId)) {
            this.data = identifyId;
        }
    }

    @Override
    protected void convertEncryptData(String convertData) {
        if (!TextUtils.isEmpty(convertData)) {
            this.data = convertData;
        }
    }

}
