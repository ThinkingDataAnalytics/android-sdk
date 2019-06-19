package com.thinking.analyselibrary.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

public class PropertyUtils {

    private static final String TAG = "ThinkingAnalyticsSDK";
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z\\d_#]{0,49}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_KEY_PATTERN = Pattern.compile("^#referrer$|^#referrer_host$|^#url$|^#url_path$|^#title$|^[a-zA-Z][a-zA-Z\\d_#]{0,49}$", Pattern.CASE_INSENSITIVE);

    public static boolean checkString(String string){
        if (string == null || !(KEY_PATTERN.matcher(string).matches()) || !(string instanceof String)) {
            return false;
        }

        return true;
    }

    public static boolean checkJsString(String string){
        if (string == null || !(JS_KEY_PATTERN.matcher(string).matches()) || !(string instanceof String)) {
            return false;
        }

        return true;
    }

    public static boolean checkProperty(JSONObject properties){

        if(properties != null) {
            for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
                String key = (String) iterator.next();

                if (null == key || key.length() < 1) {
                    TDLog.d(TAG, "key is null");
                    return false;
                }

                if (!(key instanceof String)) {
                    TDLog.d(TAG, "Property Key should by String");
                    return false;
                }

                if (!(KEY_PATTERN.matcher(key).matches())) {
                    TDLog.d(TAG, "property name[" + key + "] is not valid");
                    return false;

                }

                try {
                    Object value = properties.get(key);

                    if (!(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Date)) {
                        TDLog.d(TAG, "property values must be String,Number,Boolean,Date");
                        return false;
                    }

                    if (value instanceof String && ((String) value).getBytes("UTF-8").length > 2048) {
                        TDLog.d(TAG, "The value " + value + " is too long");
                        return false;
                    }

                    if (value instanceof Number) {
                        Double number = ((Number) value).doubleValue();
                        if (number > 9999999999999.999 || number < -9999999999999.999) {
                            TDLog.d(TAG, "number value is not valid.");
                            return false;
                        }
                    }
                } catch (JSONException e) {
                    TDLog.d(TAG, "Unexpected parameters." + e);
                    return false;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

}
