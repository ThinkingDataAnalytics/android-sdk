/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;

import android.content.Context;

import cn.thinkingdata.analytics.utils.TDUtils;
import cn.thinkingdata.core.preset.StorageRandomDeviceID;
import cn.thinkingdata.core.sp.AbstractStoragePlugin;
import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * @author liulongbing
 * @since 2022/9/7
 */
public class GlobalStoragePlugin extends AbstractStoragePlugin {

    private static final String PREFERENCE_NAME = "com.thinkingdata.analyse";
    private StorageLoginID sOldLoginId;
    private StorageRandomID sRandomID;
    private StorageLastInstallTime storageLastInstallTime;

    public GlobalStoragePlugin(Context context) {
        super(context, PREFERENCE_NAME);
    }

    @Override
    public void createStorage(Context context) {
        sRandomID = new StorageRandomID(storedSharedPrefs);
        sOldLoginId = new StorageLoginID(storedSharedPrefs);
        String currentProcessName = TDUtils.getCurrentProcessName(context);
        storageLastInstallTime = new StorageLastInstallTime(currentProcessName, storedSharedPrefs);
    }

    @Override
    public <T> SharedPreferencesStorage<T> getSharePreferenceStorage(int type) {
        switch (type) {
            case LocalStorageType.LOGIN_ID:
                return ( SharedPreferencesStorage<T> ) sOldLoginId;
            case LocalStorageType.RANDOM_ID:
                return ( SharedPreferencesStorage<T> ) sRandomID;
            case LocalStorageType.LAST_INSTALL:
                return ( SharedPreferencesStorage<T> ) storageLastInstallTime;
        }
        return null;
    }
}
