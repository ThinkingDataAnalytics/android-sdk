/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.session;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import cn.thinkingdata.android.persistence.CommonStorageManager;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/11/29
 * @since
 */
public class SessionManager {

    private static final Map<String, SessionManager> sessionInstances = new HashMap<>();

//    private final CommonStorageManager storageManager;
    private String mSessionId;

    private final Object mSessionObj = new Object();

    private int mIndex = 0;

    private boolean hasInstallSession = false;

    public SessionManager(String appId, CommonStorageManager storageManager) {
//        this.storageManager = storageManager;
//        sessionInstances.put(appId, this);
    }

    /**
     * Get SessionManager
     *
     * @param appId
     * @return
     */
    public static SessionManager getSessionManager(String appId) {
        return sessionInstances.get(appId);
    }

    /**
     * generate session id
     *
     * @return
     */
    public void generateSessionID() {
//        if (hasInstallSession) {
//            hasInstallSession = false;
//            return;
//        }
//        synchronized (mSessionObj) {
//            String uuid = UUID.randomUUID().toString();
//            if (null == storageManager) {
//                //light instance
//                mIndex++;
//                mSessionId = uuid + "_" + mIndex;
//            } else {
//                int index = storageManager.getSessionIndex();
//                index++;
//                mSessionId = uuid + "_" + index;
//                storageManager.saveSessionIndex(index);
//            }
//        }
    }

    /**
     * generate session id from install
     */
    public void generateInstallSessionID() {
//        generateSessionID();
//        hasInstallSession = true;
    }

    /**
     * get current session id
     *
     * @return
     */
    public String getSessionId() {
//        synchronized (mSessionObj) {
//            return mSessionId;
//        }
        return "";
    }

}
