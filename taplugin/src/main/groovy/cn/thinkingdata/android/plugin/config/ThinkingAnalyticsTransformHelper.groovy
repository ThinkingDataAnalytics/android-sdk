package cn.thinkingdata.android.plugin.config

import com.android.build.gradle.AppExtension

class ThinkingAnalyticsTransformHelper {

    ThinkingAnalyticsExtension extension
    AppExtension android

    boolean disableSensorsAnalyticsMultiThread
    boolean disableSensorsAnalyticsIncremental
    boolean isHookOnMethodEnter

    HashSet<String> exclude = new HashSet<>(['cn.thinkingdata.android',
                                             'android.support',
                                             'androidx',
                                             'com.qiyukf',
                                             'android.arch',
                                             'com.google.android',
                                             "com.tencent.smtt",
                                             "com.umeng.message",
                                             "com.xiaomi.push",
                                             "com.huawei.hms",
                                             "cn.jpush.android",
                                             "cn.jiguang",
                                             "com.meizu.cloud.pushsdk",
                                             "com.vivo.push",
                                             "com.igexin",
                                             "com.getui",
                                             "com.xiaomi.mipush.sdk",
                                             "com.heytap.msp.push",
                                             'com.bumptech.glide'])

    HashSet<String> include = new HashSet<>(['butterknife.internal.DebouncingOnClickListener',
                                             'com.jakewharton.rxbinding.view.ViewClickOnSubscribe',
                                             'com.facebook.react.uimanager.NativeViewHierarchyManager'])

    /** 将一些特例需要排除在外 */
    public static final HashSet<String> special = ['android.support.design.widget.TabLayout$ViewPagerOnTabSelectedListener',
                                                   'com.google.android.material.tabs.TabLayout$ViewPagerOnTabSelectedListener',
                                                   'android.support.v7.app.ActionBarDrawerToggle',
                                                   'androidx.appcompat.app.ActionBarDrawerToggle',
                                                   'androidx.fragment.app.FragmentActivity',
                                                   'androidx.core.app.NotificationManagerCompat',
                                                   'androidx.core.app.ComponentActivity',
                                                   'android.support.v4.app.NotificationManagerCompat',
                                                   'android.support.v4.app.SupportActivity',
                                                   'cn.jpush.android.service.PluginMeizuPlatformsReceiver',
                                                   'androidx.appcompat.widget.ActionMenuPresenter$OverflowMenuButton',
                                                   'android.widget.ActionMenuPresenter$OverflowMenuButton',
                                                   'android.support.v7.widget.ActionMenuPresenter$OverflowMenuButton']

    URLClassLoader urlClassLoader

    ThinkingAnalyticsTransformHelper(ThinkingAnalyticsExtension extension, AppExtension android) {
        this.extension = extension
        this.android = android
    }

    File androidJar() throws FileNotFoundException {
        File jar = new File(getSdkJarDir(), "android.jar")
        if (!jar.exists()) {
            throw new FileNotFoundException("Android jar not found!")
        }
        return jar
    }

    private String getSdkJarDir() {
        String compileSdkVersion = android.getCompileSdkVersion()
        return String.join(File.separator, android.getSdkDirectory().getAbsolutePath(), "platforms", compileSdkVersion)
    }

    void onTransform() {
        ArrayList<String> excludePackages = extension.exclude
        if (excludePackages != null) {
            exclude.addAll(excludePackages)
        }
        ArrayList<String> includePackages = extension.include
        if (includePackages != null) {
            include.addAll(includePackages)
        }
        createThinkingAnalyticsHookConfig()
    }

    private void createThinkingAnalyticsHookConfig() {
        //处理SDKConfig相关的 暂不支持
    }


    ClassNameAnalytics analytics(String className) {
        ClassNameAnalytics classNameAnalytics = new ClassNameAnalytics(className)
        if (!classNameAnalytics.isAndroidGenerated()) {
            for (pkgName in special) {
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
                if (!classNameAnalytics.isLeanback()) {
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


}