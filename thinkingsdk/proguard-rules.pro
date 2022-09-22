      # Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/sunyujuan/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#-dontwarn cn.thinkingdata.android.**
#-keep class cn.thinkingdata.android.** {
#*;
#}

# aop文件夹下辅助类不能混淆
-keep class cn.thinkingdata.android.aop.** { *; }

-keep class cn.thinkingdata.android.encrypt.TDSecreteKey { *; }

-keep public interface cn.thinkingdata.android.ScreenAutoTracker { *; }
-keep public interface cn.thinkingdata.android.crash.CrashLogListener { *; }
-keep public class cn.thinkingdata.android.TDConfig { *; }
-keep public class cn.thinkingdata.android.utils.TASensitiveInfo { *; }
-keep public class cn.thinkingdata.android.TDConfig$ModeEnum { *; }
-keep public class cn.thinkingdata.android.TDConfig$NetworkType { *; }

-keep public class cn.thinkingdata.android.TDFirstEvent { *; }
-keep public class cn.thinkingdata.android.TDOverWritableEvent { *; }
-keep public class cn.thinkingdata.android.TDUpdatableEvent { *; }

-keep public class cn.thinkingdata.android.TDWebAppInterface { *; }

-keep public class cn.thinkingdata.android.ThinkingAdapterViewItemTrackProperties { *; }
-keep public class cn.thinkingdata.android.ThinkingDataAutoTrackAppViewScreenUrl { *; }
-keep public class cn.thinkingdata.android.ThinkingDataFragmentTitle { *; }
-keep public class cn.thinkingdata.android.ThinkingDataIgnoreTrackAppClick { *; }
-keep public class cn.thinkingdata.android.ThinkingDataIgnoreTrackAppViewScreen { *; }
-keep public class cn.thinkingdata.android.ThinkingDataIgnoreTrackAppViewScreenAndAppClick { *; }
-keep public class cn.thinkingdata.android.ThinkingDataIgnoreTrackOnClick { *; }
-keep public class cn.thinkingdata.android.ThinkingDataTrackEvent { *; }
-keep public class cn.thinkingdata.android.ThinkingDataTrackViewOnClick { *; }
-keep public class cn.thinkingdata.android.ThinkingExpandableListViewItemTrackProperties { *; }

-keep public class cn.thinkingdata.android.ThinkingDataRuntimeBridge { *; }

-keep public class cn.thinkingdata.android.TDPresetProperties { *; }

-keep public class cn.thinkingdata.android.ThinkingAnalyticsSDK { *; }
-keep public class cn.thinkingdata.android.ThinkingAnalyticsSDK$AutoTrackEventType { *; }
-keep public class cn.thinkingdata.android.ThinkingAnalyticsSDK$ThinkingdataNetworkType { *; }
-keep public class cn.thinkingdata.android.ThinkingAnalyticsSDK$AutoTrackEventListener{ *; }
-keep public class cn.thinkingdata.android.ThinkingAnalyticsSDK$AutoTrackEventTrackerListener{ *; }
-keep public class cn.thinkingdata.android.ThinkingAnalyticsSDK$DynamicSuperPropertiesTracker{ *; }
-keep public class cn.thinkingdata.android.ThinkingAnalyticsSDK$DynamicSuperPropertiesTrackerListener{ *; }
-keep public class cn.thinkingdata.android.ThinkingAnalyticsSDK$TATrackStatus{ *; }

-keep public class cn.thinkingdata.android.utils.TDLog{ *; }


-keep class **.R$* {
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