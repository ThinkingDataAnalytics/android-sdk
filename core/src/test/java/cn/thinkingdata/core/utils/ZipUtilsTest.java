/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ZipUtilsTest {

    // ── gzip / unGzip round-trips ─────────────────────────────────────────────

    @Test
    public void roundTrip_shortString() {
        String original = "Hello, ThinkingData!";
        String compressed = ZipUtils.gzip(original);
        assertNotNull(compressed);
        assertFalse(compressed.isEmpty());
        assertEquals(original, ZipUtils.unGzip(compressed));
    }

    @Test
    public void roundTrip_singleChar() {
        String original = "X";
        assertEquals(original, ZipUtils.unGzip(ZipUtils.gzip(original)));
    }

    @Test
    public void roundTrip_jsonLikePayload() {
        String original = "{\"event\":\"test\",\"properties\":{\"key\":\"value\",\"number\":42}}";
        assertEquals(original, ZipUtils.unGzip(ZipUtils.gzip(original)));
    }

    @Test
    public void roundTrip_longString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("ThinkingData Analytics SDK long payload string. ");
        }
        String original = sb.toString();
        assertEquals(original, ZipUtils.unGzip(ZipUtils.gzip(original)));
    }

    @Test
    public void roundTrip_repeatingChars_compressesWell() {
        // Highly compressible content: gzip output should be smaller than input
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("aaaa");
        }
        String original = sb.toString();
        String compressed = ZipUtils.gzip(original);
        // The compressed Base64 string should be shorter than the original
        assertTrue("gzip should compress repeating content",
                compressed.length() < original.length());
        assertEquals(original, ZipUtils.unGzip(compressed));
    }

    // ── empty string ─────────────────────────────────────────────────────────

    @Test
    public void roundTrip_emptyString() {
        String original = "";
        String compressed = ZipUtils.gzip(original);
        assertNotNull(compressed);
        assertEquals(original, ZipUtils.unGzip(compressed));
    }

    // ── special characters ────────────────────────────────────────────────────

    @Test
    public void roundTrip_specialCharacters() {
        String original = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        assertEquals(original, ZipUtils.unGzip(ZipUtils.gzip(original)));
    }

    @Test
    public void roundTrip_newlinesAndTabs() {
        String original = "line1\nline2\ttabbed\rcarriage";
        assertEquals(original, ZipUtils.unGzip(ZipUtils.gzip(original)));
    }

    // ── gzip result is valid Base64 ────────────────────────────────────────────

    @Test
    public void gzip_outputIsValidBase64() {
        String compressed = ZipUtils.gzip("test data");
        // Should be decodable by Base64Coder without throwing
        byte[] decoded = Base64Coder.decode(compressed);
        assertNotNull(decoded);
        assertTrue(decoded.length > 0);
    }

    // ── error cases for unGzip ────────────────────────────────────────────────

    @Test
    public void unGzip_invalidBase64_returnsEmptyString() {
        // Non-Base64 data that is a multiple of 4 but not valid gzip
        // Providing non-gzip data after valid Base64 decoding should catch IOException
        String fakeBase64 = "AAAA";  // decodes to 3 zero bytes — not valid gzip
        assertEquals("", ZipUtils.unGzip(fakeBase64));
    }

    @Test
    public void unGzip_randomBytes_returnsEmptyString() {
        // Base64 of arbitrary bytes that are not gzip-compressed
        String notGzip = Base64Coder.encodeString("not-gzip-data-xyz");
        assertEquals("", ZipUtils.unGzip(notGzip));
    }
}
