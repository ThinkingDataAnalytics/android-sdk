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
    protected String create() {
        return UUID.randomUUID().toString();
    }

    @Override
    protected void saveOldData(SharedPreferences.Editor editor, String data) {
        editor.putString(storageKey, data);
    }

    @Override
    protected void loadOldData(SharedPreferences sharedPreferences) {
        String randomId = sharedPreferences.getString(this.storageKey, null);
        if (TextUtils.isEmpty(randomId)) {
            put(create());
        } else {
            this.data = randomId;
        }
    }

    @Override
    protected void convertEncryptData(String convertData) {
        this.data = convertData;
    }

}