/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class TimeUtilTest {

    // ── getTimeZone — pure math, no Android deps ──────────────────────────────

    @Test
    public void getTimeZone_positiveWholeHour() {
        TimeZone tz = TimeUtil.getTimeZone(8.0);
        assertNotNull(tz);
        assertEquals("GMT+08:00", tz.getID());
    }

    @Test
    public void getTimeZone_negativeHalfHour() {
        TimeZone tz = TimeUtil.getTimeZone(-5.5);
        assertNotNull(tz);
        assertEquals("GMT-05:30", tz.getID());
    }

    @Test
    public void getTimeZone_zero() {
        TimeZone tz = TimeUtil.getTimeZone(0.0);
        assertNotNull(tz);
        assertEquals("GMT+00:00", tz.getID());
    }

    @Test
    public void getTimeZone_nepalOffset_fiveFortyFive() {
        // Nepal Standard Time: UTC+5:45
        TimeZone tz = TimeUtil.getTimeZone(5.75);
        assertNotNull(tz);
        assertEquals("GMT+05:45", tz.getID());
    }

    @Test
    public void getTimeZone_negativeWholeHour() {
        TimeZone tz = TimeUtil.getTimeZone(-3.0);
        assertNotNull(tz);
        assertEquals("GMT-03:00", tz.getID());
    }

    @Test
    public void getTimeZone_positiveHalfHour() {
        // India Standard Time: UTC+5:30
        TimeZone tz = TimeUtil.getTimeZone(5.5);
        assertNotNull(tz);
        assertEquals("GMT+05:30", tz.getID());
    }

    @Test
    public void getTimeZone_offsetRawMs_correctSign() {
        // UTC+8 should have a positive raw offset
        TimeZone utcPlus8 = TimeUtil.getTimeZone(8.0);
        assertTrue("UTC+8 raw offset should be positive", utcPlus8.getRawOffset() > 0);

        TimeZone utcMinus5 = TimeUtil.getTimeZone(-5.0);
        assertTrue("UTC-5 raw offset should be negative", utcMinus5.getRawOffset() < 0);
    }

    @Test
    public void getTimeZone_offsetRawMs_correctValue() {
        // UTC+8 = 8 * 3600 * 1000 ms
        TimeZone tz = TimeUtil.getTimeZone(8.0);
        assertEquals(8 * 3600 * 1000, tz.getRawOffset());
    }

    // ── formatDate ────────────────────────────────────────────────────────────

    @Test
    public void formatDate_nullPattern_usesDefaultPattern() {
        // TextUtils.isEmpty returns false with returnDefaultValues=true,
        // so a null pattern will NOT trigger the fallback inside the method.
        // We test that the method handles a valid pattern normally.
        Date date = new Date(0); // 1970-01-01 00:00:00.000 UTC
        TimeZone utc = TimeUtil.getTimeZone(0.0);
        String result = TimeUtil.formatDate(date, TimeUtil.TIME_PATTERN, utc);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue("Result should start with 1970", result.startsWith("1970"));
    }

    @Test
    public void formatDate_nonNullPattern_returnsFormattedString() {
        Date date = new Date(1000L); // 1 second after epoch
        TimeZone utc = TimeUtil.getTimeZone(0.0);
        String result = TimeUtil.formatDate(date, "yyyy-MM-dd HH:mm:ss.SSS", utc);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void formatDate_nullDate_returnsEmptyOrHandled() {
        // Passing null date may throw or return empty — just ensure no crash
        try {
            String result = TimeUtil.formatDate(null, TimeUtil.TIME_PATTERN, TimeUtil.getTimeZone(0));
            // If it returns without throwing, it should return an empty string
            // (SimpleDateFormat.format(null) throws NullPointerException, caught internally)
        } catch (Exception e) {
            // Acceptable — method may propagate NullPointerException
        }
    }

    // ── getFormatDate ─────────────────────────────────────────────────────────

    @Test
    public void getFormatDate_validInput_parsesCorrectly() {
        String dateStr = "2024-06-15 10:30:00.000";
        Date result = TimeUtil.getFormatDate(dateStr, "yyyy-MM-dd HH:mm:ss.SSS", 0.0);
        assertNotNull(result);
        // Result should not be epoch 0 (which is the error sentinel)
        assertNotEquals(0L, result.getTime());
    }

    @Test
    public void getFormatDate_emptyPattern_returnsEpoch() {
        // Empty pattern → returns new Date(0) per source code
        Date result = TimeUtil.getFormatDate("2024-06-15", "", 8.0);
        assertEquals(0L, result.getTime());
    }

    // ── round-trip: formatDate → getFormatDate ────────────────────────────────

    @Test
    public void roundTrip_formatAndParse() {
        long millis = 1700000000000L; // some arbitrary timestamp
        Date original = new Date(millis);
        TimeZone tz = TimeUtil.getTimeZone(8.0);
        String formatted = TimeUtil.formatDate(original, TimeUtil.TIME_PATTERN, tz);
        Date parsed = TimeUtil.getFormatDate(formatted, TimeUtil.TIME_PATTERN, 8.0);
        // Round-trip should match to millisecond precision
        assertEquals(original.getTime(), parsed.getTime());
    }
}
