package cn.thinkingdata.android.persistence;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class StorageFlushBulkSize extends SharedPreferencesStorage<Integer> {
    public StorageFlushBulkSize(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "flushBulkSize");
    }

    @Override
    void save(SharedPreferences.Editor editor, Integer interval) {
        editor.putInt(storageKey, interval);
        editor.apply();
    }

    /* 触发上传数据条数 */
    void load(SharedPreferences sharedPreferences) {
        data = sharedPreferences.getInt(this.storageKey, 20);
    }
}
