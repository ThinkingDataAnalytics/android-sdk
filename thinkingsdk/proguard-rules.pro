# aop
-keep class cn.thinkingdata.analytics.aop.** { *; }

-keep class cn.thinkingdata.analytics.encrypt.TDSecreteKey { *; }

-keep public interface cn.thinkingdata.analytics.ScreenAutoTracker { *; }
-keep public interface cn.thinkingdata.analytics.crash.CrashLogListener { *; }
-keep public class cn.thinkingdata.analytics.TDConfig { *; }
-keep public class cn.thinkingdata.analytics.utils.TASensitiveInfo { *; }
-keep public class cn.thinkingdata.analytics.TDConfig$TDMode { *; }
-keep public class cn.thinkingdata.analytics.TDConfig$ModeEnum { *; }
-keep public class cn.thinkingdata.analytics.TDConfig$NetworkType { *; }

-keep public class cn.thinkingdata.analytics.TDFirstEvent { *; }
-keep public class cn.thinkingdata.analytics.TDOverWritableEvent { *; }
-keep public class cn.thinkingdata.analytics.TDUpdatableEvent { *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsEvent { *; }
-keep public class cn.thinkingdata.analytics.BuildConfig { *; }
-keep class cn.thinkingdata.analytics.model.** { *; }

-keep public class cn.thinkingdata.analytics.TDWebAppInterface { *; }

-keep public class cn.thinkingdata.analytics.ThinkingAdapterViewItemTrackProperties { *; }
-keep public class cn.thinkingdata.analytics.ThinkingDataAutoTrackAppViewScreenUrl { *; }
-keep public class cn.thinkingdata.analytics.ThinkingDataFragmentTitle { *; }
-keep public class cn.thinkingdata.analytics.ThinkingDataIgnoreTrackAppClick { *; }
-keep public class cn.thinkingdata.analytics.ThinkingDataIgnoreTrackAppViewScreen { *; }
-keep public class cn.thinkingdata.analytics.ThinkingDataIgnoreTrackAppViewScreenAndAppClick { *; }
-keep public class cn.thinkingdata.analytics.ThinkingDataIgnoreTrackOnClick { *; }
-keep public class cn.thinkingdata.analytics.ThinkingDataTrackEvent { *; }
-keep public class cn.thinkingdata.analytics.ThinkingDataTrackViewOnClick { *; }
-keep public class cn.thinkingdata.analytics.ThinkingExpandableListViewItemTrackProperties { *; }

-keep public class cn.thinkingdata.analytics.ThinkingDataRuntimeBridge { *; }

-keep public class cn.thinkingdata.analytics.TDPresetProperties { *; }

-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK { *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK$ThinkingdataNetworkType { *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK$DynamicSuperPropertiesTracker{ *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK$TATrackStatus{ *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK$AutoTrackEventType{ *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK$AutoTrackEventListener{ *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsPlugin { *; }
-keep class cn.thinkingdata.module.routes.** { *; }

-keep public class cn.thinkingdata.analytics.TDAnalytics { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDAutoTrackEventType { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDAutoTrackEventHandler { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDNetworkType { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDTrackStatus { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDDynamicSuperPropertiesHandler { *; }
-keep public class cn.thinkingdata.analytics.TDAnalyticsAPI { *; }

-keep class cn.thinkingdata.analytics.R$* {
    <fields>;
}
-keep public class * extends android.content.ContentProvider
-keepnames class * extends android.view.View

-dontwarn org.json.**
-keep class org.json.**{*;}


# AlertDialog
-keep class android.app.AlertDialog {*;}
-keep class android.support.v7.app.AlertDialog {*;}
-keep class androidx.appcompat.app.AlertDialog {*;}
-keep class * extends android.support.v7.app.AlertDialog {*;}
-keep class * extends androidx.appcompat.app.AlertDialog {*;}
-keep class * extends android.app.AlertDialog {*;}

# Fragment
-keep class android.app.Fragment {*;}
-keep class android.support.v4.app.Fragment {*;}
-keep class androidx.fragment.app.Fragment {*;}
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public void setUserVisibleHint(boolean);
    public void onViewCreated(android.view.View, android.os.Bundle);
    public void onHiddenChanged(boolean);
    public void onResume();
}
-keepclassmembers class * extends android.app.Fragment {
    public void setUserVisibleHint(boolean);
    public void onViewCreated(android.view.View, android.os.Bundle);
    public void onHiddenChanged(boolean);
    public void onResume();
}
-keepclassmembers class * extends android.support.v4.app.Fragment {
    public void setUserVisibleHint(boolean);
    public void onViewCreated(android.view.View, android.os.Bundle);
    public void onHiddenChanged(boolean);
    public void onResume();
}


# TabLayout
-keep class android.support.design.widget.TabLayout$Tab {*;}
-keep class com.google.android.material.tabs.TabLayout$Tab {*;}
-keep class * extends android.support.design.widget.TabLayout$Tab {*;}
-keep class * extends com.google.android.material.tabs.TabLayout$Tab {*;}

# ViewPager
-keep class android.support.v4.view.ViewPager {*;}
-keep class android.support.v4.view.PagerAdapter {*;}
-keep class androidx.viewpager.widget.ViewPager {*;}
-keep class androidx.viewpager.widget.PagerAdapter {*;}
-keep class * extends android.support.v4.view.ViewPager {*;}
-keep class * extends android.support.v4.view.PagerAdapter {*;}
-keep class * extends androidx.viewpager.widget.ViewPager {*;}
-keep class * extends androidx.viewpager.widget.PagerAdapter {*;}

# SwitchCompat
-keep class android.support.v7.widget.SwitchCompat {*;}
-keep class androidx.appcompat.widget.SwitchCompat {*;}
-keep class * extends android.support.v7.widget.SwitchCompat {*;}
-keep class * extends androidx.appcompat.widget.SwitchCompat {*;}

# ContextCompat
-keep class android.support.v4.content.ContextCompat {*;}
-keep class androidx.core.content.ContextCompat {*;}
-keep class * extends android.support.v4.content.ContextCompat {*;}
-keep class * extends androidx.core.content.ContextCompat {*;}

# AppCompatActivity
-keep class android.support.v7.app.AppCompatActivity {
    public android.support.v7.app.ActionBar getSupportActionBar();
}
-keep class androidx.appcompat.app.AppCompatActivity {
    public androidx.appcompat.app.ActionBar getSupportActionBar();
}
-keep class * extends android.support.v7.app.AppCompatActivity {
    public android.support.v7.app.ActionBar getSupportActionBar();
}
-keep class * extends androidx.appcompat.app.AppCompatActivity {
    public androidx.appcompat.app.ActionBar getSupportActionBar();
}

#ActionBar
-keep class android.support.v7.app.ActionBar {*;}
-keep class androidx.appcompat.app.ActionBar {*;}
-keep class * extends android.support.v7.app.ActionBar {*;}
-keep class * extends androidx.appcompat.app.ActionBar {*;}