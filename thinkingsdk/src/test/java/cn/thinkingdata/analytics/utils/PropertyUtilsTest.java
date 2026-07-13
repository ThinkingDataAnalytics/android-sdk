/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class PropertyUtilsTest {

    // ── isInvalidName — null and empty ────────────────────────────────────────

    @Test
    public void isInvalidName_null_returnsTrue() {
        assertTrue(PropertyUtils.isInvalidName(null));
    }

    @Test
    public void isInvalidName_emptyString_returnsTrue() {
        assertTrue(PropertyUtils.isInvalidName(""));
    }

    // ── isInvalidName — valid names ───────────────────────────────────────────

    @Test
    public void isInvalidName_simpleAsciiName_returnsFalse() {
        assertFalse(PropertyUtils.isInvalidName("eventName"));
    }

    @Test
    public void isInvalidName_uppercaseName_returnsFalse() {
        assertFalse(PropertyUtils.isInvalidName("EventName"));
    }

    @Test
    public void isInvalidName_nameWithUnderscore_returnsFalse() {
        assertFalse(PropertyUtils.isInvalidName("event_name_1"));
    }

    @Test
    public void isInvalidName_singleLetter_returnsFalse() {
        assertFalse(PropertyUtils.isInvalidName("a"));
    }

    @Test
    public void isInvalidName_exactly50Chars_returnsFalse() {
        // "a" + 49 more letters = exactly 50 chars
        String name = "a" + repeat("b", 49);
        assertEquals(50, name.length());
        assertFalse(PropertyUtils.isInvalidName(name));
    }

    // ── isInvalidName — invalid names ─────────────────────────────────────────

    @Test
    public void isInvalidName_startsWithDigit_returnsTrue() {
        assertTrue(PropertyUtils.isInvalidName("1invalid"));
    }

    @Test
    public void isInvalidName_startsWithUnderscore_returnsTrue() {
        assertTrue(PropertyUtils.isInvalidName("_invalid"));
    }

    @Test
    public void isInvalidName_containsSpace_returnsTrue() {
        assertTrue(PropertyUtils.isInvalidName("has space"));
    }

    @Test
    public void isInvalidName_containsHyphen_returnsTrue() {
        assertTrue(PropertyUtils.isInvalidName("event-name"));
    }

    @Test
    public void isInvalidName_containsDot_returnsTrue() {
        assertTrue(PropertyUtils.isInvalidName("event.name"));
    }

    @Test
    public void isInvalidName_51Chars_returnsTrue() {
        // Pattern allows max 50 chars (1 start + 0..49 more)
        String name = "a" + repeat("b", 50); // 51 chars
        assertEquals(51, name.length());
        assertTrue(PropertyUtils.isInvalidName(name));
    }

    @Test
    public void isInvalidName_containsChinese_returnsTrue() {
        assertTrue(PropertyUtils.isInvalidName("事件名称"));
    }

    // ── cutToBytes — ASCII strings ────────────────────────────────────────────

    @Test
    public void cutToBytes_shortAscii_withinLimit_returnsAll() throws UnsupportedEncodingException {
        byte[] result = PropertyUtils.cutToBytes("hello", 100);
        assertArrayEquals("hello".getBytes("UTF-8"), result);
    }

    @Test
    public void cutToBytes_emptyString_returnsEmptyArray() throws UnsupportedEncodingException {
        byte[] result = PropertyUtils.cutToBytes("", 10);
        assertEquals(0, result.length);
    }

    @Test
    public void cutToBytes_exactLimit_returnsAll() throws UnsupportedEncodingException {
        String s = "hello"; // 5 bytes in UTF-8
        byte[] result = PropertyUtils.cutToBytes(s, 5);
        assertArrayEquals(s.getBytes("UTF-8"), result);
    }

    @Test
    public void cutToBytes_limitShorterThanAscii_truncatesAtLimit() throws UnsupportedEncodingException {
        // "hello" → truncated to 3 bytes → "hel"
        byte[] result = PropertyUtils.cutToBytes("hello", 3);
        assertEquals(3, result.length);
        assertArrayEquals("hel".getBytes("UTF-8"), result);
    }

    // ── cutToBytes — multi-byte UTF-8 (CJK characters) ───────────────────────

    @Test
    public void cutToBytes_cjk_noSplitInMiddleOfSequence() throws UnsupportedEncodingException {
        // Each CJK character is 3 bytes in UTF-8.
        // "数据" = 6 bytes. Cutting at 5 should give 3 bytes (first char only),
        // not 5 bytes (which would split the second character).
        String cjk = "数据";
        byte[] fullBytes = cjk.getBytes("UTF-8");
        assertEquals(6, fullBytes.length);

        byte[] cut = PropertyUtils.cutToBytes(cjk, 5);
        // Must not end mid-sequence: valid lengths are 0 or 3
        assertTrue("Cut must not split a multi-byte sequence",
                cut.length == 0 || cut.length == 3);
    }

    @Test
    public void cutToBytes_cjk_singleChar_limitAtExactCharBoundary() throws UnsupportedEncodingException {
        // "数" = 3 bytes; limit = 3 → full character
        String s = "数";
        byte[] result = PropertyUtils.cutToBytes(s, 3);
        assertArrayEquals(s.getBytes("UTF-8"), result);
    }

    @Test
    public void cutToBytes_cjk_limitTooSmall_returnsEmpty() throws UnsupportedEncodingException {
        // "数" = 3 bytes; limit = 2 → not enough for even one character
        byte[] result = PropertyUtils.cutToBytes("数", 2);
        assertEquals(0, result.length);
    }

    @Test
    public void cutToBytes_mixedAsciiAndCjk_splitsAtCharBoundary() throws UnsupportedEncodingException {
        // "ab数" = 2 + 3 = 5 bytes; limit = 4 → "ab" (2 bytes, can't fit 数)
        byte[] result = PropertyUtils.cutToBytes("ab数", 4);
        assertEquals(2, result.length);
        assertArrayEquals("ab".getBytes("UTF-8"), result);
    }

    @Test
    public void cutToBytes_mixedAsciiAndCjk_fitsExactly() throws UnsupportedEncodingException {
        // "ab数" = 5 bytes; limit = 5 → full string
        byte[] result = PropertyUtils.cutToBytes("ab数", 5);
        assertArrayEquals("ab数".getBytes("UTF-8"), result);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }
}
