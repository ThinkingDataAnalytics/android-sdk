package cn.thinkingdata.demo

import android.app.Application
import cn.thinkingdata.analytics.TDAnalytics
import cn.thinkingdata.analytics.TDConfig

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        TDAnalytics.enableLog(true)
        val config = TDConfig.getInstance(this,"40eddce753cd4bef9883a01e168c3df0","https://receiver-ta-preview.thinkingdata.cn1")
//        config.enableEncrypt(3,"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwahFAPD8HMZtvI6glXVVT6\\/e2zHLIvoYcCaB6QrXhPhE5tgaKfcID1NMToyZc28+v80e0w7u4i\\/bPA1ak1gj+a\\/sQ7fQ5wvMuwqhZIy1swCGarOZtgiLi7V0bVAwJjyYFC+gOFjzCCwIOlG4e2hN50t0UcVb7QKBKYgxBLRmnf5Aod3CJwmuk66vGHRzvuDmt1ZV2z3N3yIv9VXnbYnGZ981kPhQPQQX9bRK2Tj+No0uH+5Ki+m2q+TcgVcsmdXvLaTzL1UXpOx5VXu\\/USo5fykpiu8aaraI5+CDh2c3RvqIzVswjYtLaV+KKG2yxoDbtSt5crN3AS1cdNK+0dJJuQIDAQAB")
        config.backupUrlList = listOf("https://receiver-ta-preview.thinkingdata.cn2","https://receiver-ta-preview.thinkingdata.cn3","https://receiver-ta-preview.thinkingdata.cn")
        config.enableDNSService(listOf(TDConfig.TDDNSService.CLOUD_ALI))
        TDAnalytics.init(config)
        TDAnalytics.enableAutoTrack(TDAnalytics.TDAutoTrackEventType.APP_START or TDAnalytics.TDAutoTrackEventType.APP_END or TDAnalytics.TDAutoTrackEventType.APP_VIEW_SCREEN)
    }
}