/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.core.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

/**
 * Obtain privacy information related classes
 *
 * @author bugliee
 * @since 2022/5/16
 */
public class TASensitiveInfo {

    /**
     * get AndroidID
     *
     * @author bugliee
     * @since  2022/5/16
     * @param mContext Context
     * @return {@link String}
     */
    @SuppressLint("HardwareIds")
    public String getAndroidID(Context mContext) {
        try {
            return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
