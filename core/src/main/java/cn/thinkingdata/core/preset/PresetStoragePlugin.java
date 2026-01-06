/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.preset;

import android.content.Context;

import cn.thinkingdata.core.sp.AbstractStoragePlugin;
import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 *
 * @author liulongbing
 * @create 2024/3/6
 * @since
 */
public class PresetStoragePlugin extends AbstractStoragePlugin {

    private static final String PREFERENCE_NAME = "com.thinkingdata.analyse";
    public static final int DEVICE_ID = 100;

    private StorageRandomDeviceID randomDeviceID;

    public PresetStoragePlugin(Context context) {
        super(context, PREFERENCE_NAME);
    }

    @Override
    public void createStorage(Context context) {
        randomDeviceID = new StorageRandomDeviceID(storedSharedPrefs);
    }

    @Override
    public <T> SharedPreferencesStorage<T> getSharePreferenceStorage(int type) {
        switch (type) {
            case DEVICE_ID:
                return ( SharedPreferencesStorage<T> ) randomDeviceID;
        }
        return null;
    }
}
