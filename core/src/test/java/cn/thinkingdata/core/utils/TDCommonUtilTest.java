/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for TDCommonUtil.
 *
 * getProp() uses reflection to call android.os.SystemProperties which is not
 * available in local JVM tests. The method catches Throwable, so it always
 * falls back to the provided defaultValue in this environment.
 */
public class TDCommonUtilTest {

    // ── getProp — JVM fallback behavior ──────────────────────────────────────

    @Test
    public void getProp_unknownProperty_returnsDefaultValue() {
        String result = TDCommonUtil.getProp("ro.build.version.sdk", "default");
        // In local JVM, android.os.SystemProperties is not found → Throwable caught → returns default
        assertEquals("default", result);
    }

    @Test
    public void getProp_emptyDefaultValue_returnsEmpty() {
        String result = TDCommonUtil.getProp("any.property", "");
        assertEquals("", result);
    }

    @Test
    public void getProp_nullDefaultValue_returnsNull() {
        String result = TDCommonUtil.getProp("any.property", null);
        assertNull(result);
    }

    @Test
    public void getProp_differentKeys_allReturnDefault() {
        String def = "fallback";
        assertEquals(def, TDCommonUtil.getProp("ro.product.model", def));
        assertEquals(def, TDCommonUtil.getProp("ro.product.manufacturer", def));
        assertEquals(def, TDCommonUtil.getProp("ro.hardware", def));
    }

    @Test
    public void getProp_emptyKey_returnsDefaultValue() {
        String result = TDCommonUtil.getProp("", "fallback");
        assertEquals("fallback", result);
    }
}
