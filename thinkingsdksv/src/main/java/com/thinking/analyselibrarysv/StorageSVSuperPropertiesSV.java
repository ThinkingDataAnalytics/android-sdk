package com.thinking.analyselibrarysv;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Future;

public class StorageSVSuperPropertiesSV extends SharedPreferencesStorageSV<JSONObject> {
    StorageSVSuperPropertiesSV(Future loadStoredPreferences) {
        super(loadStoredPreferences, new StroageUnit<JSONObject>() {
            @Override
            public JSONObject create() {
                return new JSONObject();
            }

            @Override
            public String save(JSONObject item) {
                return item.toString();
            }

            @Override
            public JSONObject get(String value) {
                try {
                    return new JSONObject(value);
                } catch (JSONException e) {
                    return null;
                }
            }
        }, "superProperties");
    }
}
