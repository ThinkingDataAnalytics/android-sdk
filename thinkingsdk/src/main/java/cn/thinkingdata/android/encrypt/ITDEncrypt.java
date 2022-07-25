/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.encrypt;

/**
 * ITDEncrypt 数据加密接口.
 */
public interface ITDEncrypt {

    /**
     * 对称加密类型.
     *
     * @return 加密类型
     */
    String symmetricEncryptType();

    /**
     * 加密埋点数据.
     *
     * @param event 加密前的数据
     * @return 加密后的数据
     */
    String encryptDataEvent(String event);

    /**
     *  非对称加密类型.
     *
     * @return 加密类型
     */
    String asymmetricEncryptType();

    /**
     * 加密对称加密钥匙.
     *
     * @return 密钥
     */
    String encryptSymmetricKey(String publicKey);

}
