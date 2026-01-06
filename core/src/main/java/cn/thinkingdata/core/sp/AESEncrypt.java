/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.sp;

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cn.thinkingdata.core.utils.Base64Coder;

/**
 * @author liulongbing
 * @since 2024/8/13
 */
public class AESEncrypt implements IStorePlugin {

    private static final String ENCRYPT_KEY = "thinking-data-analytics";

    private SecretKeySpec secretKeySpec;

    public AESEncrypt() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(ENCRYPT_KEY.getBytes("UTF-8"));
            key = Arrays.copyOf(key, 16);
            secretKeySpec = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IvParameterSpec generateIvParams() {
        byte[] iv = new byte[16];
        Arrays.fill(iv, ( byte ) 0x00);
        return new IvParameterSpec(iv);
    }

    @Override
    public String encrypt(String src) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, generateIvParams());
            byte[] encrypted = cipher.doFinal(src.getBytes());
            return new String(Base64Coder.encode(encrypted));
        } catch (Exception ignore) {
        }
        return null;
    }

    @Override
    public String decrypt(String dest) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, generateIvParams());
            byte[] decodedBytes = Base64Coder.decode(dest);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted);
        } catch (Exception ignore) {
        }
        return null;
    }

}
