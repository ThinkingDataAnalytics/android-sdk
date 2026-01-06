package cn.thinkingdata.core.receiver;

import android.net.Network;
import android.net.NetworkCapabilities;

public interface TDNetWorkObserver {

    void onAvailable(Network network);

    void onLost(Network network);

    void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities);

    void onChange();

}
