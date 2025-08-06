/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;
import android.content.Context;

import cn.thinkingdata.core.sp.AbstractStoragePlugin;
import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * @author liulongbing
 * @since 2022/9/7
 */
public class CommonStoragePlugin extends AbstractStoragePlugin {

    private static final String PREFERENCE_NAME = "com.thinkingdata.analyse";

    private  StorageLoginID mLoginId;
    private  StorageIdentifyId mIdentifyId;
    private  StorageEnableFlag mEnableFlag;
    private  StorageOptOutFlag mOptOutFlag;
    private  StoragePausePostFlag mPausePostFlag;
    private  StorageSuperProperties mSuperProperties;
    private StorageSessionIdIndex mSessionIndex;

    public CommonStoragePlugin(Context context, String name) {
        super(context, PREFERENCE_NAME + "_" + name);
    }

    @Override
    public void createStorage(Context context) {
        mLoginId = new StorageLoginID(storedSharedPrefs);
        mIdentifyId = new StorageIdentifyId(storedSharedPrefs);
        mSuperProperties = new StorageSuperProperties(storedSharedPrefs);
        mOptOutFlag = new StorageOptOutFlag(storedSharedPrefs);
        mEnableFlag = new StorageEnableFlag(storedSharedPrefs);
        mPausePostFlag = new StoragePausePostFlag(storedSharedPrefs);
        mSessionIndex = new StorageSessionIdIndex(storedSharedPrefs);
    }

    @Override
    public <T> SharedPreferencesStorage<T> getSharePreferenceStorage(int type) {
        switch (type){
            case LocalStorageType.LOGIN_ID:
                return (SharedPreferencesStorage<T>) mLoginId;
            case LocalStorageType.IDENTIFY:
                return (SharedPreferencesStorage<T>) mIdentifyId;
            case LocalStorageType.SUPER_PROPERTIES:
                return (SharedPreferencesStorage<T>) mSuperProperties;
            case LocalStorageType.OPT_OUT:
                return (SharedPreferencesStorage<T>) mOptOutFlag;
            case LocalStorageType.ENABLE:
                return (SharedPreferencesStorage<T>) mEnableFlag;
            case LocalStorageType.PAUSE_POST:
                return (SharedPreferencesStorage<T>) mPausePostFlag;
            case LocalStorageType.SESSION_ID:
                return (SharedPreferencesStorage<T>) mSessionIndex;
        }
        return null;
    }
}
