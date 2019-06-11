package com.thinking.analyselibrary.demo;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
            TDTracker.getInstance().setViewProperties(mButtonSend, viewProperties);
            TDTracker.getInstance().setViewID(mButtonSend, "myButtonSend");

            TDTracker.getDebugInstance().setViewID(mButtonSend, "debugButtonSend");
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
            TDTracker.getInstance().setViewID(dialog, "test_id");
            dialog.show();
        } else {
            // 对 login 事件记录时长
            TDTracker.getDebugInstance().timeEvent("user_login");
            TDTracker.getInstance().timeEvent("user_login");
            try {
                Thread.currentThread().sleep(1000);
            } catch (Exception e) {
                // ignore
            }

            // 设置账号ID
            TDTracker.getInstance().login(username);
            TDTracker.getDebugInstance().login(username + "-debug");

            // 设置用户属性
            try {
                JSONObject properties = new JSONObject();
                properties.put("UserName",username);
                TDTracker.getInstance().user_set(properties);
            } catch (JSONException e) {
                e.printStackTrace();

            }

            // 记录登录事件
            try {
                JSONObject properties = new JSONObject();
                properties.put("user_name", username);
                TDTracker.getInstance().track("user_login", properties);
                Thread.currentThread().sleep(200);
                properties.put("user_name",username + "-debug");
                TDTracker.getDebugInstance().track("user_login", properties);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
            }
        }
    }

    public void logout(View view) {
        TDTracker.getDebugInstance().logout();
        TDTracker.getInstance().logout();
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
            TDTracker.getInstance().setSuperProperties(superProperties);
            TDTracker.getInstance().track("set_super", superProperties);
            superProperties.put("vip_level",1);
            superProperties.put("Channel","B1");
            TDTracker.getDebugInstance().setSuperProperties(superProperties);
            TDTracker.getDebugInstance().track("set_super", superProperties);
            Log.d("Demo", TDTracker.getDebugInstance().getSuperProperties().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void unsetChannelProperties(View view) {
        // 清除公共事件属性 Channel
        TDTracker.getInstance().unsetSuperProperty("Channel");
        TDTracker.getDebugInstance().unsetSuperProperty("Channel");
    }

    public void unsetAllSuperProperties(View view) {
        // 清空所有公共事件属性
        TDTracker.getInstance().clearSuperProperties();
        TDTracker.getDebugInstance().clearSuperProperties();
    }

}
