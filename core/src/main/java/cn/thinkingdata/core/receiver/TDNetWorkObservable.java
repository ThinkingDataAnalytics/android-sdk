/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.receiver;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author liulongbing
 * @create 2024/3/4
 * @since
 */
public class TDNetWorkObservable {

    private volatile static TDNetWorkObservable instance = null;
    // CopyOnWriteArrayList 保证网络回调线程遍历观察者时，其他线程增删观察者不会抛出 ConcurrentModificationException
    private final List<TDNetWorkObserver> observers;
    private static final int TYPE_ON_AVAILABLE = 1;
    private static final int TYPE_ON_LOST = 2;
    private static final int TYPE_ON_CAPABILITIES_CHANGED = 3;
    private static final int TYPE_ON_CHANGE = 4;

    private TDNetWorkObservable(Context context) {
        observers = new CopyOnWriteArrayList<>();
        registerNetWorkReceiver(context);
    }

    public static TDNetWorkObservable getInstance(Context context) {
        if (instance == null) {
            synchronized (TDAnalyticsObservable.class) {
                if (instance == null) {
                    instance = new TDNetWorkObservable(context);
                }
            }
        }
        return instance;
    }

    private void registerNetWorkReceiver(Context context) {
        if (context == null) return;
        Context mContext = context.getApplicationContext();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ConnectivityManager connectivityManager = ( ConnectivityManager ) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        notifyObservers(TYPE_ON_AVAILABLE, network, null);
                        super.onAvailable(network);
                    }

                    @Override
                    public void onLost(Network network) {
                        notifyObservers(TYPE_ON_LOST, network, null);
                        super.onLost(network);
                    }

                    @Override
                    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                        notifyObservers(TYPE_ON_CAPABILITIES_CHANGED, network, networkCapabilities);
                        super.onCapabilitiesChanged(network, networkCapabilities);
                    }
                });
            } else {
                NetworkReceiver receiver = new NetworkReceiver(new NetworkReceiver.ConnectivityListener() {
                    @Override
                    public void onChanged() {
                        notifyObservers(TYPE_ON_CHANGE, null, null);
                    }
                });
                mContext.registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyObservers(int type, Network network, NetworkCapabilities networkCapabilities) {
        for (TDNetWorkObserver observer : observers) {
            if (observer == null) {
                continue;
            }
            switch (type) {
                case TYPE_ON_AVAILABLE:
                    observer.onAvailable(network);
                    break;
                case TYPE_ON_LOST:
                    observer.onLost(network);
                    break;
                case TYPE_ON_CAPABILITIES_CHANGED:
                    observer.onCapabilitiesChanged(network, networkCapabilities);
                    break;
                case TYPE_ON_CHANGE:
                    observer.onChange();
                    break;
            }
        }
    }

    public void addNetWorkObserver(TDNetWorkObserver observer) {
        observers.add(observer);
    }

    public void removeNetWorkObserver(TDNetWorkObserver observer) {
        observers.remove(observer);
    }

}
