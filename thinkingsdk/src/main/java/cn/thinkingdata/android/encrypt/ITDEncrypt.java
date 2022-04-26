package cn.thinkingdata.android.encrypt;

public interface ITDEncrypt {

    /**
     * 对称加密类型
     * @return
     */
    String symmetricEncryptType();

    /**
     * 加密埋点数据
     * @param event
     * @return
     */
    String encryptDataEvent(String event);

    /**
     *  非对称加密类型
     * @return
     */
    String asymmetricEncryptType();

    /**
     * 加密对称加密钥匙
     * @return
     */
    String encryptSymmetricKey(String publicKey);

}
