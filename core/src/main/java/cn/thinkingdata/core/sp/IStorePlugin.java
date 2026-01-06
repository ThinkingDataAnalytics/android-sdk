package cn.thinkingdata.core.sp;

public interface IStorePlugin {

    String encrypt(String src);

    String decrypt(String dest);

}
