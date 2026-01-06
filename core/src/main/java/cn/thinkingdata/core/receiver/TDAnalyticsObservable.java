/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.receiver;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * <  >.
 *
 * @author liulongbing
 * @since 2024/1/12
 */
public class TDAnalyticsObservable implements TDObservable {

    private volatile static TDAnalyticsObservable instance = null;

    private final List<TDObserver> observers;

    private TDAnalyticsObservable() {
        observers = new ArrayList<>();
    }

    public static TDAnalyticsObservable getInstance() {
        if (instance == null) {
            synchronized (TDAnalyticsObservable.class) {
                if (instance == null) {
                    instance = new TDAnalyticsObservable();
                }
            }
        }
        return instance;
    }

    @Override
    public void registerObserver(TDObserver observer) {
        synchronized (this) {
            observers.add(observer);
        }
    }

    @Override
    public void unregisterObserver(TDObserver observer) {
        synchronized (this) {
            observers.remove(observer);
        }
    }

    @Override
    public void onSdkInitCalled(String appId) {
        synchronized (this) {
            for (TDObserver observer : observers) {
                if (observer == null) continue;
                observer.onSdkInitReceived(appId);
            }
        }
    }

    @Override
    public void onLoginMethodCalled(String accountId, String distinctId, String appId) {
        synchronized (this) {
            for (TDObserver observer : observers) {
                if (observer == null) continue;
                observer.onLoginReceived(accountId, distinctId, appId);
            }
        }
    }

    @Override
    public void onSetDistinctIdMethodCalled(String accountId, String distinctId, String appId) {
        synchronized (this) {
            for (TDObserver observer : observers) {
                if (observer == null) continue;
                observer.onDistinctIdReceived(accountId, distinctId, appId);
            }
        }
    }

    @Override
    public void onLogoutMethodCalled(String distinctId, String appId) {
        synchronized (this) {
            for (TDObserver observer : observers) {
                if (observer == null) continue;
                observer.onLogoutReceived(distinctId, appId);
            }
        }
    }

    @Override
    public void onDataEnqueued(String appId, JSONObject data) {
        synchronized (this) {
            for (TDObserver observer : observers) {
                if (observer == null) continue;
                observer.onDataEnqueued(appId, data);
            }
        }
    }

    @Override
    public void onTimeCalibrated() {
        synchronized (this) {
            for (TDObserver observer : observers) {
                if (observer == null) continue;
                observer.onTimeCalibrated();
            }
        }
    }

}
