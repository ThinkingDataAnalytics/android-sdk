/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * TDContextConfig 为全局配置，针对该 Context 下所有实例生效. 其配置只能通过 AndroidManifext.xml 设置.
 */
public class TDContextConfig {
    private static final String KEY_MAIN_PROCESS_NAME = "TADeFaultMainProcessName";

    private static final int DEFAULT_RETENTION_DAYS = 10; // 本地缓存数据默认保留 10 天  最长为10天
    private static final int DEFAULT_MIN_DB_LIMIT = 10000; // 数据库文件最小大小，默认 10000 条. 最小为5000

    // 设置数据保留天数，默认 10 天
    private static final String KEY_RETENTION_DAYS = "TARetentionDays";
    // 数据库文件存储条数限制
    private static final String KEY_MIN_DB_LIMIT = "TADatabaseLimit";

    private static final Map<Context, TDContextConfig> sInstanceMap = new HashMap<>();

    private String mMainProcessName;
    private int mRetentionDays = DEFAULT_RETENTION_DAYS;
    private int mMinimumDatabaseLimit = DEFAULT_MIN_DB_LIMIT;

    /**
     * < 全局配置类 >.
     *
     * @param context 上下文
     * @return {@link TDContextConfig}
     */
    public  static TDContextConfig getInstance(Context context) {
        synchronized (sInstanceMap) {
            TDContextConfig contextConfig = sInstanceMap.get(context);
            if (null == contextConfig) {
                contextConfig = new TDContextConfig(context);
                sInstanceMap.put(context, contextConfig);
            }
            return contextConfig;
        }
    }

    private TDContextConfig(Context context) {
        Resources resources = context.getResources();
        String packageName = context.getPackageName();
        try {
            mMainProcessName = packageName;
            mMainProcessName = resources.getString(resources.getIdentifier(KEY_MAIN_PROCESS_NAME, "string", packageName));
        } catch (Exception e) {
            //ignored
        }
        try {
            mRetentionDays = resources.getInteger(resources.getIdentifier(KEY_RETENTION_DAYS, "integer", packageName));
        } catch (Exception e) {
            //ignored
        }
        try {
            mMinimumDatabaseLimit = resources.getInteger(resources.getIdentifier(KEY_MIN_DB_LIMIT, "integer", packageName));
        } catch (Exception e) {
            //ignored
        }
        TDPresetProperties.initDisableList(context);
    }

    public String getMainProcessName() {
        return mMainProcessName;
    }

    int getMinimumDatabaseLimit() {
        return Math.max(mMinimumDatabaseLimit, 5000);
    }

    // Throw away records that are older than this in milliseconds. Should be below the server side age limit for events.
    long getDataExpiration() {
        return 1000L * 60 * 60 * 24 * ((mRetentionDays > 10 || mRetentionDays < 0) ? DEFAULT_RETENTION_DAYS : mRetentionDays); // 10 days default
    }
}
