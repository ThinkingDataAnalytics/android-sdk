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

-optimizationpasses 5

#google推荐算法
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

-keep public class * extends android.content.ContentProvider

# 重命名抛出异常时的文件名称
-renamesourcefileattribute SourceFile

# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable

-keep class com.thinking.analyselibrary.ThinkingAnalyticsSDK {
    public <fields>;
    public <methods>;
    *** set*(***);
    *** get*();
}

-keep class com.thinking.analyselibrary.ThinkingAnalyticsSDK$AutoTrackEventType {
    public <fields>;
    public <methods>;
    *** set*(***);
    *** get*();
}

-keep class com.thinking.analyselibrary.ThinkingDataTrackViewOnClick{
                                                                                                                                                      *;
                                                                                                                                                  }
-keep class com.thinking.analyselibrary.ScreenAutoTracker{
                                                                                                                                           *;
                                                                                                                                       }
-keep class com.thinking.analyselibrary.ThinkingAdapterViewItemTrackProperties {
                                                                                  *;
                                                                              }

-keep class com.thinking.analyselibrary.ThinkingDataIgnoreTrackAppViewScreen{
                                                                                                                                                              *;
                                                                                                                                                          }
-keep class com.thinking.analyselibrary.ThinkingDataIgnoreTrackAppViewScreenAndAppClick{
                                                                                                                                                                         *;
                                                                                                                                                                     }
-keep class com.thinking.analyselibrary.ThinkingDataTrackEvent{
                                                                                                                                                *;
                                                                                                                                            }
-keep class com.thinking.analyselibrary.ThinkingDataTrackFragmentAppViewScreen{
                                                                                                                                                                *;
                                                                                                                                                            }
-keep class com.thinking.analyselibrary.ThinkingExpandableListViewItemTrackProperties{
                                                                                       *;
                                                                                   }

-keep class com.thinking.analyselibrary.ThinkingDataRuntimeBridge{
                                                                                       *;
                                                                                   }

#-keep class com.thinking.analyselibrary.** {
#*;
#}