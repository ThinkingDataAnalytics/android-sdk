/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.sp;

/**
 * @author liulongbing
 * @since 2024/8/13
 */
public class TDStorageEncryptPlugin {

    private IStorePlugin mEncryptPlugin;
    private boolean mEncryptFlag = false;

    private TDStorageEncryptPlugin() {
    }

    private static final class InstanceHolder {
        static final TDStorageEncryptPlugin instance = new TDStorageEncryptPlugin();
    }

    public static TDStorageEncryptPlugin getInstance() {
        return InstanceHolder.instance;
    }

    public void enableEncrypt() {
        this.mEncryptFlag = true;
        mEncryptPlugin = new AESEncrypt();
    }

    public boolean isEnableEncrypt() {
        return this.mEncryptFlag;
    }

    public String encryptData(String data) {
        if (this.mEncryptFlag) {
            return mEncryptPlugin.encrypt(data);
        }
        return data;
    }

    public String decryptData(String data) {
        if (this.mEncryptFlag) {
            return mEncryptPlugin.decrypt(data);
        }
        return data;
    }

}
