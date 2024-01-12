/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;
import android.content.Context;

/**
 * @author liulongbing
 * @since 2022/9/8
 */
public class GlobalStorageManager {

    private final GlobalStoragePlugin storagePlugin;

    private static final Object sRandomIDLock = new Object();
    private static final Object sOldLoginIdLock = new Object();

    private GlobalStorageManager(Context context) {
        storagePlugin = new GlobalStoragePlugin(context);
    }

    private static GlobalStorageManager instance;

    public static GlobalStorageManager getInstance(Context context) {
        if (instance == null) {
            synchronized (GlobalStorageManager.class) {
                if (instance == null) {
                    instance = new GlobalStorageManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * @return Obtain the last installation time
     */
    public Long getLastInstallTime(){
        return storagePlugin.get(LocalStorageType.LAST_INSTALL);
    }

    /**
     * @param installTime Save the last installation time
     */
    public void saveLastInstallTime(Long installTime){
        storagePlugin.save(LocalStorageType.LAST_INSTALL,installTime);
    }

    /**
     * @param deviceId Save device id
     */
    public void saveRandomDeviceId(String deviceId){
        storagePlugin.save(LocalStorageType.DEVICE_ID,deviceId);
    }

    /**
     * @return Get device id
     */
    public String getRandomDeviceID(){
        return storagePlugin.get(LocalStorageType.DEVICE_ID);
    }

    /**
     * @return get random
     */
    public String getRandomID(){
        synchronized (sRandomIDLock) {
            return storagePlugin.get(LocalStorageType.RANDOM_ID);
        }
    }

    /**
     * @return old login id
     */
    public String getOldLoginId(){
        synchronized (sOldLoginIdLock) {
            return storagePlugin.get(LocalStorageType.LOGIN_ID);
        }
    }

    /**
     * Clear the old device ID
     */
    public void clearOldLoginId(){
        synchronized (sOldLoginIdLock) {
            storagePlugin.save(LocalStorageType.LOGIN_ID,null);
        }
    }

}
