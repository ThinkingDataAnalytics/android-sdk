/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

/**
 * <  >.
 *
 * @author bugliee
 * @create 2022/9/22
 * @since 1.0.0
 */
public class NetworkReceiver extends BroadcastReceiver {

    public interface ConnectivityListener {
        void onChanged();
    }

    private ConnectivityListener listener;

    public void setListener(ConnectivityListener listener) {
        this.listener = listener;
    }

    NetworkReceiver(ConnectivityListener listener) {
        setListener(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (listener != null) {
                listener.onChanged();
            }
        }
    }
}