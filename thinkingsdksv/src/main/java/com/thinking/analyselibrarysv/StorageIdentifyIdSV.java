package com.thinking.analyselibrarysv;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class StorageIdentifyIdSV extends SharedPreferencesStorageSV<String> {
    StorageIdentifyIdSV(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, new StroageUnit<String>() {
            @Override
            public String create() {
                return null;
            }

            @Override
            public String save(String item) {
                return item;
            }

            @Override
            public String get(String value) {
                return value;
            }
        }, "identifyID");
    }
}
