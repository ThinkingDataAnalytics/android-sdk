package cn.thinkingdata.android.android.persistence;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class StorageLoginID extends SharedPreferencesStorage<String> {
    public StorageLoginID(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences,"loginID");
    }
}
