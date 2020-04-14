# ThinkingData Android SDK

ThinkingData Android SDK 为 Android 代码埋点提供了 API. 主要功能包括:
- 上报事件数据和用户属性数据
- 本地数据缓存
- 多实例上报
- 用户数据自动采集


本项目包括以下模块:
- thinkingsdk: 核心功能的实现
- aopplugin: 基于AOP 的自动埋点插件(可选)
- runtime: 自动埋点支持库(与 aopplugin 一同发布)
- demox: 使用 ThinkingData Android SDK 的 demo.


## 集成方法
Gradle 编译环境: 在 build.gradle 中添加以下依赖项：
```
dependencies {
    implementation 'cn.thinkingdata.android:ThinkingAnalyticsSDK:2.5.1'
}
```

如果需要使用自动采集功能(控件点击事件和 Fragment 浏览事件)，请添加自动采集插件(可选):
```
apply plugin: 'cn.thinkingdata.android'

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'cn.thinkingdata.android:android-gradle-plugin:2.0.1'
    }
}
```

## 上报数据

在上报之前，首先通过以下方法初始化 SDK
```java
ThinkingAnalyticsSDK instance = ThinkingAnalyticsSDK.sharedInstance(mContext, TA_APP_ID, TA_SERVER_URL);
```

参数`TA_APP_ID`是您的项目的APP\_ID，在您申请项目时会给出，请在此处填入

参数`TA_SERVER_URL`为数据上传的URL

如果您使用的是数数科技云服务，请输入以下URL:

http://receiver.ta.thinkingdata.cn

或https://receiver.ta.thinkingdata.cn

如果您使用的是私有化部署的版本，请输入以下URL:

http://<font color="red">数据采集地址</font>

后续可以通过如下两种方法使用 SDK
```java
instance.track("some_event");

ThinkingAnalyticsSDK.sharedInstance(this, TA_APP_ID).track("some_event");
```

如果您详细的使用指南，可以查看[Android SDK 使用指南](https://doc.thinkingdata.cn/tdamanual/installation/android_sdk_installation.html).


## 感谢
- [mixpanel-android](https://github.com/mixpanel/mixpanel-android)
- [gradle_plugin_android_aspectjx](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx)
