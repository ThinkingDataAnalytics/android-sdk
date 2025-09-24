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
    public Long getLastInstallTime() {
        Long time = 0L;
        Object obj = storagePlugin.get(LocalStorageType.LAST_INSTALL);
        if (obj instanceof Long) {
            time = (Long) obj;
        }
        return time;
    }

    /**
     * @param installTime Save the last installation time
     */
    public void saveLastInstallTime(Long installTime) {
        storagePlugin.save(LocalStorageType.LAST_INSTALL, installTime);
    }

    /**
     * @return get random
     */
    public String getRandomID() {
        synchronized (sRandomIDLock) {
            String id = "";
            Object obj = storagePlugin.get(LocalStorageType.RANDOM_ID);
            if (obj instanceof String) {
                id = (String) obj;
            }
            return id;
        }
    }

    /**
     * @return old login id
     */
    public String getOldLoginId() {
        synchronized (sOldLoginIdLock) {
            String id = "";
            Object obj = storagePlugin.get(LocalStorageType.LOGIN_ID);
            if (obj instanceof String) {
                id = (String) obj;
            }
            return id;
        }
    }

    /**
     * Clear the old device ID
     */
    public void clearOldLoginId() {
        synchronized (sOldLoginIdLock) {
            storagePlugin.save(LocalStorageType.LOGIN_ID, "");
        }
    }

}
