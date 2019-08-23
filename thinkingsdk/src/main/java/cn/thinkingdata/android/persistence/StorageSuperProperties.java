package cn.thinkingdata.android.persistence;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Future;

public class StorageSuperProperties extends SharedPreferencesStorage <JSONObject> {
    public StorageSuperProperties(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "superProperties");
    }

    @Override
    JSONObject create() {
        return new JSONObject();
    }

    @Override
    void save(SharedPreferences.Editor editor, JSONObject data) {
        String stringData = (data == null) ? null : data.toString();
        editor.putString(this.storageKey, stringData);
        editor.apply();
    }

    @Override
    void load(SharedPreferences sharedPreferences) {
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