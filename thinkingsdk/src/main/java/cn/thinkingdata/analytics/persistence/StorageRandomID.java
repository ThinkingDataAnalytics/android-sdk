/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.UUID;
import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StorageRandomID.
 * */
public class StorageRandomID extends SharedPreferencesStorage<String> {

    public StorageRandomID(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "randomID");
    }

    @Override
    public String create() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void saveOldData(SharedPreferences.Editor editor, String data) {
        editor.putString(storageKey, data);
    }

    @Override
    public void loadOldData(SharedPreferences sharedPreferences) {
        String randomId = sharedPreferences.getString(this.storageKey, null);
        if (TextUtils.isEmpty(randomId)) {
            put(create());
        } else {
            this.data = randomId;
        }
    }

    @Override
    public void convertEncryptData(String convertData) {
        this.data = convertData;
    }

}