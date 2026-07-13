/*
 * Copyright (C) 2025 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class CustomTimestampFormatterTest {

    // ── Unix epoch ────────────────────────────────────────────────────────────

    @Test
    public void epoch_utcZero_producesEpochString() {
        // Timestamp 0 in UTC → 1970-01-01 00:00:00.000
        String result = CustomTimestampFormatter.formatTimestamp(0L, 0.0);
        assertEquals("1970-01-01 00:00:00.000", result);
    }

    // ── timezone offsets ──────────────────────────────────────────────────────

    @Test
    public void epochWithPositiveOffset_addsHours() {
        // UTC+8: timestamp 0 should become 1970-01-01 08:00:00.000
        String result = CustomTimestampFormatter.formatTimestamp(0L, 8.0);
        assertEquals("1970-01-01 08:00:00.000", result);
    }

    @Test
    public void epochWithNegativeOffset_subtractsHours() {
        // 2024-01-15 12:30:45.678 UTC with UTC-5 view → 2024-01-15 07:30:45.678
        // offset = (long)(-5.0) * 60*60*1000 = -18000000 ms
        long ts = 1705321845678L;
        String result = CustomTimestampFormatter.formatTimestamp(ts, -5.0);
        assertEquals("2024-01-15 07:30:45.678", result);
    }

    @Test
    public void positiveHalfHourOffset() {
        // UTC+5:30 (India): offset truncated to int (5) in current impl → UTC+5
        // Source: (long) timeZoneOffsetHours * 60 * 60 * 1000 — cast truncates fraction
        // So 5.5 → (long)5.5 = 5 → 5 hours offset
        String result = CustomTimestampFormatter.formatTimestamp(0L, 5.5);
        assertEquals("1970-01-01 05:00:00.000", result);
    }

    // ── known timestamp ───────────────────────────────────────────────────────

    @Test
    public void knownTimestamp_utc() {
        // 2024-01-15 12:30:45.678 UTC = 1705321845678 ms
        long ts = 1705321845678L;
        String result = CustomTimestampFormatter.formatTimestamp(ts, 0.0);
        assertEquals("2024-01-15 12:30:45.678", result);
    }

    @Test
    public void knownTimestamp_withPositiveOffset() {
        // 2024-01-15 12:30:45.678 UTC viewed in UTC+8 → 2024-01-15 20:30:45.678
        long ts = 1705321845678L;
        String result = CustomTimestampFormatter.formatTimestamp(ts, 8.0);
        assertEquals("2024-01-15 20:30:45.678", result);
    }

    // ── leap year ─────────────────────────────────────────────────────────────

    @Test
    public void leapYear2024_feb29_isValid() {
        // 2024-02-29 00:00:00.000 UTC in epoch millis = 1709164800000
        long ts = 1709164800000L;
        String result = CustomTimestampFormatter.formatTimestamp(ts, 0.0);
        assertEquals("2024-02-29 00:00:00.000", result);
    }

    @Test
    public void leapYear2000_dividesBy400_isLeap() {
        // 2000-02-29 00:00:00.000 UTC in epoch millis = 951782400000
        long ts = 951782400000L;
        String result = CustomTimestampFormatter.formatTimestamp(ts, 0.0);
        assertEquals("2000-02-29 00:00:00.000", result);
    }

    // ── millisecond padding ───────────────────────────────────────────────────

    @Test
    public void millisPadding_singleDigit_twoLeadingZeros() {
        // Find a timestamp where millis = 5
        // e.g., 1000 * N + 5
        long ts = 1705317045005L; // millis = 5
        String result = CustomTimestampFormatter.formatTimestamp(ts, 0.0);
        assertTrue("millis=5 should have '005' suffix", result.endsWith(".005"));
    }

    @Test
    public void millisPadding_twoDigits_oneLeadingZero() {
        // millis = 50
        long ts = 1705317045050L;
        String result = CustomTimestampFormatter.formatTimestamp(ts, 0.0);
        assertTrue("millis=50 should have '050' suffix", result.endsWith(".050"));
    }

    @Test
    public void millisPadding_threeDigits_noLeadingZero() {
        // millis = 678
        long ts = 1705317045678L;
        String result = CustomTimestampFormatter.formatTimestamp(ts, 0.0);
        assertTrue("millis=678 should have '678' suffix", result.endsWith(".678"));
    }

    // ── output format ─────────────────────────────────────────────────────────

    @Test
    public void outputFormat_matchesExpectedPattern() {
        String result = CustomTimestampFormatter.formatTimestamp(1705321845678L, 0.0);
        // Pattern: yyyy-MM-dd HH:mm:ss.SSS (23 chars)
        assertEquals(23, result.length());
        assertEquals('-', result.charAt(4));
        assertEquals('-', result.charAt(7));
        assertEquals(' ', result.charAt(10));
        assertEquals(':', result.charAt(13));
        assertEquals(':', result.charAt(16));
        assertEquals('.', result.charAt(19));
    }

    @Test
    public void dayPadding_singleDigitDay_hasLeadingZero() {
        // 2024-01-05 → day 5
        long ts = 1704412800000L; // 2024-01-05 00:00:00.000 UTC
        String result = CustomTimestampFormatter.formatTimestamp(ts, 0.0);
        assertEquals("2024-01-05 00:00:00.000", result);
    }

    @Test
    public void monthPadding_singleDigitMonth_hasLeadingZero() {
        // 2024-01-01
        long ts = 1704067200000L; // 2024-01-01 00:00:00.000 UTC
        String result = CustomTimestampFormatter.formatTimestamp(ts, 0.0);
        assertEquals("2024-01-01 00:00:00.000", result);
    }

    // ── end-of-year boundary ──────────────────────────────────────────────────

    @Test
    public void endOfYear2023_dec31() {
        // 2023-12-31 23:59:59.999 UTC = 1704067199999
        long ts = 1704067199999L;
        String result = CustomTimestampFormatter.formatTimestamp(ts, 0.0);
        assertEquals("2023-12-31 23:59:59.999", result);
    }
}
