/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;

import android.content.Context;

import cn.thinkingdata.core.sp.AbstractStoragePlugin;
import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * storage config
 *
 * @author liulongbing
 * @since 2022/9/7
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
    protected void createStorage(Context context) {
        mFlushInterval = new StorageFlushInterval(storedSharedPrefs, DEFAULT_FLUSH_INTERVAL);
        mFlushBulkSize = new StorageFlushBulkSize(storedSharedPrefs, DEFAULT_FLUSH_BULK_SIZE);
    }

    @Override
    protected <T> SharedPreferencesStorage<T> getSharePreferenceStorage(int type) {
        switch (type) {
            case LocalStorageType.FLUSH_INTERVAL:
                return (SharedPreferencesStorage<T>) mFlushInterval;
            case LocalStorageType.FLUSH_SIZE:
                return (SharedPreferencesStorage<T>) mFlushBulkSize;
        }
        return null;
    }
}
