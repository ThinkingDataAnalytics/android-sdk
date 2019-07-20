## ThinkingData Android SDK

The project including:
- demox: a demo for using thinkingdata sdk
- thinkingsdk: the main libraray for core functions
- aopplugin: a gradle plugin for auto track using aop
- runtime: support library for the plugin containing aspect files.

The main features:
- track events and user datas
- cache the data in sqlite database before posting to server
- support multi-instances with different appContext and appId
- support click events auto track

The aop plugin config:

```
thinkingAnalytics {
    exclude 'com.google.android.gms'
    include 'com.android'
}
```
