package cn.thinkingdata.android.demo;

import android.text.TextUtils;

import org.json.JSONArray;

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import cn.thinkingdata.android.encrypt.TDBase64;
import cn.thinkingdata.android.utils.Base64Coder;
import cn.thinkingdata.android.utils.TDLog;

public class TDEncryptUtils {

    private static final String TAG = "ThinkingAnalytics.TAEncryptUtils";

    /**
     * 生成AES加密密钥
     *
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static byte[] generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        return aesKey.getEncoded();
    }

    /**
     * 非对称加密 AES key
     *
     * @return
     */
    public static String rsaEncrypt(String publicKey, byte[] content) {
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
     * AES 加密
     *
     * @param key
     * @return
     */
    public static String aesEncrypt(byte[] key, String content) {

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
     * 是否包含加密数据
     *
     * @param array
     * @return
     */
    public static boolean isEncryptedData(JSONArray array) {
        try {
            for (int i = 0; i < array.length(); i++) {
                if (array.getJSONObject(i).has("ekey")) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

}
