package cn.thinkingdata.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.thinkingdata.analytics.TDAnalytics
import cn.thinkingdata.analytics.TDConfig
import cn.thinkingdata.demo.ui.theme.DemoTheme
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        TDAnalytics.enableLog(true)
        val config = TDConfig.getInstance(this,"40eddce753cd4bef9883a01e168c3df0","https://receiver-ta-preview.thinkingdata.cn")
//        config.enableEncrypt(3,"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwahFAPD8HMZtvI6glXVVT6\\/e2zHLIvoYcCaB6QrXhPhE5tgaKfcID1NMToyZc28+v80e0w7u4i\\/bPA1ak1gj+a\\/sQ7fQ5wvMuwqhZIy1swCGarOZtgiLi7V0bVAwJjyYFC+gOFjzCCwIOlG4e2hN50t0UcVb7QKBKYgxBLRmnf5Aod3CJwmuk66vGHRzvuDmt1ZV2z3N3yIv9VXnbYnGZ981kPhQPQQX9bRK2Tj+No0uH+5Ki+m2q+TcgVcsmdXvLaTzL1UXpOx5VXu\\/USo5fykpiu8aaraI5+CDh2c3RvqIzVswjYtLaV+KKG2yxoDbtSt5crN3AS1cdNK+0dJJuQIDAQAB")
        TDAnalytics.init(config)
        TDAnalytics.login("3sss")
        TDAnalytics.track("test3")
        TDAnalytics.track("test4", JSONObject().apply {
            put("name", "\"test")
        })
        TDAnalytics.flush()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DemoTheme {
        Greeting("Android")
    }
}