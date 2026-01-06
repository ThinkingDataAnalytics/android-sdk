/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.receiver;

import org.json.JSONObject;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2024/1/12
 * @since
 */
public interface TDObserver {

    void onSdkInitReceived(String appId);

    void onLoginReceived(String accountId,String distinctId,String appId);

    void onDistinctIdReceived(String accountId,String distinctId,String appId);

    void onLogoutReceived(String distinctId,String appId);

    void onDataEnqueued(String appId, JSONObject data);

    void onTimeCalibrated();

}
