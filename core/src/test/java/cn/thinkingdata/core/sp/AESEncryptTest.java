/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.sp;

import org.junit.Before;
import org.junit.Test;

import javax.crypto.spec.IvParameterSpec;

import static org.junit.Assert.*;

public class AESEncryptTest {

    private AESEncrypt aesEncrypt;

    @Before
    public void setUp() {
        aesEncrypt = new AESEncrypt();
    }

    // ── encrypt / decrypt round-trip ─────────────────────────────────────────

    @Test
    public void roundTrip_simplePlainText() {
        String original = "Hello, ThinkingData!";
        String encrypted = aesEncrypt.encrypt(original);
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());
        assertNotEquals(original, encrypted);
        assertEquals(original, aesEncrypt.decrypt(encrypted));
    }

    @Test
    public void roundTrip_shortText() {
        String original = "hi";
        assertEquals(original, aesEncrypt.decrypt(aesEncrypt.encrypt(original)));
    }

    @Test
    public void roundTrip_emptyString() {
        String original = "";
        String encrypted = aesEncrypt.encrypt(original);
        assertNotNull(encrypted);
        assertEquals(original, aesEncrypt.decrypt(encrypted));
    }

    @Test
    public void roundTrip_specialCharacters() {
        String original = "key=value&foo=bar!@#$%^&*()";
        assertEquals(original, aesEncrypt.decrypt(aesEncrypt.encrypt(original)));
    }

    @Test
    public void roundTrip_jsonPayload() {
        String original = "{\"event\":\"purchase\",\"amount\":99.99,\"user\":\"test_user\"}";
        assertEquals(original, aesEncrypt.decrypt(aesEncrypt.encrypt(original)));
    }

    @Test
    public void roundTrip_longText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) sb.append("ThinkingData Analytics SDK test data. ");
        String original = sb.toString();
        assertEquals(original, aesEncrypt.decrypt(aesEncrypt.encrypt(original)));
    }

    @Test
    public void roundTrip_unicodeText() {
        String original = "分析数据 ThinkingData 유니코드";
        assertEquals(original, aesEncrypt.decrypt(aesEncrypt.encrypt(original)));
    }

    // ── encrypt output is deterministic ──────────────────────────────────────

    @Test
    public void encrypt_sameInput_sameOutput() {
        // AES-CBC with a fixed IV is deterministic
        String input = "same input";
        assertEquals(aesEncrypt.encrypt(input), aesEncrypt.encrypt(input));
    }

    @Test
    public void encrypt_differentInputs_differentOutputs() {
        assertNotEquals(aesEncrypt.encrypt("hello"), aesEncrypt.encrypt("world"));
    }

    // ── encrypt output is valid Base64 ────────────────────────────────────────

    @Test
    public void encrypt_outputIsValidBase64() {
        String encrypted = aesEncrypt.encrypt("test data");
        assertNotNull(encrypted);
        // Base64-encoded AES output — should be decodable
        byte[] decoded = cn.thinkingdata.core.utils.Base64Coder.decode(encrypted);
        assertNotNull(decoded);
        assertTrue(decoded.length > 0);
    }

    // ── two AESEncrypt instances share the same key ───────────────────────────

    @Test
    public void encrypt_decrypt_acrossInstances() {
        AESEncrypt encryptor = new AESEncrypt();
        AESEncrypt decryptor = new AESEncrypt();
        String original = "cross-instance round-trip";
        assertEquals(original, decryptor.decrypt(encryptor.encrypt(original)));
    }

    // ── generateIvParams ─────────────────────────────────────────────────────

    @Test
    public void generateIvParams_returns16ByteIV() {
        IvParameterSpec iv = aesEncrypt.generateIvParams();
        assertNotNull(iv);
        assertEquals(16, iv.getIV().length);
    }

    @Test
    public void generateIvParams_allBytesAreZero() {
        byte[] ivBytes = aesEncrypt.generateIvParams().getIV();
        for (byte b : ivBytes) {
            assertEquals(0, b);
        }
    }

    @Test
    public void generateIvParams_calledTwice_returnsIdenticalIV() {
        byte[] iv1 = aesEncrypt.generateIvParams().getIV();
        byte[] iv2 = aesEncrypt.generateIvParams().getIV();
        assertArrayEquals(iv1, iv2);
    }
}
