/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.encrypt;

public interface ITDEncrypt {

    /**
     * Symmetric encryption type.
     *
     * @return encryption type
     */
    String symmetricEncryptType();

    /**
     * Encrypt the buried data.
     *
     * @param event Data before encryption
     * @return Encrypted data
     */
    String encryptDataEvent(String event);

    /**
     *  Asymmetric encryption type.
     *
     * @return encryption type
     */
    String asymmetricEncryptType();

    /**
     * Encryption symmetric encryption key.
     *
     * @return secret key
     */
    String encryptSymmetricKey(String publicKey);

}
