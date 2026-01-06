/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author liulongbing
 * @since 2024/5/22
 */
public class MessageDigestUtils {
    public static String calculateSHA256(String data) {
        if (data == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(data.getBytes());
            char[] resultBytes = Base64Coder.encode(hashedBytes);
            return new String(resultBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String calculateMD5(String data) {
        if (data == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes());
            char[] resultBytes = Base64Coder.encode(digest);
            return new String(resultBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
