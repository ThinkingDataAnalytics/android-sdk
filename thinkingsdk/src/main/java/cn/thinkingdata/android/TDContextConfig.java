package cn.thinkingdata.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.android.utils.TDLog;

/**
 * TDContextConfig 为全局配置，针对该 Context 下所有实例生效. 其配置只能通过 AndroidManifext.xml 设置
 */
class TDContextConfig {
    private static final String KEY_MAIN_PROCESS_NAME = "cn.thinkingdata.android.MainProcessName";

    private static final int DEFAULT_RETENTION_DAYS = 15; // 本地缓存数据默认保留 15 天
    private static final int DEFAULT_QUIT_SAFELY_TIME_OUT = 2000; // 安全退出时，超时时长, 默认 2000ms
    private static final int DEFAULT_MIN_DB_LIMIT = 32; // 数据库文件最小大小，默认 32 M.

    // 是否打开日志
    private static final String KEY_ENABLE_LOG = "cn.thinkingdata.android.EnableTrackLogging";
    // 设置数据保留天数，默认 15 天
    private static final String KEY_RETENTION_DAYS = "cn.thinkingdata.android.RetentionDays";
    // 数据库文件最小大小，单位 Mb; 当系统空间不足时，缓存数据库达到此上限后会删除最老的 100 条数据.
    private static final String KEY_MIN_DB_LIMIT = "cn.thinkingdata.android.MinimumDatabaseLimit";
    // 是否允许退出时等待工作线程安全退出，默认允许
    private static final String KEY_ENABLE_QUIT_SAFELY = "cn.thinkingdata.android.EnableQuitSafely";
    // 在允许退出时等待工作线程安全退出的情况下，请求超时时长
    private static final String KEY_QUIT_SAFELY_TIMEOUT = "cn.thinkingdata.android.QuitSafelyTimeout";

    private final static  Map<Context, TDContextConfig> sInstanceMap = new HashMap<>();

    private final int mRetentionDays;
    private final boolean mEnableQuitSafely;
    private final int mQuitSafelyTimeout;
    private final String mMainProcessName;
    private final int mMinimumDatabaseLimit;

    static TDContextConfig getInstance(Context context) {
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
        mMainProcessName = configBundle.getString(KEY_MAIN_PROCESS_NAME);

        int retentionDays = configBundle.getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS);
        mRetentionDays = retentionDays > 0 ? retentionDays : DEFAULT_RETENTION_DAYS;

        mEnableQuitSafely = configBundle.getBoolean(KEY_ENABLE_QUIT_SAFELY, false);
        int quitSafelyTimeout = configBundle.getInt(KEY_QUIT_SAFELY_TIMEOUT, DEFAULT_QUIT_SAFELY_TIME_OUT);
        mQuitSafelyTimeout = quitSafelyTimeout > 0 ? quitSafelyTimeout : DEFAULT_QUIT_SAFELY_TIME_OUT;

        int minDatabaseLimit = configBundle.getInt(KEY_MIN_DB_LIMIT, DEFAULT_MIN_DB_LIMIT);
        mMinimumDatabaseLimit = minDatabaseLimit > 0 ? minDatabaseLimit: DEFAULT_MIN_DB_LIMIT;

        if (configBundle.containsKey(KEY_ENABLE_LOG)) {
            boolean enableTrackLog = configBundle.getBoolean(KEY_ENABLE_LOG, false);
            TDLog.setEnableLog(enableTrackLog);
        }
    }

    String getMainProcessName() {
        return mMainProcessName;
    }

    boolean quitSafelyEnabled() {
        return mEnableQuitSafely;
    }

    int getQuitSafelyTimeout() {
        return mQuitSafelyTimeout;
    }

    int getMinimumDatabaseLimit() {
        return mMinimumDatabaseLimit * 1024 * 1024;
    }

    // Throw away records that are older than this in milliseconds. Should be below the server side age limit for events.
    long getDataExpiration() {
        return 1000 * 60 * 60 * 24 * mRetentionDays; // 15 days default
    }
}
