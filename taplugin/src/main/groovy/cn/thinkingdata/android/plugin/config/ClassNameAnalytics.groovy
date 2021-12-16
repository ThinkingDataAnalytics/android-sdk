package cn.thinkingdata.android.plugin.config
class ClassNameAnalytics {
    public String className
    boolean isShouldModify = false

    ClassNameAnalytics(String className) {
        this.className = className
    }

    boolean isLeanback() {
        return className.startsWith("android.support.v17.leanback") || className.startsWith("androidx.leanback")
    }

    boolean isAndroidGenerated() {
        return className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')
    }

}