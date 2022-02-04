package cn.thinkingdata.android.plugin.config
class ThinkingClassNameAnalytics {

    public String className
    boolean isShouldModify = false

    ThinkingClassNameAnalytics(String className) {
        this.className = className
    }

    boolean isLeanbackClass() {
        return className.startsWith("android.support.v17.leanback") || className.startsWith("androidx.leanback")
    }

    /**
     * 是否是配置相关的class
     * @return
     */
    boolean isAndroidConfigClass() {
        return className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')
    }

}