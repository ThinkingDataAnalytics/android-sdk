package cn.thinkingdata.android.plugin.utils

import java.lang.reflect.Field

/**
 * 版本工具类
 */
class ThinkingVersionUtils {
    // TA SDK 版本号
    public static String thinkingSDKVersion = ""

    //SDK至少需要2.7.6
    public static final String MIN_SDK_VERSION = "2.7.6"

    /**
     * 读取TA 埋点 SDK 版本号
     */
    public static void loadAndroidSDKVersion(URLClassLoader urlClassLoader) {
        try {
            Class thinkingDataAPI = urlClassLoader.loadClass("cn.thinkingdata.android.TDConfig");
            Field mVersionField = thinkingDataAPI.getDeclaredField("VERSION");
            mVersionField.setAccessible(true);
            thinkingSDKVersion = (String) mVersionField.get(null);
            LoggerUtil.info("TA SDK 版本号:" + thinkingSDKVersion);
        } catch(Throwable throwable) {
            LoggerUtil.info("TA SDK 版本号读取失败: " + throwable.getMessage());
        }
    }

}