/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.concurrent.Future;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * Persists the current analytics session id as a UUID v7 string.
 */
public class StorageAnalyticsSessionId extends SharedPreferencesStorage<String> {

    StorageAnalyticsSessionId(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "analyticsSessionId");
    }

    @Override
    public void convertEncryptData(String convertData) {
        if (!TextUtils.isEmpty(convertData)) {
            this.data = convertData;
        }
    }

    @Override
    public void saveOldData(SharedPreferences.Editor editor, String data) {
        editor.putString(storageKey, data);
    }

    @Override
    public void loadOldData(SharedPreferences sharedPreferences) {
        try {
            String sessionId = sharedPreferences.getString(this.storageKey, null);
            if (!TextUtils.isEmpty(sessionId)) {
                this.data = sessionId;
            }
        } catch (Exception ignore) {
            this.data = null;
        }
    }
}
