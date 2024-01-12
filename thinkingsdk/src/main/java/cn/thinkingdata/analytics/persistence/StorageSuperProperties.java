/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.persistence;

import android.content.SharedPreferences;
import java.util.concurrent.Future;
import org.json.JSONException;
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
    protected void save(SharedPreferences.Editor editor, JSONObject data) {
        String stringData = (data == null) ? null : data.toString();
        editor.putString(this.storageKey, stringData);
        editor.apply();
    }

    @Override
    protected void load(SharedPreferences sharedPreferences) {
        String data = sharedPreferences.getString(this.storageKey, null);
        if (data == null) {
            put(create());
        } else {
            try {
                this.data = new JSONObject(data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}