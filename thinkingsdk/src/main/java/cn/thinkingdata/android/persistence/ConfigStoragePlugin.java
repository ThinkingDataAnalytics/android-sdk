/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.persistence;

import android.content.Context;

/**
 * storage config
 *
 * @author liulongbing
 * @create 2022/9/7
 * @since
 */
public class ConfigStoragePlugin extends AbstractStoragePlugin {

    private static final String PREFERENCE_NAME_PREFIX = "cn.thinkingdata.android.config";

    public static final int DEFAULT_FLUSH_INTERVAL = 15000;
    public static final int DEFAULT_FLUSH_BULK_SIZE = 20;

    private StorageFlushInterval mFlushInterval;
    private StorageFlushBulkSize mFlushBulkSize;

    public ConfigStoragePlugin(Context context, String name) {
        super(context, PREFERENCE_NAME_PREFIX + "_" + name);
    }

    @Override
    protected void createStorage() {
        mFlushInterval = new StorageFlushInterval(storedSharedPrefs, DEFAULT_FLUSH_INTERVAL);
        mFlushBulkSize = new StorageFlushBulkSize(storedSharedPrefs, DEFAULT_FLUSH_BULK_SIZE);
    }

    @Override
    protected <T> SharedPreferencesStorage<T> getSharePreferenceStorage(LocalStorageType type) {
        switch (type) {
            case FLUSH_INTERVAL:
                return (SharedPreferencesStorage<T>) mFlushInterval;
            case FLUSH_SIZE:
                return (SharedPreferencesStorage<T>) mFlushBulkSize;
        }
        return null;
    }
}
