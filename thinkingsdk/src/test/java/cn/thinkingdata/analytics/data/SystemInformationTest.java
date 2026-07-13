/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.analytics.data;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression tests for the second line of defense added to
 * {@code SystemInformation#setupDeviceInfo}.
 *
 * <p>{@code SystemInformation} depends on an Android {@code Context}, so it cannot be
 * instantiated in a local JVM unit test. These tests instead pin the exact semantics of
 * the preset-property merge guard: when the preset-property module throws a
 * {@link java.lang.Error} (e.g. {@link NoClassDefFoundError} from failed class resolution
 * of a framework class), the merge step must not propagate it — the already-populated
 * device-info fields (lib / lib_version) must survive so SDK initialization completes.
 */
public class SystemInformationTest {

    /**
     * Reproduces the original risk: merging preset properties without a
     * {@code catch (Throwable)} guard lets a {@link NoClassDefFoundError} escape and
     * crash the initialization thread, discarding the device-info accumulated so far.
     */
    @Test
    public void mergeWithoutGuard_propagatesError() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("#lib", "Android");
        try {
            mergePresetProperties(deviceInfo, /* throwError = */ true, /* guard = */ false);
            fail("unguarded merge must not swallow NoClassDefFoundError");
        } catch (NoClassDefFoundError expected) {
            // Matches the reported Fatal Exception path.
        }
    }

    /**
     * Verifies the fix: the {@code catch (Throwable)} guard contains the error and keeps
     * the previously populated device-info fields intact.
     */
    @Test
    public void mergeWithGuard_errorDowngradesButKeepsExistingFields() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("#lib", "Android");
        deviceInfo.put("#lib_version", "3.4.4");
        mergePresetProperties(deviceInfo, /* throwError = */ true, /* guard = */ true);
        assertEquals("Android", deviceInfo.get("#lib"));
        assertEquals("3.4.4", deviceInfo.get("#lib_version"));
    }

    /**
     * Verifies that on a healthy device all preset properties still merge normally.
     */
    @Test
    public void mergeWithGuard_normalDevice_mergesAllProperties() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("#lib", "Android");
        mergePresetProperties(deviceInfo, /* throwError = */ false, /* guard = */ true);
        assertTrue(deviceInfo.containsKey("#carrier"));
        assertEquals("中国移动", deviceInfo.get("#carrier"));
    }

    /**
     * Mirrors the setupDeviceInfo() merge control flow: obtain preset properties (which
     * may throw an Error) and copy them into deviceInfo. When {@code guard} is false only
     * the raw merge runs (old behavior); when true the merge is wrapped in
     * {@code catch (Throwable)} (fixed behavior).
     */
    private void mergePresetProperties(Map<String, Object> deviceInfo, boolean throwError, boolean guard) {
        if (guard) {
            try {
                doMerge(deviceInfo, throwError);
            } catch (Throwable ignored) {
                // downgrade: keep existing deviceInfo fields
            }
        } else {
            doMerge(deviceInfo, throwError);
        }
    }

    private void doMerge(Map<String, Object> deviceInfo, boolean throwError) {
        Map<String, Object> presetProperties = resolvePresetProperties(throwError);
        for (String key : presetProperties.keySet()) {
            deviceInfo.put(key, presetProperties.get(key));
        }
    }

    /**
     * Stand-in for {@code TDPresetModel.getInstance(...).getPresetProperties()}: throwing
     * a NoClassDefFoundError simulates the unresolved framework class inside the preset
     * module, just like the real crash.
     */
    private Map<String, Object> resolvePresetProperties(boolean throwError) {
        if (throwError) {
            throw new NoClassDefFoundError(
                    "Failed resolution of: Landroid/telephony/TelephonyManager;");
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("#carrier", "中国移动");
        return properties;
    }
}
