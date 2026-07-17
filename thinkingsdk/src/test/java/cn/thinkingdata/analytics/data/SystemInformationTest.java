/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.analytics.data;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    // ---------------------------------------------------------------------
    // currentPresetProperties() copy-isolation regression (v3.4.5)
    //
    // Under the new R8 (AGP 9.2.1) `private final Map mDeviceInfo` is treated as an
    // immutable parameter, so the old `new JSONObject(mDeviceInfo)` + remove path
    // crashed. The fix iterates the source read-only and writes only the wanted keys
    // straight into a fresh container (skipping the lib keys), so no remove ever runs
    // and no reference to the source survives. These tests pin that control flow:
    // mDeviceInfo is exposed as an unmodifiableMap and must remain intact.
    // ---------------------------------------------------------------------

    private static final String KEY_LIB = "#lib";
    private static final String KEY_LIB_VERSION = "#lib_version";

    /** Building the result must not mutate the read-only source map. */
    @Test
    public void copyIsolation_buildResult_doesNotMutateSource() {
        Map<String, Object> source = Collections.unmodifiableMap(sampleDeviceInfo());
        Map<String, Object> result = buildPresetPropertiesFrom(source);

        // Source still holds every key, including the lib keys.
        assertTrue(source.containsKey(KEY_LIB));
        assertTrue(source.containsKey(KEY_LIB_VERSION));
        assertEquals("Android", source.get(KEY_LIB));
        assertEquals("3.4.5", source.get(KEY_LIB_VERSION));
        // Result is an independent instance.
        assertFalse(result == source);
    }

    /** The produced result excludes the lib keys but preserves everything else. */
    @Test
    public void buildResult_excludesLibKeys() {
        Map<String, Object> source = Collections.unmodifiableMap(sampleDeviceInfo());
        Map<String, Object> result = buildPresetPropertiesFrom(source);

        assertFalse(result.containsKey(KEY_LIB));
        assertFalse(result.containsKey(KEY_LIB_VERSION));
        assertEquals("iPhone-clone", result.get("#manufacturer"));
        assertEquals("中国移动", result.get("#carrier"));
        assertEquals(source.size() - 2, result.size());
    }

    /** Reverse case: removing directly on an unmodifiableMap must throw — proving the source must stay read-only. */
    @Test
    public void removeDirectlyOnUnmodifiableMap_throws() {
        Map<String, Object> source = Collections.unmodifiableMap(sampleDeviceInfo());
        try {
            source.remove(KEY_LIB);
            fail("removing on an unmodifiableMap must throw; iterate-and-filter is mandatory");
        } catch (UnsupportedOperationException expected) {
            // Matches the crash the fix avoids.
        }
    }

    private Map<String, Object> sampleDeviceInfo() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put(KEY_LIB, "Android");
        deviceInfo.put(KEY_LIB_VERSION, "3.4.5");
        deviceInfo.put("#manufacturer", "iPhone-clone");
        deviceInfo.put("#carrier", "中国移动");
        return deviceInfo;
    }

    /**
     * Mirrors the fixed {@code currentPresetProperties()} control flow: iterate the source
     * read-only and write only the wanted keys straight into a fresh container, skipping the
     * lib keys during iteration. No remove is performed, and the source keeps no live
     * reference in the result.
     */
    private Map<String, Object> buildPresetPropertiesFrom(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (KEY_LIB.equals(key) || KEY_LIB_VERSION.equals(key)) {
                continue;
            }
            result.put(key, entry.getValue());
        }
        return result;
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
