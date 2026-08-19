/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link TDConfig#parseGcmSupported(JSONObject)}, which decides whether the
 * receiver can decrypt AES-GCM payloads based on the remote config field {@code secret_key.mode}.
 */
public class TDConfigGcmModeTest {

    private static JSONObject configData(String secretKeyJson) throws Exception {
        String json = "{\"sync_interval\":15,\"sync_batch_size\":20"
                + (secretKeyJson == null ? "" : ",\"secret_key\":" + secretKeyJson)
                + "}";
        return new JSONObject(json);
    }

    // ── supported ─────────────────────────────────────────────────────────────

    @Test
    public void parseGcmSupported_modeGcm_returnsTrue() throws Exception {
        JSONObject data = configData("{\"mode\":\"gcm\",\"key\":\"PUB\",\"version\":1,"
                + "\"symmetric\":\"AES\",\"asymmetric\":\"RSA\"}");
        assertTrue(TDConfig.parseGcmSupported(data));
    }

    @Test
    public void parseGcmSupported_modeUpperCase_returnsTrue() throws Exception {
        JSONObject data = configData("{\"mode\":\"GCM\",\"key\":\"PUB\",\"version\":1}");
        assertTrue(TDConfig.parseGcmSupported(data));
    }

    @Test
    public void parseGcmSupported_modeOnlyWithoutKey_returnsTrue() throws Exception {
        JSONObject data = configData("{\"mode\":\"gcm\"}");
        assertTrue(TDConfig.parseGcmSupported(data));
    }

    // ── unsupported ───────────────────────────────────────────────────────────

    @Test
    public void parseGcmSupported_oldServerWithoutMode_returnsFalse() throws Exception {
        // Old receiver: secret_key is delivered but carries no mode field.
        JSONObject data = configData("{\"key\":\"PUB\",\"version\":1,"
                + "\"symmetric\":\"AES\",\"asymmetric\":\"RSA\"}");
        assertFalse(TDConfig.parseGcmSupported(data));
    }

    @Test
    public void parseGcmSupported_otherMode_returnsFalse() throws Exception {
        JSONObject data = configData("{\"mode\":\"cbc\",\"key\":\"PUB\",\"version\":1}");
        assertFalse(TDConfig.parseGcmSupported(data));
    }

    @Test
    public void parseGcmSupported_emptyMode_returnsFalse() throws Exception {
        JSONObject data = configData("{\"mode\":\"\",\"key\":\"PUB\",\"version\":1}");
        assertFalse(TDConfig.parseGcmSupported(data));
    }

    @Test
    public void parseGcmSupported_noSecretKey_returnsFalse() throws Exception {
        assertFalse(TDConfig.parseGcmSupported(configData(null)));
    }

    @Test
    public void parseGcmSupported_secretKeyNotAnObject_returnsFalse() throws Exception {
        assertFalse(TDConfig.parseGcmSupported(configData("\"gcm\"")));
    }

    @Test
    public void parseGcmSupported_nullData_returnsFalse() {
        assertFalse(TDConfig.parseGcmSupported(null));
    }

    @Test
    public void parseGcmSupported_emptyData_returnsFalse() {
        assertFalse(TDConfig.parseGcmSupported(new JSONObject()));
    }

    // ── encrypt-without-gcm warning ───────────────────────────────────────────

    @Test
    public void shouldLogEncryptWithoutGcm_encryptOnAndUnsupported_returnsTrue() {
        assertTrue(TDConfig.shouldLogEncryptWithoutGcm(true, false));
    }

    @Test
    public void shouldLogEncryptWithoutGcm_encryptOnAndSupported_returnsFalse() {
        assertFalse(TDConfig.shouldLogEncryptWithoutGcm(true, true));
    }

    @Test
    public void shouldLogEncryptWithoutGcm_encryptOff_returnsFalse() {
        assertFalse(TDConfig.shouldLogEncryptWithoutGcm(false, false));
        assertFalse(TDConfig.shouldLogEncryptWithoutGcm(false, true));
    }
}
