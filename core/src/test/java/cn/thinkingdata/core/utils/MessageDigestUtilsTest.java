/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class MessageDigestUtilsTest {

    // ── SHA-256 ───────────────────────────────────────────────────────────────

    @Test
    public void sha256_null_returnsEmpty() {
        assertEquals("", MessageDigestUtils.calculateSHA256(null));
    }

    @Test
    public void sha256_emptyString_returnsNonEmpty() {
        String result = MessageDigestUtils.calculateSHA256("");
        assertNotNull(result);
        assertFalse("SHA256 of empty string should not be empty", result.isEmpty());
    }

    @Test
    public void sha256_helloWorld_isConsistent() {
        String result1 = MessageDigestUtils.calculateSHA256("hello");
        String result2 = MessageDigestUtils.calculateSHA256("hello");
        assertEquals("SHA256 must be deterministic", result1, result2);
    }

    @Test
    public void sha256_differentInputs_produceDifferentHashes() {
        String hash1 = MessageDigestUtils.calculateSHA256("hello");
        String hash2 = MessageDigestUtils.calculateSHA256("world");
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void sha256_knownValue() {
        // SHA256("abc") in Base64 = "ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0="
        String result = MessageDigestUtils.calculateSHA256("abc");
        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", result);
    }

    @Test
    public void sha256_longInput_returnsFixedLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) sb.append("x");
        String result = MessageDigestUtils.calculateSHA256(sb.toString());
        assertFalse(result.isEmpty());
        // SHA256 output is 32 bytes → Base64 → 44 chars (with padding)
        assertEquals(44, result.length());
    }

    // ── MD5 ───────────────────────────────────────────────────────────────────

    @Test
    public void md5_null_returnsEmpty() {
        assertEquals("", MessageDigestUtils.calculateMD5(null));
    }

    @Test
    public void md5_emptyString_returnsNonEmpty() {
        String result = MessageDigestUtils.calculateMD5("");
        assertNotNull(result);
        assertFalse("MD5 of empty string should not be empty", result.isEmpty());
    }

    @Test
    public void md5_helloWorld_isConsistent() {
        String result1 = MessageDigestUtils.calculateMD5("hello");
        String result2 = MessageDigestUtils.calculateMD5("hello");
        assertEquals("MD5 must be deterministic", result1, result2);
    }

    @Test
    public void md5_differentInputs_produceDifferentHashes() {
        String hash1 = MessageDigestUtils.calculateMD5("hello");
        String hash2 = MessageDigestUtils.calculateMD5("world");
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void md5_knownValue() {
        // MD5("abc") raw bytes in Base64 = "kAFQmDzST7DWlj99KOF/cg=="
        String result = MessageDigestUtils.calculateMD5("abc");
        assertEquals("kAFQmDzST7DWlj99KOF/cg==", result);
    }

    @Test
    public void md5_output_isAlways24Chars() {
        // MD5 is 16 bytes → Base64 → 24 chars (with padding)
        String result = MessageDigestUtils.calculateMD5("ThinkingData");
        assertEquals(24, result.length());
    }

    // ── SHA256 vs MD5 are distinct ────────────────────────────────────────────

    @Test
    public void sha256_and_md5_produceDistinctOutputs() {
        String input = "ThinkingData";
        assertNotEquals(
                MessageDigestUtils.calculateSHA256(input),
                MessageDigestUtils.calculateMD5(input));
    }
}
