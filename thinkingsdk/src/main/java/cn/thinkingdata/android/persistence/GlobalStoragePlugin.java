/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.persistence;

import android.content.Context;

/**
 * 全局配置，所有实例
 *
 * @author liulongbing
 * @create 2022/9/7
 * @since
 */
public class GlobalStoragePlugin extends AbstractStoragePlugin {

    private static final String PREFERENCE_NAME = "com.thinkingdata.analyse";
    private StorageLoginID sOldLoginId;
    private StorageRandomID sRandomID;
    private StorageLastInstallTime storageLastInstallTime;
    private StorageRandomDeviceID randomDeviceID;

    public GlobalStoragePlugin(Context context) {
        super(context, PREFERENCE_NAME);
    }

    @Override
    protected void createStorage() {
        sRandomID = new StorageRandomID(storedSharedPrefs);
        sOldLoginId = new StorageLoginID(storedSharedPrefs);
        storageLastInstallTime = new StorageLastInstallTime(storedSharedPrefs);
        randomDeviceID = new StorageRandomDeviceID(storedSharedPrefs);
    }

    @Override
    protected <T> SharedPreferencesStorage<T> getSharePreferenceStorage(LocalStorageType type) {
        switch (type){
            case LOGIN_ID:
                return (SharedPreferencesStorage<T>) sOldLoginId;
            case RANDOM_ID:
                return (SharedPreferencesStorage<T>) sRandomID;
            case LAST_INSTALL:
                return (SharedPreferencesStorage<T>) storageLastInstallTime;
            case DEVICE_ID:
                return ( SharedPreferencesStorage<T> ) randomDeviceID;
        }
        return null;
    }
}
