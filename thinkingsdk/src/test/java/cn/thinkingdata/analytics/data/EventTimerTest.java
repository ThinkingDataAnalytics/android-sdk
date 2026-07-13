/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.data;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class EventTimerTest {

    // ── duration() by TimeUnit ────────────────────────────────────────────────

    @Test
    public void duration_milliseconds_fiveSeconds() {
        // start=0, end=5000 → 5000 ms
        EventTimer timer = new EventTimer(TimeUnit.MILLISECONDS, 0L);
        String result = timer.duration(5000L);
        assertEquals("5000.0", result);
    }

    @Test
    public void duration_seconds_fiveSeconds() {
        // 5000 ms / 1000 = 5.0 s
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        assertEquals("5.0", timer.duration(5000L));
    }

    @Test
    public void duration_minutes_oneMinute() {
        // 60 000 ms / 1000 / 60 = 1.0 min
        EventTimer timer = new EventTimer(TimeUnit.MINUTES, 0L);
        assertEquals("1.0", timer.duration(60_000L));
    }

    @Test
    public void duration_hours_oneHour() {
        // 3 600 000 ms / 1000 / 60 / 60 = 1.0 h
        EventTimer timer = new EventTimer(TimeUnit.HOURS, 0L);
        assertEquals("1.0", timer.duration(3_600_000L));
    }

    @Test
    public void duration_defaultTimeUnit_treatedAsMilliseconds() {
        // Any TimeUnit not SECONDS/MINUTES/HOURS falls through to raw milliseconds
        EventTimer timer = new EventTimer(TimeUnit.DAYS, 0L);
        String result = timer.duration(1000L);
        assertEquals("1000.0", result);
    }

    // ── negative duration ─────────────────────────────────────────────────────

    @Test
    public void duration_endBeforeStart_returnsZero() {
        // start=5000, end=1000 → duration=-4000 → "0"
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 5000L);
        assertEquals("0", timer.duration(1000L));
    }

    @Test
    public void duration_sameStartAndEnd_returnsZero() {
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 1000L);
        assertEquals("0.0", timer.duration(1000L));
    }

    // ── MAX_DURATION cap ──────────────────────────────────────────────────────

    @Test
    public void duration_exceedsMaxDuration_isCappedAtMax() {
        long overMax = EventTimer.MAX_DURATION + 10_000L;
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        String overResult = timer.duration(overMax);

        // Duration at exactly MAX_DURATION in SECONDS
        EventTimer maxTimer = new EventTimer(TimeUnit.SECONDS, 0L);
        String maxResult = maxTimer.duration(EventTimer.MAX_DURATION);

        assertEquals("Exceeding MAX_DURATION should produce the same as MAX_DURATION",
                maxResult, overResult);
    }

    @Test
    public void duration_exactlyMaxDuration_seconds() {
        // MAX_DURATION = 24 * 60 * 60 * 1000 ms = 86400000 ms = 86400 s
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        String result = timer.duration(EventTimer.MAX_DURATION);
        assertEquals("86400.0", result);
    }

    // ── accumulated duration ──────────────────────────────────────────────────

    @Test
    public void duration_withAccumulatedDuration_addsUp() {
        // Simulate: first session 2000 ms, then accumulated 3000 ms more
        EventTimer timer = new EventTimer(TimeUnit.MILLISECONDS, 0L);
        timer.setEventAccumulatedDuration(3000L);
        // Now: elapsed = (5000 - 0) + 3000 = 8000 ms
        assertEquals("8000.0", timer.duration(5000L));
    }

    @Test
    public void setEventAccumulatedDuration_getEventAccumulatedDuration_roundTrip() {
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        timer.setEventAccumulatedDuration(12345L);
        assertEquals(12345L, timer.getEventAccumulatedDuration());
    }

    // ── backgroundDuration ────────────────────────────────────────────────────

    @Test
    public void backgroundDuration_zero_returnsZeroString() {
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        // default backgroundDuration = 0 → 0/1000 = 0.0 s
        assertEquals("0.0", timer.backgroundDuration());
    }

    @Test
    public void backgroundDuration_nonZero_formatsCorrectly() {
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        timer.setBackgroundDuration(10_000L); // 10 000 ms → 10.0 s
        assertEquals("10.0", timer.backgroundDuration());
    }

    @Test
    public void backgroundDuration_negative_returnsZero() {
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        timer.setBackgroundDuration(-500L);
        assertEquals("0", timer.backgroundDuration());
    }

    @Test
    public void setBackgroundDuration_getBackgroundDuration_roundTrip() {
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        timer.setBackgroundDuration(99999L);
        assertEquals(99999L, timer.getBackgroundDuration());
    }

    // ── startTime accessors ───────────────────────────────────────────────────

    @Test
    public void getStartTime_returnsConstructorValue() {
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 42000L);
        assertEquals(42000L, timer.getStartTime());
    }

    @Test
    public void setStartTime_updatesStartTime() {
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        timer.setStartTime(9999L);
        assertEquals(9999L, timer.getStartTime());
    }

    // ── precision formatting ──────────────────────────────────────────────────

    @Test
    public void duration_milliseconds_partialSecond() {
        // 1500 ms in SECONDS → 1.5 s
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        assertEquals("1.5", timer.duration(1500L));
    }

    @Test
    public void duration_milliseconds_threeDecimalPrecision() {
        // 1001 ms in SECONDS → 1.001 s → formatNumberWithSpace rounds to 3 decimals
        EventTimer timer = new EventTimer(TimeUnit.SECONDS, 0L);
        String result = timer.duration(1001L);
        // 1001/1000 = 1.001 → rounded to 3 decimal places = 1.001
        assertEquals("1.001", result);
    }
}
