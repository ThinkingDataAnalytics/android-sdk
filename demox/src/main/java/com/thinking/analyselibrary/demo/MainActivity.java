package com.thinking.analyselibrary.demo;

import android.content.DialogInterface;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
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
        // 自定义控件属性
        try {
            JSONObject viewProperties = new JSONObject();
            viewProperties.put("viewProperty", "test");
            ThinkingAnalyticsSDK.sharedInstance(getApplicationContext()).setViewProperties(mButtonSend, viewProperties);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        if(TextUtils.isEmpty(username)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                }
            });
            AlertDialog dialog = builder.create();
            // 自定义控件ID
            ThinkingAnalyticsSDK.sharedInstance(getApplicationContext()).setViewID(dialog, "test_id");
            dialog.show();
        } else {
            // 对 login 事件记录时长
            ThinkingAnalyticsSDK.sharedInstance(getApplicationContext()).timeEvent("user_login");
            try {
                Thread.currentThread().sleep(1000);
            } catch (Exception e) {
                // ignore
            }

            // 设置账号ID
            ThinkingAnalyticsSDK.sharedInstance(getApplicationContext()).login(username);

            // 设置用户属性
            try {
                JSONObject properties = new JSONObject();
                properties.put("UserName",username);
                ThinkingAnalyticsSDK.sharedInstance(this).user_set(properties);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // 记录登录事件
            try {
                JSONObject properties = new JSONObject();
                properties.put("user_name", username);
                ThinkingAnalyticsSDK.sharedInstance(getApplicationContext()).track("user_login", properties);
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
