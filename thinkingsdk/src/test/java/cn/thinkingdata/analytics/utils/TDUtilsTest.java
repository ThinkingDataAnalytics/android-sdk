/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

import org.junit.Test;

import java.util.TimeZone;

import static org.junit.Assert.*;

public class TDUtilsTest {

    // ── formatNumber — round to 1 decimal place ───────────────────────────────

    @Test
    public void formatNumber_zero_returnsZero() {
        assertEquals(0.0, TDUtils.formatNumber(0.0), 1e-9);
    }

    @Test
    public void formatNumber_positiveRoundUp() {
        // 1.05 → Math.round(1.05 * 10) / 10 = Math.round(10.5) / 10 = 11 / 10 = 1.1
        assertEquals(1.1, TDUtils.formatNumber(1.05), 1e-9);
    }

    @Test
    public void formatNumber_positiveRoundDown() {
        // 1.04 → Math.round(10.4) = 10 → 1.0
        assertEquals(1.0, TDUtils.formatNumber(1.04), 1e-9);
    }

    @Test
    public void formatNumber_negativeValue() {
        // -2.35 → Math.round(-23.5) → in Java, rounds towards positive infinity → -23 → -2.3
        // Actually Math.round(-23.5) = -23 in Java (rounds towards positive infinity for .5)
        assertEquals(-2.3, TDUtils.formatNumber(-2.35), 1e-9);
    }

    @Test
    public void formatNumber_wholeNumber_unchanged() {
        assertEquals(5.0, TDUtils.formatNumber(5.0), 1e-9);
        assertEquals(100.0, TDUtils.formatNumber(100.0), 1e-9);
    }

    @Test
    public void formatNumber_alreadyOneDecimal_unchanged() {
        assertEquals(3.7, TDUtils.formatNumber(3.7), 1e-9);
    }

    // ── formatNumberWithSpace — round to N decimal places ────────────────────

    @Test
    public void formatNumberWithSpace_zeroSpace_roundsToInteger() {
        // space=0 → ran=1 → Math.round(num) / 1.0f
        assertEquals(5.0f, TDUtils.formatNumberWithSpace(5.4f, 0), 1e-5f);
        assertEquals(6.0f, TDUtils.formatNumberWithSpace(5.6f, 0), 1e-5f);
    }

    @Test
    public void formatNumberWithSpace_oneDecimal() {
        assertEquals(1.5f, TDUtils.formatNumberWithSpace(1.5f, 1), 1e-5f);
        assertEquals(2.0f, TDUtils.formatNumberWithSpace(1.95f, 1), 1e-4f);
    }

    @Test
    public void formatNumberWithSpace_threeDecimals() {
        // space=3 → ran=1000 → Math.round(num * 1000) / 1000.0f
        assertEquals(1.001f, TDUtils.formatNumberWithSpace(1.001f, 3), 1e-5f);
        assertEquals(5.0f, TDUtils.formatNumberWithSpace(5.000f, 3), 1e-5f);
    }

    @Test
    public void formatNumberWithSpace_negativeNumber() {
        assertEquals(-1.5f, TDUtils.formatNumberWithSpace(-1.5f, 1), 1e-5f);
    }

    @Test
    public void formatNumberWithSpace_zero_returnsZero() {
        assertEquals(0.0f, TDUtils.formatNumberWithSpace(0.0f, 3), 1e-5f);
    }

    // ── getTimezoneOffset — TimeZone → double hours ───────────────────────────

    @Test
    public void getTimezoneOffset_utcPlus8_returns8() {
        TimeZone tz = TimeZone.getTimeZone("GMT+8");
        assertEquals(8.0, TDUtils.getTimezoneOffset(0L, tz), 1e-9);
    }

    @Test
    public void getTimezoneOffset_utc_returnsZero() {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        assertEquals(0.0, TDUtils.getTimezoneOffset(0L, tz), 1e-9);
    }

    @Test
    public void getTimezoneOffset_utcMinus5_returnsNegative5() {
        TimeZone tz = TimeZone.getTimeZone("GMT-5");
        assertEquals(-5.0, TDUtils.getTimezoneOffset(0L, tz), 1e-9);
    }

    @Test
    public void getTimezoneOffset_halfHour_returnsDecimal() {
        // India Standard Time: UTC+5:30 = 5.5
        TimeZone tz = TimeZone.getTimeZone("Asia/Kolkata");
        assertEquals(5.5, TDUtils.getTimezoneOffset(0L, tz), 1e-9);
    }

    @Test
    public void getTimezoneOffset_nullTimeZone_usesSystemDefault() {
        // When null is passed, method falls back to TimeZone.getDefault()
        double result = TDUtils.getTimezoneOffset(0L, null);
        // Result should be in reasonable range [-12, +14]
        assertTrue("Offset should be in [-12, 14]", result >= -12.0 && result <= 14.0);
    }

    @Test
    public void getTimezoneOffset_matchesRawOffset() {
        TimeZone tz = TimeZone.getTimeZone("GMT+8");
        // raw offset = 8 * 3600 * 1000 ms = 28800000
        double expected = tz.getOffset(0L) / (1000.0 * 60 * 60);
        assertEquals(expected, TDUtils.getTimezoneOffset(0L, tz), 1e-9);
    }

    // ── getSuffix — returns trailing N chars (note: hardcoded to 4 internally) ─

    @Test
    public void getSuffix_stringLongerThanLength_returnsLast4Chars() {
        // The implementation always uses substring(length - 4), not the `length` param.
        // "hello world" (11 chars): substring(11 - 4) = substring(7) = "orld"
        String result = TDUtils.getSuffix("hello world", 4);
        assertEquals("orld", result);
    }

    @Test
    public void getSuffix_stringEqualToLength_returnsFull() {
        // source.length() <= length → returns full string
        String result = TDUtils.getSuffix("abcd", 4);
        assertEquals("abcd", result);
    }

    @Test
    public void getSuffix_stringShorterThanLength_returnsFull() {
        String result = TDUtils.getSuffix("hi", 10);
        assertEquals("hi", result);
    }

    @Test
    public void getSuffix_longString_alwaysReturns4TrailingChars() {
        // Regardless of `length` param (when > 4), method returns last 4 chars
        String s = "abcdefghij"; // 10 chars
        // substring(10 - 4) = substring(6) = "ghij"
        assertEquals("ghij", TDUtils.getSuffix(s, 6));
        assertEquals("ghij", TDUtils.getSuffix(s, 5));
        assertEquals("ghij", TDUtils.getSuffix(s, 4));
    }

    @Test
    public void getSuffix_emptyString_returnsEmpty() {
        // TextUtils.isEmpty("") returns false with returnDefaultValues=true,
        // so the isEmpty branch is not taken. Then "".length() <= any_length,
        // so the full (empty) string is returned.
        String result = TDUtils.getSuffix("", 5);
        assertEquals("", result);
    }
}
