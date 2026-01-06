package cn.thinkingdata.core.receiver;

import org.json.JSONObject;

public interface TDObservable {

    void  registerObserver(TDObserver observer);

    void unregisterObserver(TDObserver observer);

    void onSdkInitCalled(String appId);

    void onLoginMethodCalled(String accountId,String distinctId,String appId);

    void onSetDistinctIdMethodCalled(String accountId,String distinctId,String appId);

    void onLogoutMethodCalled(String distinctId,String appId);

    void onDataEnqueued(String appId, JSONObject data);

    void onTimeCalibrated();

}
