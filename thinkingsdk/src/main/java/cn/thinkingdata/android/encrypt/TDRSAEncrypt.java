package cn.thinkingdata.android.encrypt;

/**
 * RSA + AES
 */
public class TDRSAEncrypt implements ITDEncrypt {

    byte[] aesKey; //AES密钥

    String mEncryptKey; //RSA加密之后的密钥

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
