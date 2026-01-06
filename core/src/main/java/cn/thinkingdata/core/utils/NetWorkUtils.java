/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2024/5/22
 * @since
 */
public class NetWorkUtils {
    public static boolean isNetWorkAvailable(Context context) {
        try {
            ConnectivityManager connectivityManager = ( ConnectivityManager ) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}
