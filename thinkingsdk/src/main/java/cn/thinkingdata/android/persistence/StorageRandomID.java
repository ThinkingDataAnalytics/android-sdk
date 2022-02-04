package cn.thinkingdata.android.persistence;

import android.content.SharedPreferences;

import java.util.UUID;
import java.util.concurrent.Future;

public class StorageRandomID extends SharedPreferencesStorage<String> {
    public StorageRandomID(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "randomID");
    }

    @Override
    String create() {
        return UUID.randomUUID().toString();
    }
}