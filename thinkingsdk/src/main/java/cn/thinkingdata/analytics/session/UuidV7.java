/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.session;

import java.security.SecureRandom;

/**
 * UUID v7 (time-ordered), matching the session-record JS SDK.
 * The first 48 bits are the session start time in milliseconds. The remaining bits are random,
 * so two ids created in the same millisecond stay distinct when the wall clock moves backward.
 */
final class UuidV7 {

    static final String PATTERN =
            "^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    /**
     * @param timestampMillis session start time in epoch milliseconds
     * @return UUID v7 string
     */
    static String generate(long timestampMillis) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        bytes[0] = (byte) ((timestampMillis >>> 40) & 0xff);
        bytes[1] = (byte) ((timestampMillis >>> 32) & 0xff);
        bytes[2] = (byte) ((timestampMillis >>> 24) & 0xff);
        bytes[3] = (byte) ((timestampMillis >>> 16) & 0xff);
        bytes[4] = (byte) ((timestampMillis >>> 8) & 0xff);
        bytes[5] = (byte) (timestampMillis & 0xff);
        bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x70);
        bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80);
        return format(bytes);
    }

    private static String format(byte[] bytes) {
        char[] chars = new char[36];
        int charIndex = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                chars[charIndex++] = '-';
            }
            int value = bytes[i] & 0xff;
            chars[charIndex++] = HEX[value >>> 4];
            chars[charIndex++] = HEX[value & 0x0f];
        }
        return new String(chars);
    }
}
