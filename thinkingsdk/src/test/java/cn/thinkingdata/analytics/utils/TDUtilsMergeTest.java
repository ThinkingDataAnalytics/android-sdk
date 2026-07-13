/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

import org.junit.Test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class TDUtilsMergeTest {

    // ── mergeJSONObject — basic copy ──────────────────────────────────────────

    @Test
    public void merge_stringValue_copiedToDest() throws JSONException {
        JSONObject source = new JSONObject();
        source.put("name", "Alice");
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, null);

        assertEquals("Alice", dest.getString("name"));
    }

    @Test
    public void merge_integerValue_copiedToDest() throws JSONException {
        JSONObject source = new JSONObject();
        source.put("score", 42);
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, null);

        assertEquals(42, dest.getInt("score"));
    }

    @Test
    public void merge_booleanValue_copiedToDest() throws JSONException {
        JSONObject source = new JSONObject();
        source.put("active", true);
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, null);

        assertTrue(dest.getBoolean("active"));
    }

    @Test
    public void merge_multipleScalarValues_allCopied() throws JSONException {
        JSONObject source = new JSONObject();
        source.put("str", "hello");
        source.put("num", 3.14);
        source.put("flag", false);
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, null);

        assertEquals("hello", dest.getString("str"));
        assertEquals(3.14, dest.getDouble("num"), 1e-9);
        assertFalse(dest.getBoolean("flag"));
    }

    // ── mergeJSONObject — overwrite ───────────────────────────────────────────

    @Test
    public void merge_sourceOverwritesExistingDestKey() throws JSONException {
        JSONObject source = new JSONObject();
        source.put("key", "new_value");
        JSONObject dest = new JSONObject();
        dest.put("key", "old_value");

        TDUtils.mergeJSONObject(source, dest, null);

        assertEquals("new_value", dest.getString("key"));
    }

    @Test
    public void merge_destKeysNotInSource_preserved() throws JSONException {
        JSONObject source = new JSONObject();
        source.put("from_source", "a");
        JSONObject dest = new JSONObject();
        dest.put("existing", "b");

        TDUtils.mergeJSONObject(source, dest, null);

        assertEquals("a", dest.getString("from_source"));
        assertEquals("b", dest.getString("existing"));
    }

    // ── mergeJSONObject — null guards ─────────────────────────────────────────

    @Test
    public void merge_nullSource_destUnchanged() throws JSONException {
        JSONObject dest = new JSONObject();
        dest.put("key", "value");

        TDUtils.mergeJSONObject(null, dest, null);

        assertEquals("value", dest.getString("key"));
        assertEquals(1, dest.length());
    }

    @Test
    public void merge_nullDest_noException() throws JSONException {
        JSONObject source = new JSONObject();
        source.put("key", "value");

        // Should return silently without NPE
        TDUtils.mergeJSONObject(source, null, null);
    }

    @Test
    public void merge_bothNull_noException() throws JSONException {
        TDUtils.mergeJSONObject(null, null, null);
    }

    @Test
    public void merge_emptySource_destUnchanged() throws JSONException {
        JSONObject source = new JSONObject();
        JSONObject dest = new JSONObject();
        dest.put("existing", "keep");

        TDUtils.mergeJSONObject(source, dest, null);

        assertEquals("keep", dest.getString("existing"));
        assertEquals(1, dest.length());
    }

    // ── mergeJSONObject — Date formatting ────────────────────────────────────

    @Test
    public void merge_dateValue_formattedToPattern() throws JSONException {
        Date date = new Date(0); // epoch: 1970-01-01
        JSONObject source = new JSONObject();
        source.put("event_time", date);
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, null);

        String result = dest.getString("event_time");
        assertNotNull(result);
        assertTrue(
                "Date must be formatted as yyyy-MM-dd HH:mm:ss.SSS, got: " + result,
                Pattern.compile(TDConstants.TIME_CHECK_PATTERN).matcher(result).find()
        );
    }

    @Test
    public void merge_dateWithTimezone_usesCorrectOffset() throws JSONException {
        // Use a fixed epoch so we know the exact expected output
        Date date = new Date(0); // epoch = 1970-01-01 00:00:00 UTC
        TimeZone utcPlus8 = TimeZone.getTimeZone("GMT+8");

        JSONObject source = new JSONObject();
        source.put("ts", date);
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, utcPlus8);

        String expected = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA)
                .format(new Date(0 + 8 * 3600 * 1000L)); // offset is applied in formatter
        SimpleDateFormat fmt = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
        fmt.setTimeZone(utcPlus8);
        String expectedFormatted = fmt.format(date);

        assertEquals(expectedFormatted, dest.getString("ts"));
    }

    @Test
    public void merge_dateWithNullTimezone_usesSystemDefault() throws JSONException {
        Date date = new Date(1_000_000_000_000L);
        JSONObject source = new JSONObject();
        source.put("ts", date);
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, null);

        String result = dest.getString("ts");
        assertTrue(
                "Date formatted without timezone must still match pattern",
                Pattern.compile(TDConstants.TIME_CHECK_PATTERN).matcher(result).find()
        );
    }

    // ── mergeJSONObject — JSONArray values ────────────────────────────────────

    @Test
    public void merge_jsonArrayValue_preservedInDest() throws JSONException {
        JSONArray arr = new JSONArray();
        arr.put("item1");
        arr.put(123);
        arr.put(true);

        JSONObject source = new JSONObject();
        source.put("tags", arr);
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, null);

        JSONArray result = dest.getJSONArray("tags");
        assertEquals(3, result.length());
        assertEquals("item1", result.getString(0));
        assertEquals(123, result.getInt(1));
        assertTrue(result.getBoolean(2));
    }

    @Test
    public void merge_emptyJsonArray_preservedInDest() throws JSONException {
        JSONObject source = new JSONObject();
        source.put("empty", new JSONArray());
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, null);

        assertEquals(0, dest.getJSONArray("empty").length());
    }

    // ── mergeJSONObject — JSONObject values ──────────────────────────────────

    @Test
    public void merge_jsonObjectValue_copiedToDest() throws JSONException {
        JSONObject nested = new JSONObject();
        nested.put("x", 1);
        nested.put("y", 2);

        JSONObject source = new JSONObject();
        source.put("point", nested);
        JSONObject dest = new JSONObject();

        TDUtils.mergeJSONObject(source, dest, null);

        JSONObject resultNested = dest.getJSONObject("point");
        assertEquals(1, resultNested.getInt("x"));
        assertEquals(2, resultNested.getInt("y"));
    }

    // ── mergeNestedJSONObject — new key creation ──────────────────────────────

    @Test
    public void mergeNested_newKeyInSource_createdInDest() throws JSONException {
        JSONObject innerSource = new JSONObject();
        innerSource.put("prop", "value");

        JSONObject source = new JSONObject();
        source.put("nested", innerSource);

        JSONObject dest = new JSONObject();

        TDUtils.mergeNestedJSONObject(source, dest, null);

        assertTrue(dest.has("nested"));
        assertEquals("value", dest.getJSONObject("nested").getString("prop"));
    }

    @Test
    public void mergeNested_existingKeyInDest_innerPropertiesMerged() throws JSONException {
        JSONObject innerSource = new JSONObject();
        innerSource.put("new_prop", "from_source");

        JSONObject source = new JSONObject();
        source.put("group", innerSource);

        JSONObject innerDest = new JSONObject();
        innerDest.put("existing_prop", "keep_this");

        JSONObject dest = new JSONObject();
        dest.put("group", innerDest);

        TDUtils.mergeNestedJSONObject(source, dest, null);

        JSONObject resultGroup = dest.getJSONObject("group");
        assertEquals("from_source", resultGroup.getString("new_prop"));
        assertEquals("keep_this", resultGroup.getString("existing_prop"));
    }

    @Test
    public void mergeNested_sourceScalarValuesIgnored() throws JSONException {
        // mergeNestedJSONObject only processes top-level keys whose values are JSONObject;
        // scalar top-level values in source are skipped
        JSONObject source = new JSONObject();
        source.put("scalar", "should_be_ignored");

        JSONObject dest = new JSONObject();

        TDUtils.mergeNestedJSONObject(source, dest, null);

        assertFalse("Scalar top-level keys must not be copied by mergeNestedJSONObject",
                dest.has("scalar"));
    }

    @Test
    public void mergeNested_withTimezone_dateFormattedInNestedObject() throws JSONException {
        TimeZone utcPlus8 = TimeZone.getTimeZone("GMT+8");
        Date date = new Date(0);

        JSONObject innerSource = new JSONObject();
        innerSource.put("ts", date);

        JSONObject source = new JSONObject();
        source.put("meta", innerSource);

        JSONObject dest = new JSONObject();

        TDUtils.mergeNestedJSONObject(source, dest, utcPlus8);

        String result = dest.getJSONObject("meta").getString("ts");
        assertTrue(
                "Nested Date must be formatted correctly",
                Pattern.compile(TDConstants.TIME_CHECK_PATTERN).matcher(result).find()
        );
    }
}
