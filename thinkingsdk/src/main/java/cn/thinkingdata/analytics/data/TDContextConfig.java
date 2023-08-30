/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.data;

import android.content.Context;
import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.analytics.TDPresetProperties;

/**
 *  The TDContextConfig configuration is global and takes effect for all instances in the Context.
 *  It can only be configured by AndroidManifest.xml.
 */
public class TDContextConfig {
    private static final String KEY_MAIN_PROCESS_NAME = "TADeFaultMainProcessName";

    private static final int DEFAULT_RETENTION_DAYS = 10; // By default, local cached data is reserved for 10 days at most
    private static final int DEFAULT_MIN_DB_LIMIT = 10000; // Minimum size of database files. The default number is 10000. The minimum is 5000

    // The default data retention period is 10 days
    private static final String KEY_RETENTION_DAYS = "TARetentionDays";
    // Limit the number of database files to be stored
    private static final String KEY_MIN_DB_LIMIT = "TADatabaseLimit";

    private static final Map<Context, TDContextConfig> sInstanceMap = new HashMap<>();

    private String mMainProcessName;
    private int mRetentionDays = DEFAULT_RETENTION_DAYS;
    private int mMinimumDatabaseLimit = DEFAULT_MIN_DB_LIMIT;

    /**
     * Global Configuration class
     *
     * @param context context
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

    public int getMinimumDatabaseLimit() {
        return Math.max(mMinimumDatabaseLimit, 5000);
    }

    // Throw away records that are older than this in milliseconds. Should be below the server side age limit for events.
    public long getDataExpiration() {
        return 1000L * 60 * 60 * 24 * ((mRetentionDays > 10 || mRetentionDays < 0) ? DEFAULT_RETENTION_DAYS : mRetentionDays); // 10 days default
    }
}
