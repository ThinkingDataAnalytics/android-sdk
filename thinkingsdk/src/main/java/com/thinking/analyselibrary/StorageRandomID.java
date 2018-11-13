package com.thinking.analyselibrary;

import android.content.SharedPreferences;

import java.util.UUID;
import java.util.concurrent.Future;

public class StorageRandomID extends SharedPreferencesStorage<String> {
    StorageRandomID(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, new StroageUnit<String>() {
            @Override
            public String create() {
                return UUID.randomUUID().toString();
            }

            @Override
            public String save(String item) {
                return item;
            }

            @Override
            public String get(String value) {
                return value;
            }
        }, "randomID");
    }
}
