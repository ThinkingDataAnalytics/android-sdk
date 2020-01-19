package cn.thinkingdata.android.utils;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

public class PropertyUtils {

    private static final String TAG = "ThinkingAnalytics.PropertyUtils";
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z\\d_]{0,49}$", Pattern.CASE_INSENSITIVE);

    public static boolean isInvalidName(String string){
        return string == null || !KEY_PATTERN.matcher(string).matches();
    }

    public static boolean checkProperty(JSONObject properties){

        if(properties != null && TDLog.mEnableLog) {
            for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
                String key = (String) iterator.next();

                if (TextUtils.isEmpty(key)) {
                    TDLog.d(TAG, "Empty property name is not allowed.");
                    //return false;
                }

                if (!(KEY_PATTERN.matcher(key).matches())) {
                    TDLog.d(TAG, "Property name[" + key + "] is not valid. The property KEY must be string that starts with English letter, " +
                            "and contains letter, number, and '_'. The max length of the property KEY is 50. ");
                    //return false;
                }

                try {
                    Object value = properties.get(key);

                    if (!(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Date || value instanceof JSONArray)) {
                        TDLog.d(TAG, "Property value must be type String, Number, Boolean, Date, or JSONArray");
                        //return false;
                    }

                    /*
                    // The server has a default limitation for string length 2048.
                    if ((value instanceof String) && (((String) value).getBytes("UTF-8").length > 2048)) {
                        TDLog.d(TAG, "The value for key [" + key + "] is too long, we cut the value to 2048 bytes");
                        properties.put(key, new String(PropertyUtils.cutToBytes((String) value, 2048), "UTF-8"));
                    }
                    */

                    if (value instanceof Number) {
                        double number = ((Number) value).doubleValue();
                        if (number > 9999999999999.999 || number < -9999999999999.999) {
                            TDLog.d(TAG, "The number value [" + value + "] is invalid.");
                            //return false;
                        }
                    }
                } catch (JSONException e) {
                    TDLog.d(TAG, "Unexpected parameters." + e);
                    return false;
                }
            }
        }
        return true;
    }

    // cut string by byte limitations
    public static byte[] cutToBytes(String s, int charLimit) throws UnsupportedEncodingException {
        byte[] utf8 = s.getBytes("UTF-8");
        if (utf8.length <= charLimit) {
            return utf8;
        }
        if ((utf8[charLimit] & 0x80) == 0) {
            // the limit doesn't cut an UTF-8 sequence
            return Arrays.copyOf(utf8, charLimit);
        }
        int i = 0;
        while ((utf8[charLimit-i-1] & 0x80) > 0 && (utf8[charLimit-i-1] & 0x40) == 0) {
            ++i;
        }
        if ((utf8[charLimit-i-1] & 0x80) > 0) {
            // we have to skip the starter UTF-8 byte
            return Arrays.copyOf(utf8, charLimit-i-1);
        } else {
            // we passed all UTF-8 bytes
            return Arrays.copyOf(utf8, charLimit-i);
        }
    }
}
