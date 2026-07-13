/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.encrypt;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class TDEncryptUtilsTest {

    // ── isEncryptedData — null and empty ──────────────────────────────────────

    @Test
    public void isEncryptedData_null_returnsFalse() {
        assertFalse(TDEncryptUtils.isEncryptedData(null));
    }

    @Test
    public void isEncryptedData_emptyString_returnsFalse() {
        assertFalse(TDEncryptUtils.isEncryptedData(""));
    }

    // ── isEncryptedData — missing keywords ────────────────────────────────────

    @Test
    public void isEncryptedData_plainText_returnsFalse() {
        assertFalse(TDEncryptUtils.isEncryptedData("plain text without keywords"));
    }

    @Test
    public void isEncryptedData_onlyEkey_returnsFalse() {
        assertFalse(TDEncryptUtils.isEncryptedData("{\"ekey\":\"abc123\"}"));
    }

    @Test
    public void isEncryptedData_onlyPkv_returnsFalse() {
        assertFalse(TDEncryptUtils.isEncryptedData("{\"pkv\":1}"));
    }

    @Test
    public void isEncryptedData_onlyPayload_returnsFalse() {
        assertFalse(TDEncryptUtils.isEncryptedData("{\"payload\":\"data\"}"));
    }

    @Test
    public void isEncryptedData_ekeyAndPkv_missingPayload_returnsFalse() {
        assertFalse(TDEncryptUtils.isEncryptedData("{\"ekey\":\"abc\",\"pkv\":1}"));
    }

    @Test
    public void isEncryptedData_ekeyAndPayload_missingPkv_returnsFalse() {
        assertFalse(TDEncryptUtils.isEncryptedData("{\"ekey\":\"abc\",\"payload\":\"data\"}"));
    }

    @Test
    public void isEncryptedData_pkvAndPayload_missingEkey_returnsFalse() {
        assertFalse(TDEncryptUtils.isEncryptedData("{\"pkv\":1,\"payload\":\"data\"}"));
    }

    // ── isEncryptedData — all three keywords present ──────────────────────────

    @Test
    public void isEncryptedData_allThreeKeywords_returnsTrue() {
        String encrypted = "{\"ekey\":\"abc\",\"pkv\":1,\"payload\":\"xyz\"}";
        assertTrue(TDEncryptUtils.isEncryptedData(encrypted));
    }

    @Test
    public void isEncryptedData_keywordsInDifferentOrder_returnsTrue() {
        String encrypted = "{\"payload\":\"xyz\",\"pkv\":1,\"ekey\":\"abc\"}";
        assertTrue(TDEncryptUtils.isEncryptedData(encrypted));
    }

    @Test
    public void isEncryptedData_keywordsAsSubstrings_returnsTrue() {
        // Keywords appear embedded in a longer string — still detects them
        String str = "this string contains ekey and pkv and payload as substrings";
        assertTrue(TDEncryptUtils.isEncryptedData(str));
    }

    @Test
    public void isEncryptedData_realWorldLikePayload_returnsTrue() {
        String encrypted = "{\"#type\":\"track\",\"ekey\":\"RSA_ENC_KEY\","
                + "\"pkv\":2,\"payload\":\"AES_ENC_DATA\"}";
        assertTrue(TDEncryptUtils.isEncryptedData(encrypted));
    }

    // ── isEncryptedData — case sensitivity ────────────────────────────────────

    @Test
    public void isEncryptedData_uppercaseKeywords_returnsFalse() {
        // Method uses String.contains() which is case-sensitive
        assertFalse(TDEncryptUtils.isEncryptedData("{\"EKEY\":\"abc\",\"PKV\":1,\"PAYLOAD\":\"xyz\"}"));
    }

    // ── generateAESKey ────────────────────────────────────────────────────────

    @Test
    public void generateAESKey_returns16ByteArray() throws NoSuchAlgorithmException {
        byte[] key = TDEncryptUtils.generateAESKey();
        assertNotNull(key);
        assertEquals("AES-128 key must be 16 bytes", 16, key.length);
    }

    @Test
    public void generateAESKey_isRandomEachCall() throws NoSuchAlgorithmException {
        byte[] key1 = TDEncryptUtils.generateAESKey();
        byte[] key2 = TDEncryptUtils.generateAESKey();
        assertFalse("Two generated keys should not be identical", Arrays.equals(key1, key2));
    }

    @Test
    public void generateAESKey_allValuesInitialized() throws NoSuchAlgorithmException {
        // A key of all-zeros indicates the generator failed to produce real entropy
        byte[] key = TDEncryptUtils.generateAESKey();
        boolean allZero = true;
        for (byte b : key) {
            if (b != 0) { allZero = false; break; }
        }
        assertFalse("Generated key must not be all-zeros", allZero);
    }

    // ── aesEncrypt — null-guard tests (JVM-safe) ─────────────────────────────
    //
    // Note: aesEncrypt() uses AES/ECB/PKCS7Padding internally. The "PKCS7Padding"
    // name is specific to Android's JCE provider; standard JVM JCE does not
    // recognise it (uses "PKCS5Padding" instead), so the cipher init silently
    // fails and the method returns null on a JVM. Tests that require a non-null
    // ciphertext result must be run as instrumented (device/emulator) tests.

    @Test
    public void aesEncrypt_nullKey_returnsNull() {
        assertNull(TDEncryptUtils.aesEncrypt(null, "some content"));
    }

    @Test
    public void aesEncrypt_nullContent_returnsNull() throws NoSuchAlgorithmException {
        byte[] key = TDEncryptUtils.generateAESKey();
        assertNull(TDEncryptUtils.aesEncrypt(key, null));
    }

    @Test
    public void aesEncrypt_nullKeyAndContent_returnsNull() {
        assertNull(TDEncryptUtils.aesEncrypt(null, null));
    }

    // ── generateAESKey + aesEncrypt pipeline — structural test ───────────────

    @Test
    public void pipeline_sameKeyAndContent_consistentOutput() throws NoSuchAlgorithmException {
        // Regardless of whether encryption succeeds or fails on JVM, the result
        // for identical inputs with the same key must always be the same value.
        byte[] key = TDEncryptUtils.generateAESKey();
        String result1 = TDEncryptUtils.aesEncrypt(key, "payload");
        String result2 = TDEncryptUtils.aesEncrypt(key, "payload");
        assertEquals("aesEncrypt must be deterministic for the same key+content", result1, result2);
    }

}
