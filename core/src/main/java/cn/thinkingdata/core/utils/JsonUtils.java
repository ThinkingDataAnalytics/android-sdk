/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author liulongbing
 * @since 2024/5/17
 */
public class JsonUtils {
    public static Map<String, String> jsonToMap(JSONObject obj) {
        Map<String, String> maps = new HashMap<>();
        if (obj == null) return maps;
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = obj.opt(key);
            if (value != null) {
                maps.put(key, value.toString());
            }
        }
        return maps;
    }
}
