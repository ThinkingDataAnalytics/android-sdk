/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.persistence;
import android.content.Context;
/**
 * 全局存储 单例
 *
 * @author liulongbing
 * @create 2022/9/8
 * @since
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
     * 获取上次安装时间
     * @return
     */
    public Long getLastInstallTime(){
        return storagePlugin.get(LocalStorageType.LAST_INSTALL);
    }

    /**
     * 保存上次安装时间
     * @param installTime
     */
    public void saveLastInstallTime(Long installTime){
        storagePlugin.save(LocalStorageType.LAST_INSTALL,installTime);
    }

    /**
     * 保存设备号
     * @param deviceId
     */
    public void saveRandomDeviceId(String deviceId){
        storagePlugin.save(LocalStorageType.DEVICE_ID,deviceId);
    }

    /**
     * 获取设备号
     * @return
     */
    public String getRandomDeviceID(){
        return storagePlugin.get(LocalStorageType.DEVICE_ID);
    }

    /**
     * 获取随机数
     * @return
     */
    public String getRandomID(){
        synchronized (sRandomIDLock) {
            return storagePlugin.get(LocalStorageType.RANDOM_ID);
        }
    }

    /**
     * 获取老账号ID
     * @return
     */
    public String getOldLoginId(){
        synchronized (sOldLoginIdLock) {
            return storagePlugin.get(LocalStorageType.LOGIN_ID);
        }
    }

    /**
     * 清除老设备ID
     */
    public void clearOldLoginId(){
        synchronized (sOldLoginIdLock) {
            storagePlugin.save(LocalStorageType.LOGIN_ID,null);
        }
    }

}
