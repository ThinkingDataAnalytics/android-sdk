/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import android.text.TextUtils;
import java.util.concurrent.Future;
import org.json.JSONObject;

import cn.thinkingdata.core.sp.SharedPreferencesStorage;

/**
 * StorageSuperProperties.
 */
public class StorageSuperProperties extends SharedPreferencesStorage<JSONObject> {

    public StorageSuperProperties(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "superProperties");
    }

    @Override
    protected JSONObject create() {
        return new JSONObject();
    }

    @Override
    protected void saveOldData(SharedPreferences.Editor editor, JSONObject data) {
        editor.putString(this.storageKey, data.toString());
    }

    @Override
    protected void loadOldData(SharedPreferences sharedPreferences) {
        String properties = sharedPreferences.getString(this.storageKey, null);
        if (TextUtils.isEmpty(properties)) {
            put(create());
        } else {
            try {
                this.data = new JSONObject(properties);
            } catch (Exception e) {
                this.data = new JSONObject();
            }
        }
    }

    @Override
    protected void convertEncryptData(String convertData) {
        try {
            this.data = new JSONObject(convertData);
        } catch (Exception e) {
            this.data = new JSONObject();
        }
    }
}