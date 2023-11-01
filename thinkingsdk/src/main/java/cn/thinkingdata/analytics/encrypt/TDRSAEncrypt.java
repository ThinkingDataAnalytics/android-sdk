/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.encrypt;

/**
 * RSA + AES.
 */
public class TDRSAEncrypt implements ITDEncrypt {

    byte[] aesKey; //AES secret key

    String mEncryptKey; //RSA encryption key

    @Override
    public String symmetricEncryptType() {
        return "AES";
    }

    @Override
    public String encryptDataEvent(String event) {
        return TDEncryptUtils.aesEncrypt(aesKey, event);
    }

    @Override
    public String asymmetricEncryptType() {
        return "RSA";
    }

    @Override
    public String encryptSymmetricKey(String publicKey) {
        try {
            aesKey = TDEncryptUtils.generateAESKey();
            mEncryptKey = TDEncryptUtils.rsaEncrypt(publicKey, aesKey);
        } catch (Exception e) {
            return null;
        }
        return mEncryptKey;
    }
}
