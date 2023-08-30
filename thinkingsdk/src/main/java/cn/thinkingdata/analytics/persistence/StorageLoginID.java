/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

/**
 * StorageLoginID.
 * */
public class StorageLoginID extends SharedPreferencesStorage<String> {
    public StorageLoginID(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "loginID");
    }
}
