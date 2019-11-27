package cn.thinkingdata.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.android.utils.TDLog;

public class TDContextConfig {
    private static final String KEY_AUTO_TRACK = "cn.thinkingdata.android.AutoTrack";
    private static final String KEY_MAIN_PROCESS_NAME = "cn.thinkingdata.android.MainProcessName";
    private static final String KEY_ENABLE_LOG = "cn.thinkingdata.android.EnableTrackLogging";

    private final static  Map<Context, TDContextConfig> sInstanceMap = new HashMap<>();

    private final String mMainProcessName;
    private final boolean mAutoTrack;
    private long mDataExpiration = 1000 * 60 * 60 * 24 * 15; // 15 days default
    private int mMinimumDatabaseLimit = 32 * 1024 * 1024;  // 32 M default

    public static TDContextConfig getInstance(Context context) {
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

        final String packageName = context.getApplicationContext().getPackageName();
        final ApplicationInfo appInfo;
        Bundle configBundle = null;
        try {
            appInfo = context.getApplicationContext().getPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            configBundle = appInfo.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (null == configBundle) {
            configBundle = new Bundle();
        }
        mAutoTrack = configBundle.getBoolean(KEY_AUTO_TRACK,
                false);
        mMainProcessName = configBundle.getString(KEY_MAIN_PROCESS_NAME);

        if (configBundle.containsKey(KEY_ENABLE_LOG)) {
            boolean enableTrackLog = configBundle.getBoolean(KEY_ENABLE_LOG, false);
            TDLog.setEnableLog(enableTrackLog);
        }
    }

    String getMainProcessName() {
        return mMainProcessName;
    }

    boolean getAutoTrackConfig() {
        return mAutoTrack;
    }

    public int getMinimumDatabaseLimit() {
        return mMinimumDatabaseLimit;
    }

    // Throw away records that are older than this in milliseconds. Should be below the server side age limit for events.
    public long getDataExpiration() {
        return mDataExpiration;
    }

    public void setMinimumDatabaseLimit(int limit) {
        mMinimumDatabaseLimit = limit;

    }

    public void setDataExpiration(int hours) {
        mDataExpiration = 1000 * 60 * 60 * hours;
    }
}
