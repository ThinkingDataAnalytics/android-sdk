package com.thinking.analyselibrarysv;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

class StorageSVLoginIDSV extends SharedPreferencesStorageSV<String> {
    StorageSVLoginIDSV(Future<SharedPreferences> loadStoredPreferences) {
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
        }, "loginID");
    }
}
