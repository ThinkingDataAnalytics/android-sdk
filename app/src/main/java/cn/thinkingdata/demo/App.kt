package cn.thinkingdata.demo

import android.app.Application
import android.util.Log
import cn.thinkingdata.analytics.TDAnalytics
import cn.thinkingdata.analytics.TDConfig

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        TDAnalytics.enableLog(true)
//        val config = TDConfig.getInstance(this,"40eddce753cd4bef9883a01e168c3df0","https://receiver-ta-preview.thinkingdata.cn")
        val config = TDConfig.getInstance(this,"8de3005299604ea1ba5541e564722d73","http://10.82.2.137:8991")
        config.enableEncrypt(0,"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqOvk7QsPiVQ+ygNXdwBBInPs6+Cpgp7R4etlldsS0IlzUvWE9dEEWBCLMfuGmcPJf5b4vI9OaLjlUeiYt2tF+rLpwc1s/XOnIqzv4qBd5yKcfnh/y9i26r9+HYUfxkoVawncIEXm9Ge4WAswtXVAsHBEdJgy3LqCoBriLTJ1MWU7YlXQGyLAXzF/fMMWiU5rh2LRmc0q3sST5JLDtudOzg5uzwrqdlAsrvkZJzLwOiRAhtbETS5tZcNRjNS32POhmPcrtD+QQ3caFpLpDELl7y4e7qICjb8pKZqZoo1gA2xHgBC0okHOUT4eLrcLAYqJ/PCgByHCR5wkD6kGcdx0hQIDAQAB")
//        config.backupUrlList = listOf("https://receiver-ta-preview.thinkingdata.cn2","https://receiver-ta-preview.thinkingdata.cn3","https://receiver-ta-preview.thinkingdata.cn")
//        config.enableDNSService(listOf(TDConfig.TDDNSService.CLOUD_ALI))
        TDAnalytics.init(config)
        TDAnalytics.enableAutoTrack(TDAnalytics.TDAutoTrackEventType.APP_START or TDAnalytics.TDAutoTrackEventType.APP_END or TDAnalytics.TDAutoTrackEventType.APP_VIEW_SCREEN)
        TDAnalytics.registerErrorCallback { code, errorMsg, ext ->
            Log.i("========","${code}====${errorMsg}")
        }
    }
}