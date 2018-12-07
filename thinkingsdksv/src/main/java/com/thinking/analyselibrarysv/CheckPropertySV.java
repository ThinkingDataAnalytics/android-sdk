package com.thinking.analyselibrarysv;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

public class CheckPropertySV {

    private static final String TAG = "ThinkingAnalyticsSDKSV";
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z\\d_#]{0,49}$", Pattern.CASE_INSENSITIVE);

    static boolean checkString(String string){
        if (string == null || !(KEY_PATTERN.matcher(string).matches()) || !(string instanceof String)) {
            return false;
        }

        return true;
    }

    static boolean checkProperty(JSONObject properties){

        if(properties != null) {
            for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
                String key = (String) iterator.next();

                if (null == key || key.length() < 1) {
                    TDLogSV.d(TAG, "key is null");
                    return false;
                }

                if (!(key instanceof String)) {
                    TDLogSV.d(TAG, "Property Key should by String");
                    return false;
                }

                if (!(KEY_PATTERN.matcher(key).matches())) {
                    TDLogSV.d(TAG, "property name[" + key + "] is not valid");
                    return false;

                }

                try {
                    Object value = properties.get(key);

                    if (!(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Date)) {
                        TDLogSV.d(TAG, "property values must be String,Number,Boolean,Date");
                        return false;
                    }

                    if (value instanceof String && ((String) value).length() > 5000) {
                        TDLogSV.d(TAG, "The value " + value + " is too long");
                        return false;
                    }

                    if (value instanceof Number) {
                        Double number = ((Number) value).doubleValue();
                        if (number > 9999999999999.999 || number < -9999999999999.999) {
                            TDLogSV.d(TAG, "number value is not valid.");
                            return false;
                        }
                    }
                } catch (JSONException e) {
                    TDLogSV.d(TAG, "Unexpected parameters." + e);
                    return false;
                }
            }
        }
        return true;
    }

}
