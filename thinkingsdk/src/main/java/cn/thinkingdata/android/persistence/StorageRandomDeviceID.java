package cn.thinkingdata.android.persistence;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.concurrent.Future;

import cn.thinkingdata.android.utils.TDUtils;

public class StorageRandomDeviceID extends SharedPreferencesStorage<String> {
    public StorageRandomDeviceID(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "randomDeviceID");
    }


    @Override
    String create() {
        return TDUtils.getRandomHEXValue(16);
    }

    @Override
    public void save(SharedPreferences.Editor editor, String deviceID) {
        editor.putString(this.storageKey, deviceID);
        editor.apply();
    }

    @Override
    public void load(SharedPreferences sharedPreferences) {
        String data = sharedPreferences.getString(this.storageKey, "");
        if (TextUtils.isEmpty(data)) {
            put(create());
        } else {
            this.data = data;
        }
    }
}