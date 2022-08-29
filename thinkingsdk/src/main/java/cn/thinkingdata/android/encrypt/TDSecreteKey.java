/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.encrypt;

/**
 * 封装密钥类.
 * */
public class TDSecreteKey {

    //公钥
    public String publicKey;

    //公钥版本
    public int version;

    //对称加密类型
    public String symmetricEncryption;

    //非对称加密类型
    public String asymmetricEncryption;

    public TDSecreteKey() {
    }

    /**
     * < TDSecreteKey >.
     *
     * @param publicKey 公钥
     * @param version 版本
     * @param symmetricEncryption 对称加密
     * @param asymmetricEncryption 非对称加密
     */
    public TDSecreteKey(String publicKey, int version, String symmetricEncryption, String asymmetricEncryption) {
        this.publicKey = publicKey;
        this.version = version;
        this.symmetricEncryption = symmetricEncryption;
        this.asymmetricEncryption = asymmetricEncryption;
    }
}
