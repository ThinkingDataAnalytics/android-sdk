# aop
-keep class cn.thinkingdata.analytics.aop.** { *; }

-keep class cn.thinkingdata.analytics.encrypt.TDSecreteKey { *; }

-keep public interface cn.thinkingdata.analytics.ScreenAutoTracker { *; }
-keep public interface cn.thinkingdata.analytics.crash.CrashLogListener { *; }
-keep public class cn.thinkingdata.analytics.TDConfig { *; }
-keep public class cn.thinkingdata.core.utils.TASensitiveInfo { *; }
-keep public class cn.thinkingdata.analytics.TDConfig$TDMode { *; }
-keep public class cn.thinkingdata.analytics.TDConfig$ModeEnum { *; }
-keep public class cn.thinkingdata.analytics.TDConfig$NetworkType { *; }
-keep public class cn.thinkingdata.analytics.TDConfig$TDDNSService { *; }

-keep public class cn.thinkingdata.analytics.TDFirstEvent { *; }
-keep public class cn.thinkingdata.analytics.TDOverWritableEvent { *; }
-keep public class cn.thinkingdata.analytics.TDUpdatableEvent { *; }
-keep public class cn.thinkingdata.analytics.TDEventModel { *; }
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
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK$AutoTrackDynamicProperties{ *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK$TATrackStatus{ *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK$AutoTrackEventType{ *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsSDK$AutoTrackEventListener{ *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsPlugin { *; }
-dontwarn cn.thinkingdata.analytics.ThinkingAnalyticsPlugin.**
-dontwarn cn.thinkingdata.analytics.ThinkingAnalyticsPlugin
-dontwarn cn.thinkingdata.analytics.ThinkingAnalyticsProvider
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsProvider { *; }
-dontwarn cn.thinkingdata.analytics.ThinkingAnalyticsProvider.**
-keep class cn.thinkingdata.module.routes.** { *; }

-keep public class cn.thinkingdata.analytics.TDAnalytics { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDAutoTrackEventType { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDAutoTrackEventHandler { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDNetworkType { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDTrackStatus { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDDynamicSuperPropertiesHandler { *; }
-keep public class cn.thinkingdata.analytics.TDAnalytics$TDSendDataErrorCallback { *; }
-keep public class cn.thinkingdata.analytics.TDAnalyticsAPI { *; }

#Unity
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsProxy { *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsProxy$DynamicSuperPropertiesTrackerListener { *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalyticsProxy$AutoTrackEventTrackerListener { *; }

-keep public class cn.thinkingdata.analytics.ThinkingAnalytics { *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalytics$DynamicSuperPropertiesTrackerListener { *; }
-keep public class cn.thinkingdata.analytics.ThinkingAnalytics$AutoTrackEventTrackerListener { *; }
-keeppackagenames cn.thinkingdata.analytics.*
-keeppackagenames cn.thinkingdata.analytics.utils.*
