package cn.thinkingdata.android;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import cn.thinkingdata.android.utils.TDConstants;

public class TestUtils {
    public static final String TAG = "TA_TEST.TestUtils";


    //测试报告地址  线上 http://10.1.0.168:5000/save  本地 http://192.168.20.52/save
    private static String testReportServer = "http://10.1.0.168:5000/save";


    /**
     * 推送测试结果到服务器
     */
    public static void postToServer(TestProperties testProperties) {
        new Thread(() -> {
            OutputStreamWriter out = null;
            BufferedReader in = null;
            StringBuilder result = new StringBuilder();
            HttpURLConnection conn = null;
            try {
                URL url = new URL(testReportServer);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                //发送POST请求必须设置为true
                conn.setDoOutput(true);
                conn.setDoInput(true);
                //设置连接超时时间和读取超时时间
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                //获取输出流
                out = new OutputStreamWriter(conn.getOutputStream());
                out.write(testProperties.toString());
                out.flush();
                out.close();
                //取得输入流，并使用Reader读取
                if (200 == conn.getResponseCode()) {
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    String line;
                    while ((line = in.readLine()) != null) {
                        result.append(line);
                    }
                } else {
                    Log.d(TAG, "ResponseCode is an error code:" + conn.getResponseCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }).start();
    }

    // 日期转换成时间戳
    public static boolean convertStamp(String time) {
        SimpleDateFormat format = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
        try {
            format.parse(time);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
