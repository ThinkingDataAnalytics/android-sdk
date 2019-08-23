package cn.thinkingdata.android.persistence;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class StorageFlushInterval extends SharedPreferencesStorage<Integer> {
    public StorageFlushInterval(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "flushInterval");
    }

    @Override
    void save(SharedPreferences.Editor editor, Integer interval) {
        editor.putInt(storageKey, interval);
        editor.apply();
    }

    /* 触发上传时间间隔，单位毫秒 */
    void load(SharedPreferences sharedPreferences) {
        data = sharedPreferences.getInt(this.storageKey, 15000);
    }
}
