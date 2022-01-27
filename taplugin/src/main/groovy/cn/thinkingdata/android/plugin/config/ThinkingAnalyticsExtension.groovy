package cn.thinkingdata.android.plugin.config

import org.gradle.api.Action
import org.gradle.internal.reflect.Instantiator

/**
 * 配置信息
 */
class ThinkingAnalyticsExtension {

    public boolean debug = false
    public boolean disableJar = false
    public boolean useInclude = false
    public boolean lambdaEnabled = true
    public boolean lambdaParamOptimize = false

    public ArrayList<String> exclude = []
    public ArrayList<String> include = []

    //public ThinkingAnalyticsSDKExtension sdk

    ThinkingAnalyticsExtension(Instantiator ins) {
        //sdk = ins.newInstance(ThinkingAnalyticsSDKExtension)
    }

//    void sdk(Action<? super ThinkingAnalyticsSDKExtension> action) {
//        action.execute(sdk)
//    }

}