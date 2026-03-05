package cn.thinkingdata.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.thinkingdata.analytics.TDAnalytics
import cn.thinkingdata.analytics.TDConfig
import cn.thinkingdata.demo.ui.theme.DemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ButtonsLayout(
                        modifier = Modifier.padding(
                            innerPadding
                        )
                    )
                }
            }
        }
        TDAnalytics.enableLog(true)
        val config = TDConfig.getInstance(this,"40eddce753cd4bef9883a01e168c3df0","https://receiver-ta-preview.thinkingdata.cn")
//        config.enableEncrypt(3,"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwahFAPD8HMZtvI6glXVVT6\\/e2zHLIvoYcCaB6QrXhPhE5tgaKfcID1NMToyZc28+v80e0w7u4i\\/bPA1ak1gj+a\\/sQ7fQ5wvMuwqhZIy1swCGarOZtgiLi7V0bVAwJjyYFC+gOFjzCCwIOlG4e2hN50t0UcVb7QKBKYgxBLRmnf5Aod3CJwmuk66vGHRzvuDmt1ZV2z3N3yIv9VXnbYnGZ981kPhQPQQX9bRK2Tj+No0uH+5Ki+m2q+TcgVcsmdXvLaTzL1UXpOx5VXu\\/USo5fykpiu8aaraI5+CDh2c3RvqIzVswjYtLaV+KKG2yxoDbtSt5crN3AS1cdNK+0dJJuQIDAQAB")
        TDAnalytics.init(config)
//        TDAnalytics.enableAutoTrack(TDAnalytics.TDAutoTrackEventType.APP_START or TDAnalytics.TDAutoTrackEventType.APP_END or TDAnalytics.TDAutoTrackEventType.APP_VIEW_SCREEN)
        TDAnalytics.enableAutoTrack(TDAnalytics.TDAutoTrackEventType.APP_END)
//        TDAnalytics.login("3sss")
//        TDAnalytics.track("test3")
//        TDAnalytics.track("test4", JSONObject().apply {
//            put("name", "\"test")
//        })
//        TDAnalytics.flush()
    }
}

@Composable
fun ButtonsLayout(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // 使用Column布局垂直排列两个按钮
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 第一个按钮
        Button(
            onClick = {
                context.startActivity(Intent(context, SecondActivity::class.java))
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "按钮 1",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        // 第二个按钮
        Button(
            onClick = {
                //强制触发GC 测试Activity是否能被回收
                // 1. 提示系统执行GC（基础操作）
                System.gc()

                // 2. 触发finalize（可选，加速对象回收）
                System.runFinalization()

                // 3. Android 专属：触发低内存通知，促使系统回收
                Runtime.getRuntime().gc()

                // 4. 模拟内存压力（关键：让系统"有动力"执行GC）
                // 申请一大块临时内存，用完立即释放，制造内存波动
                try {
                    var temp: ByteArray? = ByteArray(1024 * 1024 * 50) // 50MB 临时数组
                    temp = null // 立即释放
                } catch (e: OutOfMemoryError) {
                    // 内存不足时直接捕获，不影响程序
                    e.printStackTrace()
                }
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "触发GC",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        Button(
            onClick = {
//                TDAnalytics.track("AA")
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "按钮 3",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DemoTheme {
        ButtonsLayout()
    }
}