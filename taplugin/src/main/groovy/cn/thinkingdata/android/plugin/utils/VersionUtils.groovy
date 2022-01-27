package cn.thinkingdata.android.plugin.utils

import java.lang.reflect.Field

class VersionUtils {
    // TA SDK 版本号
    public static String thinkingSDKVersion = ""

    //支持此插件最低需要的SDK版本
    public static final String MIN_SDK_VERSION = "2.7.6"

    /**
     * 读取TA Android 埋点 SDK 版本号
     * @param urlClassLoader ClassLoader
     */
    public static void loadAndroidSDKVersion(URLClassLoader urlClassLoader) {
        try {
            Class sensorsDataAPI = urlClassLoader.loadClass("cn.thinkingdata.android.TDConfig");
            Field versionField = sensorsDataAPI.getDeclaredField("VERSION");
            versionField.setAccessible(true);
            thinkingSDKVersion = (String) versionField.get(null);
            Logger.info("TA埋点 SDK 版本号:" + thinkingSDKVersion);
        } catch(Throwable throwable) {
            Logger.info("TA埋点 SDK 版本号读取失败，reason: " + throwable.getMessage());
        }
    }

}