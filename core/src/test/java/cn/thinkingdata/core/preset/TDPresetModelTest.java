/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.preset;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Regression tests for the TDPresetModel crash fix.
 *
 * <p>TDPresetModel depends on an Android {@code Context} and framework classes
 * ({@code TelephonyManager}, {@code WindowManager}, ...), so it cannot be
 * instantiated in a local JVM unit test. These tests instead pin the exact
 * failure semantics behind the fix: preset-property collection (notably
 * {@code getCarrier}) may trigger {@link NoClassDefFoundError} when
 * {@code android.telephony.TelephonyManager} fails class resolution on trimmed
 * ROMs. That error is a {@link java.lang.Error}, NOT an {@link Exception}, so a
 * {@code catch (Exception)} guard lets it escape and crash the init thread.
 * The guard must be widened to {@code catch (Throwable)} with a field-level
 * downgrade to an empty carrier string.
 */
public class TDPresetModelTest {

    /**
     * Reproduces the original bug: a {@code catch (Exception)} guard does NOT
     * contain a {@link NoClassDefFoundError}, so it propagates and would crash
     * the initialization worker thread.
     */
    @Test
    public void catchException_doesNotContainNoClassDefFoundError() {
        try {
            simulateCarrierCollection(new String[]{null}, /* catchThrowable = */ false);
            fail("catch(Exception) must not swallow NoClassDefFoundError");
        } catch (NoClassDefFoundError expected) {
            // Matches the reported Fatal Exception path.
        }
    }

    /**
     * Verifies the fix: widening the guard to {@code catch (Throwable)} contains
     * the {@link NoClassDefFoundError} and downgrades to an empty carrier string,
     * keeping SDK initialization alive.
     */
    @Test
    public void catchThrowable_downgradesCarrierToEmpty() {
        String carrier = simulateCarrierCollection(new String[]{null}, /* catchThrowable = */ true);
        assertEquals("", carrier);
    }

    /**
     * Verifies that on a healthy device the carrier value is unchanged by the
     * widened guard.
     */
    @Test
    public void catchThrowable_normalDevice_returnsCarrierUnchanged() {
        String carrier = simulateCarrierCollection(new String[]{"中国移动"}, /* catchThrowable = */ true);
        assertEquals("中国移动", carrier);
    }

    /**
     * Mirrors the getCarrier() control flow: resolve a value that may throw a
     * NoClassDefFoundError, otherwise fall back to an empty string. When
     * {@code catchThrowable} is false, only Exception is caught (old buggy
     * behavior); when true, Throwable is caught (fixed behavior).
     */
    private String simulateCarrierCollection(String[] holder, boolean catchThrowable) {
        if (catchThrowable) {
            try {
                return resolveCarrier(holder);
            } catch (Throwable e) {
                return "";
            }
        } else {
            try {
                return resolveCarrier(holder);
            } catch (Exception e) {
                return "";
            }
        }
    }

    /**
     * Stand-in for the TelephonyManager access inside getCarrier(): a null value
     * simulates the unresolved framework class and triggers a
     * NoClassDefFoundError, just like the real crash.
     */
    private String resolveCarrier(String[] holder) {
        String value = holder[0];
        if (value == null) {
            throw new NoClassDefFoundError(
                    "Failed resolution of: Landroid/telephony/TelephonyManager;");
        }
        return value;
    }
}
