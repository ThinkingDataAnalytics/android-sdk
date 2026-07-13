/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class Base64CoderTest {

    // ── encodeString / decodeString round-trips ──────────────────────────────

    @Test
    public void roundTrip_ascii() {
        String original = "Hello, ThinkingData!";
        String encoded = Base64Coder.encodeString(original);
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
        assertEquals(original, Base64Coder.decodeString(encoded));
    }

    @Test
    public void roundTrip_singleChar() {
        String original = "A";
        assertEquals(original, Base64Coder.decodeString(Base64Coder.encodeString(original)));
    }

    @Test
    public void roundTrip_specialChars() {
        String original = "key=value&foo=bar";
        assertEquals(original, Base64Coder.decodeString(Base64Coder.encodeString(original)));
    }

    @Test
    public void roundTrip_longString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("abcdefghij");
        }
        String original = sb.toString();
        assertEquals(original, Base64Coder.decodeString(Base64Coder.encodeString(original)));
    }

    // ── encode / decode on raw bytes ─────────────────────────────────────────

    @Test
    public void byteRoundTrip_allZeros() {
        byte[] original = new byte[]{0, 0, 0, 0};
        char[] encoded = Base64Coder.encode(original);
        byte[] decoded = Base64Coder.decode(encoded);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void byteRoundTrip_allOnes() {
        byte[] original = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        assertArrayEquals(original, Base64Coder.decode(Base64Coder.encode(original)));
    }

    @Test
    public void byteRoundTrip_mixedBytes() {
        byte[] original = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertArrayEquals(original, Base64Coder.decode(Base64Coder.encode(original)));
    }

    // ── empty string ─────────────────────────────────────────────────────────

    @Test
    public void encode_emptyString_producesEmptyOutput() {
        char[] encoded = Base64Coder.encode(new byte[0]);
        assertEquals(0, encoded.length);
    }

    @Test
    public void decodeString_emptyString_producesEmptyOutput() {
        assertEquals("", Base64Coder.decodeString(""));
    }

    // ── output length is always a multiple of 4 ──────────────────────────────

    @Test
    public void encodedLength_isMultipleOf4() {
        for (int len = 1; len <= 10; len++) {
            byte[] data = new byte[len];
            char[] encoded = Base64Coder.encode(data);
            assertEquals("encoded length must be multiple of 4 for input length " + len,
                    0, encoded.length % 4);
        }
    }

    // ── padding characters ────────────────────────────────────────────────────

    @Test
    public void encode_oneByteInput_twoTrailingPadding() {
        // 1 byte → 4 Base64 chars, last 2 are '='
        char[] encoded = Base64Coder.encode(new byte[]{65});
        assertEquals(4, encoded.length);
        assertEquals('=', encoded[2]);
        assertEquals('=', encoded[3]);
    }

    @Test
    public void encode_twoBytesInput_oneTrailingPadding() {
        // 2 bytes → 4 Base64 chars, last 1 is '='
        char[] encoded = Base64Coder.encode(new byte[]{65, 66});
        assertEquals(4, encoded.length);
        assertEquals('=', encoded[3]);
    }

    // ── error cases ───────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void decode_lengthNotMultipleOf4_throwsIllegalArgument() {
        Base64Coder.decode("ABC");  // 3 chars – not a multiple of 4
    }

    @Test(expected = IllegalArgumentException.class)
    public void decode_illegalCharacter_throwsIllegalArgument() {
        // '@' is not a valid Base64 character
        Base64Coder.decode("QUJ@");
    }

    @Test(expected = IllegalArgumentException.class)
    public void decode_highByteCharacter_throwsIllegalArgument() {
        // Character > 127 is rejected
        Base64Coder.decode(new char[]{'Q', 'U', '\u0080', '='});
    }

    // ── known encode output ──────────────────────────────────────────────────

    @Test
    public void encodeString_knownValue() {
        // "Man" encodes to "TWFu" in standard Base64
        assertEquals("TWFu", Base64Coder.encodeString("Man"));
    }

    @Test
    public void decodeString_knownValue() {
        assertEquals("Man", Base64Coder.decodeString("TWFu"));
    }
}
