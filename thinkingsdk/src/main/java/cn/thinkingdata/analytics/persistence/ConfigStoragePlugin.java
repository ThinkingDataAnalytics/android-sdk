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

    /**
     * The receiver has not told us whether it supports AES-GCM yet.
     */
    public static final int GCM_UNKNOWN = -1;
    /**
     * The receiver does not support AES-GCM, data must be reported in plaintext.
     */
    public static final int GCM_UNSUPPORTED = 0;
    /**
     * The receiver supports AES-GCM.
     */
    public static final int GCM_SUPPORTED = 1;

    private StorageFlushInterval mFlushInterval;
    private StorageFlushBulkSize mFlushBulkSize;
    private StorageEncryptSupportGcm mEncryptSupportGcm;

    public ConfigStoragePlugin(Context context, String name) {
        super(context, PREFERENCE_NAME_PREFIX + "_" + name);
    }

    @Override
    public void createStorage(Context context) {
        mFlushInterval = new StorageFlushInterval(storedSharedPrefs, DEFAULT_FLUSH_INTERVAL);
        mFlushBulkSize = new StorageFlushBulkSize(storedSharedPrefs, DEFAULT_FLUSH_BULK_SIZE);
        mEncryptSupportGcm = new StorageEncryptSupportGcm(storedSharedPrefs, GCM_UNKNOWN);
    }

    @Override
    public <T> SharedPreferencesStorage<T> getSharePreferenceStorage(int type) {
        switch (type) {
            case LocalStorageType.FLUSH_INTERVAL:
                return (SharedPreferencesStorage<T>) mFlushInterval;
            case LocalStorageType.FLUSH_SIZE:
                return (SharedPreferencesStorage<T>) mFlushBulkSize;
            case LocalStorageType.ENCRYPT_SUPPORT_GCM:
                return (SharedPreferencesStorage<T>) mEncryptSupportGcm;
        }
        return null;
    }
}
