// ITAToolServer.aidl
package cn.thinkingdata.android.aidl;

// Declare any non-default types here with import statements

interface ITAToolServer {
    void trackLog(String cmdOrder);

    void enableLog(boolean enable);

    void enableTransLog(boolean enable);

    void clearLog();

    String getAppAndSDKInfo();

    String getAppAndSDKInfoWithAppID(String appID);

    void mockTrack(String eventName, String props);

    String showSharedPreferences();

    void unbindThis();

    String checkAppID(String appIDs);
}