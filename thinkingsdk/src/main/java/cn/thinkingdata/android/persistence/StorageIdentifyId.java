package cn.thinkingdata.android.persistence;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class StorageIdentifyId extends SharedPreferencesStorage <String>  {
    public StorageIdentifyId(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "identifyID");
    }
}
