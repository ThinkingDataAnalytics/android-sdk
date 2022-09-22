package cn.thinkingdata.android.plugin.config

import com.android.build.gradle.AppExtension

/**
 * 插件扫描工具类
 */
class ThinkingAnalyticsTransformHelper {

    ThinkingAnalyticsExtension extension
    AppExtension android

    boolean enableTAMultiThread
    boolean enableTAIncremental
    boolean enableTASensitiveInfoFilter
    boolean isAddOnMethodEnter

    /**
     * 需要排除的隐私属性 默认为空
     */
    HashSet<String> sensitiveExclude = new HashSet<>([])

    /**
     * 隔离掉的隐私属性相关方法
     */
    HashSet<String> disableMethods = new HashSet<>([])

    /**
     * 隐私属性和类及方法对应关系
     */
    public static final Map<String, String> sensitiveIndex = new HashMap<>()

    /**
     * 默认需要排除的类
     */
    HashSet<String> exclude = new HashSet<>(['cn.thinkingdata.android',
                                             'androidx',
                                             'com.bumptech.glide',
                                             "com.heytap.msp.push",
                                             "com.xiaomi.mipush.sdk",
                                             "com.igexin",
                                             'com.google.android',
                                             'android.arch',
                                             'com.qiyukf',
                                             "com.tencent.smtt",
                                             "com.meizu.cloud.pushsdk",
                                             "com.vivo.push",
                                             "cn.jiguang",
                                             "com.umeng.message",
                                             "cn.jpush.android",
                                             "com.huawei.hms",
                                             "com.xiaomi.push",
                                             "com.getui",
                                             'android.support'
    ])
    /**
     * 只扫描指定的 默认为空
     */
    HashSet<String> include = new HashSet<>([])


    URLClassLoader mUrlClassLoader

    ThinkingAnalyticsTransformHelper(ThinkingAnalyticsExtension extension, AppExtension android) {
        this.extension = extension
        this.android = android
    }

    File androidJar() throws FileNotFoundException {
        File jar = new File(getSdkJarPath(), "android.jar")
        if (!jar.exists()) {
            throw new FileNotFoundException("Android jar not found!")
        }
        return jar
    }

    private String getSdkJarPath() {
        String compileSdkVersion = android.getCompileSdkVersion()
        return String.join(File.separator, android.getSdkDirectory().getAbsolutePath(), "platforms", compileSdkVersion)
    }

    /**
     * 扫描之前合并配置参数
     */
    void beforeTransform() {
        ArrayList<String> excludePackages = extension.exclude
        if (excludePackages != null) {
            exclude.addAll(excludePackages)
        }
        ArrayList<String> includePackages = extension.include
        if (includePackages != null) {
            include.addAll(includePackages)
        }
        ArrayList<String> excludeSensitive = extension.excludeSensitive
        if (excludeSensitive != null) {
            sensitiveExclude.addAll(excludeSensitive)
        }
        initSensitiveIndexMap()
        enableTASensitiveInfoFilter = !sensitiveExclude.isEmpty()
    }

    static void initSensitiveIndexMap() {
        sensitiveIndex.put("AndroidID", "getAndroidID")
    }


    /**
     * 分析类是否需要修改
     * @param className
     * @return
     */
    ThinkingClassNameAnalytics analytics(String className) {
        ThinkingClassNameAnalytics classNameAnalytics = new ThinkingClassNameAnalytics(className)
        if (classNameAnalytics.isThinkingVersionAPI) {
            classNameAnalytics.isShouldModify = true
        } else if (classNameAnalytics.isSensitiveInfoAPI && !sensitiveExclude.isEmpty()) {
            for (sensitive in sensitiveExclude) {
                String methodStr = sensitiveIndex.get(sensitive)
                if (methodStr != null && !methodStr.trim().isEmpty()) {
                    String[] methodArr = methodStr.split("#")
                    List<String> methods = new ArrayList<>()
                    methodArr.each {
                        method ->
                            if (!method.trim().isEmpty()) {
                                methods.add(method)
                            }
                    }
                    disableMethods.addAll(methods)
                }
            }
            classNameAnalytics.isShouldModify = !disableMethods.isEmpty()
        } else if (!classNameAnalytics.isAndroidConfigClass()) {
            for (pkgName in internal) {
                if (className.startsWith(pkgName)) {
                    classNameAnalytics.isShouldModify = true
                    return classNameAnalytics
                }
            }
            if (extension.useInclude) {
                for (pkgName in include) {
                    if (className.startsWith(pkgName)) {
                        classNameAnalytics.isShouldModify = true
                        break
                    }
                }
            } else {
                classNameAnalytics.isShouldModify = true
                if (!classNameAnalytics.isLeanbackClass()) {
                    for (pkgName in exclude) {
                        if (className.startsWith(pkgName)) {
                            classNameAnalytics.isShouldModify = false
                            break
                        }
                    }
                }
            }
        }
        return classNameAnalytics
    }


    public static final HashSet<String> internal = [
            'androidx.appcompat.widget.ActionMenuPresenter$OverflowMenuButton',
            'android.support.v7.widget.ActionMenuPresenter$OverflowMenuButton',
            'cn.jpush.android.service.PluginMeizuPlatformsReceiver',
            'android.support.v4.app.NotificationManagerCompat',
            'androidx.core.app.ComponentActivity',
            'androidx.appcompat.app.ActionBarDrawerToggle',
            'android.support.v7.app.ActionBarDrawerToggle',
            'androidx.fragment.app.FragmentActivity',
            'com.google.android.material.tabs.TabLayout$ViewPagerOnTabSelectedListener',
            'androidx.core.app.NotificationManagerCompat',
            'android.support.v4.app.SupportActivity',
            'android.support.design.widget.TabLayout$ViewPagerOnTabSelectedListener',
            'android.widget.ActionMenuPresenter$OverflowMenuButton',
    ]

}