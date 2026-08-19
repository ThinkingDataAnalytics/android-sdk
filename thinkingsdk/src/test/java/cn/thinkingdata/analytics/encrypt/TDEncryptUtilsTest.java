/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.encrypt;

import org.junit.Test;

import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cn.thinkingdata.core.utils.Base64Coder;

import static org.junit.Assert.*;

public class TDEncryptUtilsTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

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
    public void isEncryptedData_legacyEnvelopeWithoutMode_returnsTrue() {
        // Pre-GCM rows stored in DB only have ekey/pkv/payload.
        String legacy = "{\"ekey\":\"LEGACY\",\"pkv\":1,\"payload\":\"OLD_CIPHER\"}";
        assertTrue(TDEncryptUtils.isEncryptedData(legacy));
    }

    @Test
    public void isEncryptedData_gcmEnvelopeWithMode_returnsTrue() {
        String gcm = "{\"ekey\":\"abc\",\"pkv\":1,\"payload\":\"xyz\",\"mode\":\"gcm\"}";
        assertTrue(TDEncryptUtils.isEncryptedData(gcm));
    }

    @Test
    public void isEncryptedData_keywordsInDifferentOrder_returnsTrue() {
        String encrypted = "{\"payload\":\"xyz\",\"pkv\":1,\"ekey\":\"abc\"}";
        assertTrue(TDEncryptUtils.isEncryptedData(encrypted));
    }

    @Test
    public void isEncryptedData_keywordsAsSubstrings_returnsTrue() {
        String str = "this string contains ekey and pkv and payload as substrings";
        assertTrue(TDEncryptUtils.isEncryptedData(str));
    }

    @Test
    public void isEncryptedData_realWorldLikePayload_returnsTrue() {
        String encrypted = "{\"#type\":\"track\",\"ekey\":\"RSA_ENC_KEY\","
                + "\"pkv\":2,\"payload\":\"AES_ENC_DATA\",\"mode\":\"gcm\"}";
        assertTrue(TDEncryptUtils.isEncryptedData(encrypted));
    }

    // ── isEncryptedData — case sensitivity ────────────────────────────────────

    @Test
    public void isEncryptedData_uppercaseKeywords_returnsFalse() {
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
        byte[] key = TDEncryptUtils.generateAESKey();
        boolean allZero = true;
        for (byte b : key) {
            if (b != 0) { allZero = false; break; }
        }
        assertFalse("Generated key must not be all-zeros", allZero);
    }

    // ── aesEncrypt — null-guard tests ─────────────────────────────────────────

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

    // ── AES-GCM encrypt / decrypt round-trip ──────────────────────────────────

    @Test
    public void aesEncrypt_gcm_roundTripDecryptsOriginal() throws Exception {
        byte[] key = TDEncryptUtils.generateAESKey();
        String plain = "{\"#event_name\":\"purchase\",\"#type\":\"track\"}";
        String cipherB64 = TDEncryptUtils.aesEncrypt(key, plain);
        assertNotNull(cipherB64);

        byte[] combined = Base64Coder.decode(cipherB64);
        assertTrue("payload must contain IV + ciphertext",
                combined.length > TDEncryptUtils.GCM_IV_LENGTH);

        byte[] iv = Arrays.copyOfRange(combined, 0, TDEncryptUtils.GCM_IV_LENGTH);
        byte[] cipherText = Arrays.copyOfRange(combined, TDEncryptUtils.GCM_IV_LENGTH, combined.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TDEncryptUtils.GCM_TAG_LENGTH_BITS, iv));
        String decrypted = new String(cipher.doFinal(cipherText), UTF_8);
        assertEquals(plain, decrypted);
    }

    @Test
    public void aesEncrypt_sameKeyAndContent_producesDifferentCiphertext() throws Exception {
        // Random IV makes GCM non-deterministic.
        byte[] key = TDEncryptUtils.generateAESKey();
        String result1 = TDEncryptUtils.aesEncrypt(key, "payload");
        String result2 = TDEncryptUtils.aesEncrypt(key, "payload");
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals("AES-GCM must use a fresh IV each call", result1, result2);
    }

}
