/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;

/**
 * StorageIdentifyId.
 */
public class StorageIdentifyId extends SharedPreferencesStorage<String> {
    public StorageIdentifyId(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "identifyID");
    }
}
