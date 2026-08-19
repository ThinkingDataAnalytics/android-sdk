/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.encrypt;

import android.text.TextUtils;
import cn.thinkingdata.core.utils.Base64Coder;
import cn.thinkingdata.core.utils.TDLog;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Data encryption tool class.
 * */
public class TDEncryptUtils {

    private static final String TAG = "ThinkingAnalytics.TAEncryptUtils";

    /** AES-GCM IV / nonce length in bytes. */
    static final int GCM_IV_LENGTH = 12;
    /** AES-GCM authentication tag length in bits. */
    static final int GCM_TAG_LENGTH_BITS = 128;

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Generate an AES encryption key.
     *
     * @return byte[]
     * @throws NoSuchAlgorithmException Exception
     */
    static byte[] generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        return aesKey.getEncoded();
    }

    /**
     * Asymmetric encryption AES key.
     *
     * @return Encrypted public key
     */
    static String rsaEncrypt(String publicKey, byte[] content) {
        if (TextUtils.isEmpty(publicKey)) {
            TDLog.i(TAG, "PublicKey is null.");
            return null;
        }
        try {
            byte[] keyBytes = TDBase64.decode(publicKey);
            KeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            Key rsaPublicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            byte[] encryptedData = cipher.doFinal(content);
            return new String(Base64Coder.encode(encryptedData));
        } catch (Exception ex) {
            TDLog.d(TAG, "RSA Encryption Fail:" + ex.getMessage());
        }
        return null;
    }

    /**
     * AES-GCM encrypt. Payload is Base64(IV || ciphertext || tag).
     *
     * @param key Key
     * @return Encrypted data
     */
    static String aesEncrypt(byte[] key, String content) {
        if (key == null || content == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);
            byte[] encryptedBytes = cipher.doFinal(content.getBytes(UTF_8));

            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
            return new String(Base64Coder.encode(combined));
        } catch (Exception ex) {
            TDLog.d(TAG, "AES Encryption Fail:" + ex.getMessage());
        }
        return null;
    }

    /**
     * Whether the data is encrypted.
     *
     * @param jsonStr String
     * @return boolean
     */
    public static boolean isEncryptedData(String jsonStr) {
        if (jsonStr == null) {
            return false;
        }

        return jsonStr.contains("ekey") && jsonStr.contains("pkv") && jsonStr.contains("payload");
    }

}
