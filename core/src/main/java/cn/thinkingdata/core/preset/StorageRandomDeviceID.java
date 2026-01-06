/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.preset;
import android.content.SharedPreferences;
import cn.thinkingdata.core.sp.SharedPreferencesStorage;
import java.util.concurrent.Future;
/**
 * StorageRandomDeviceID.
 * */
public class StorageRandomDeviceID extends SharedPreferencesStorage<String> {

    public StorageRandomDeviceID(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "randomDeviceID");
    }

    @Override
    public void saveOldData(SharedPreferences.Editor editor, String deviceID) {
        editor.putString(storageKey, deviceID);
    }

    @Override
    public void loadOldData(SharedPreferences sharedPreferences) {
        this.data = sharedPreferences.getString(this.storageKey, "");
    }

    @Override
    public void convertEncryptData(String convertData) {
        this.data = convertData;
    }

}