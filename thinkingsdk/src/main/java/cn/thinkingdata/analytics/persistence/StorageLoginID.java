/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StorageLoginID.
 * */
public class StorageLoginID extends SharedPreferencesStorage<String> {

    public StorageLoginID(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "loginID");
    }

    @Override
    public void saveOldData(SharedPreferences.Editor editor, String data) {
        editor.putString(storageKey, data);
    }

    @Override
    public void loadOldData(SharedPreferences sharedPreferences) {
        String loginId = sharedPreferences.getString(this.storageKey, null);
        if (!TextUtils.isEmpty(loginId)) {
            this.data = loginId;
        }
    }

    @Override
    public void convertEncryptData(String convertData) {
        if (!TextUtils.isEmpty(convertData)) {
            this.data = convertData;
        }
    }

}
