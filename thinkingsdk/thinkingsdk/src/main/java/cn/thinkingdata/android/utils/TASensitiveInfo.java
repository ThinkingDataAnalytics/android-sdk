/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

/**
 * < 获取隐私信息相关类 >.
 *
 * @author bugliee
 * @create 2022/5/16
 * @since 1.0.0
 */
public class TASensitiveInfo {

    /**
     * < 获取AndroidID >.
     *
     * @author bugliee
     * @create 2022/5/16
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
