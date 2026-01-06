/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

/**
 * network status change receiver
 *
 * @author bugliee
 * @since 2022/9/22
 */
public class NetworkReceiver extends BroadcastReceiver {

    public interface ConnectivityListener {
        void onChanged();
    }

    private ConnectivityListener listener;

    public void setListener(ConnectivityListener listener) {
        this.listener = listener;
    }

    public NetworkReceiver(ConnectivityListener listener) {
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