package cn.thinkingdata.android.persistence;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class StorageInstalledFlag extends SharedPreferencesStorage<Boolean> {

    public StorageInstalledFlag(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "installedFlag");
    }

    @Override
    protected void save(SharedPreferences.Editor editor, Boolean data) {
        editor.putBoolean(storageKey, data);
        editor.apply();
    }

    @Override
    protected void load(SharedPreferences sharedPreferences) {
        data = sharedPreferences.getBoolean(this.storageKey, false);
    }
}
