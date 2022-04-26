package cn.thinkingdata.android.encrypt;
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

    public TDSecreteKey(String publicKey, int version, String symmetricEncryption, String asymmetricEncryption) {
        this.publicKey = publicKey;
        this.version = version;
        this.symmetricEncryption = symmetricEncryption;
        this.asymmetricEncryption = asymmetricEncryption;
    }
}
