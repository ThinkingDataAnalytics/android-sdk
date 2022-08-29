/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.encrypt;

import android.text.TextUtils;
import cn.thinkingdata.android.utils.Base64Coder;
import cn.thinkingdata.android.utils.TDLog;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 数据加密工具类.
 * */
public class TDEncryptUtils {

    private static final String TAG = "ThinkingAnalytics.TAEncryptUtils";

    /**
     * 生成AES加密密钥.
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
     * 非对称加密 AES key.
     *
     * @return 加密后的公钥
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
            TDLog.d(TAG, "AES加密失败:" + ex.getMessage());
        }
        return null;
    }

    /**
     * AES 加密.
     *
     * @param key Key
     * @return 加密后的数据
     */
    static String aesEncrypt(byte[] key, String content) {

        if (key == null || content == null) {
            return null;
        }
        byte[] contentBytes = content.getBytes();

        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptedBytes = cipher.doFinal(contentBytes);
            return new String(Base64Coder.encode(encryptedBytes));
        } catch (Exception ex) {
            TDLog.d(TAG, "RSA加密失败:" + ex.getMessage());
        }
        return null;
    }

    /**
     * 是否包含加密数据.
     *
     * @param array JSONArray
     * @return boolean
     */
    public static boolean hasEncryptedData(JSONArray array) {
        try {
            for (int i = 0; i < array.length(); i++) {
                if (isEncryptedData(array.getJSONObject(i))) {
                    return true;
                }
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    /**
     * 是否是加密数据.
     *
     * @param json JSONObject
     * @return boolean
     */
    public static boolean isEncryptedData(JSONObject json) {
        if (json == null) {
            return false;
        }
        return json.length() == 3 && json.has("ekey") && json.has("pkv") && json.has("payload");
    }

}
