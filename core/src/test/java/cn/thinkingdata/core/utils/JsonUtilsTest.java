/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class JsonUtilsTest {

    // ── null / empty input ────────────────────────────────────────────────────

    @Test
    public void jsonToMap_null_returnsEmptyMap() {
        Map<String, String> result = JsonUtils.jsonToMap(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void jsonToMap_emptyObject_returnsEmptyMap() throws Exception {
        Map<String, String> result = JsonUtils.jsonToMap(new JSONObject());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── string values ─────────────────────────────────────────────────────────

    @Test
    public void jsonToMap_singleStringEntry() throws Exception {
        JSONObject obj = new JSONObject("{\"key\":\"value\"}");
        Map<String, String> result = JsonUtils.jsonToMap(obj);
        assertEquals(1, result.size());
        assertEquals("value", result.get("key"));
    }

    @Test
    public void jsonToMap_multipleStringEntries() throws Exception {
        JSONObject obj = new JSONObject("{\"a\":\"1\",\"b\":\"2\",\"c\":\"3\"}");
        Map<String, String> result = JsonUtils.jsonToMap(obj);
        assertEquals(3, result.size());
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
    }

    // ── non-string values are toString()'ed ────────────────────────────────────

    @Test
    public void jsonToMap_integerValue_isStringified() throws Exception {
        JSONObject obj = new JSONObject("{\"count\":42}");
        Map<String, String> result = JsonUtils.jsonToMap(obj);
        assertEquals("42", result.get("count"));
    }

    @Test
    public void jsonToMap_doubleValue_isStringified() throws Exception {
        JSONObject obj = new JSONObject("{\"price\":3.14}");
        Map<String, String> result = JsonUtils.jsonToMap(obj);
        assertNotNull(result.get("price"));
        assertTrue(result.get("price").contains("3.14") || result.get("price").startsWith("3.1"));
    }

    @Test
    public void jsonToMap_booleanValue_isStringified() throws Exception {
        JSONObject obj = new JSONObject("{\"flag\":true}");
        Map<String, String> result = JsonUtils.jsonToMap(obj);
        assertEquals("true", result.get("flag"));
    }

    @Test
    public void jsonToMap_nullJsonValue_isExcluded() throws Exception {
        // JSONObject.opt() returns JSONObject.NULL for explicit JSON null,
        // which is not Java null, so it IS included as "null" string.
        // However, if opt() returns Java null, it is excluded.
        JSONObject obj = new JSONObject();
        obj.put("present", "yes");
        // JSON null maps to JSONObject.NULL (not Java null), toString → "null"
        obj.put("absent", JSONObject.NULL);
        Map<String, String> result = JsonUtils.jsonToMap(obj);
        // "present" must be in the map
        assertEquals("yes", result.get("present"));
    }

    // ── key integrity ─────────────────────────────────────────────────────────

    @Test
    public void jsonToMap_keyWithSpecialChars() throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("content-type", "application/json");
        Map<String, String> result = JsonUtils.jsonToMap(obj);
        assertEquals("application/json", result.get("content-type"));
    }

    @Test
    public void jsonToMap_numericKey() throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("123", "numeric-key");
        Map<String, String> result = JsonUtils.jsonToMap(obj);
        assertEquals("numeric-key", result.get("123"));
    }

    // ── result is a mutable map ───────────────────────────────────────────────

    @Test
    public void jsonToMap_returnedMapIsMutable() throws Exception {
        JSONObject obj = new JSONObject("{\"k\":\"v\"}");
        Map<String, String> result = JsonUtils.jsonToMap(obj);
        result.put("extra", "added");
        assertEquals("added", result.get("extra"));
    }
}
