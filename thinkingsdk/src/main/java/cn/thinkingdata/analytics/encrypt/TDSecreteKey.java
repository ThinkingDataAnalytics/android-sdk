/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.encrypt;

/**
 * Encapsulate the key class.
 * */
public class TDSecreteKey {

    public String publicKey;

    //Public key version
    public int version;

    //Symmetric encryption type
    public String symmetricEncryption;

    //Asymmetric encryption type
    public String asymmetricEncryption;

    public TDSecreteKey() {
    }

    public TDSecreteKey(String publicKey, int version, String symmetricEncryption, String asymmetricEncryption) {
        this.publicKey = publicKey;
        this.version = version;
        this.symmetricEncryption = symmetricEncryption;
        this.asymmetricEncryption = asymmetricEncryption;
    }
}
