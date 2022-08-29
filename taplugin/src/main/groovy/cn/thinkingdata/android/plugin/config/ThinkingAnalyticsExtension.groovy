package cn.thinkingdata.android.plugin.config
import org.gradle.internal.reflect.Instantiator
/**
 * 配置信息
 */
class ThinkingAnalyticsExtension {

    //是否是debug模式
    public boolean debug = false
    //是否允许扫描jar
    public boolean disableJar = false
    //是否扫描指定的类
    public boolean useInclude = false
    //是否支持lambda表达式
    public boolean lambdaEnabled = true
    public boolean lambdaParamOptimize = false

    public boolean disableTrackPush = true

    public ArrayList<String> exclude = []
    public ArrayList<String> include = []
    public ArrayList<String> excludeSensitive = []


    ThinkingAnalyticsExtension(Instantiator ins) {}

}