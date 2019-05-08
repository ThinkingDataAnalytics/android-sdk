package com.thinking.analyselibrary.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.thinking.analyselibrary.ThinkingAnalyticsSDK;
import com.thinking.analyselibrary.ThinkingDataTrackViewOnClick;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mButtonSend;

    public static final String EXTRA_MESSAGE = "com.thinking.analyselibrary.demo.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        
        initView();
    }

    private void initView() {
        mButtonSend = (Button) findViewById(R.id.button9);
        mButtonSend.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button9:
                send(v);
                break;
            default: break;
        }
    }


    /** Called when the user taps the Login button */
    @ThinkingDataTrackViewOnClick
    public void login(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        String username = editText.getText().toString();

        // 设置账号ID
        ThinkingAnalyticsSDK.sharedInstance(getApplicationContext()).login(username);

        // 记录登录事件
        try {
            JSONObject properties = new JSONObject();
            properties.put("user_name", username);
            ThinkingAnalyticsSDK.sharedInstance(getApplicationContext()).track("user_login", properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void logout(View view) {
        ThinkingAnalyticsSDK.sharedInstance(getApplicationContext()).logout();
    }

    public void webViewTest(View view) {
        Intent intent = new Intent(this, WebviewActivity.class);
        startActivity(intent);
    }

    public void send(View view) {
        EditText editText = (EditText) findViewById(R.id.editText4);
        String message = editText.getText().toString();

        // Do something in response to button
        Intent intent = new Intent(this, DisplayActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);

    }

    /** Called when the user taps the Set Super Properties button */
    public void setSuperProperties(View view) {
        //设置公共事件属性
        try {
            JSONObject superProperties = new JSONObject();
            superProperties.put("vip_level",2);
            superProperties.put("Channel","A1");
            ThinkingAnalyticsSDK.sharedInstance(this).setSuperProperties(superProperties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void unsetChannelProperties(View view) {
        // 清除公共事件属性 Channel
        ThinkingAnalyticsSDK.sharedInstance(this).unsetSuperProperty("Channel");
    }

    public void unsetAllSuperProperties(View view) {
        // 清空所有公共事件属性
        ThinkingAnalyticsSDK.sharedInstance(this).clearSuperProperties();
    }

}
