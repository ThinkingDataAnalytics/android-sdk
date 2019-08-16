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
import android.widget.ToggleButton;

import com.thinking.analyselibrary.ThinkingAnalyticsSDK;
import com.thinking.analyselibrary.ThinkingDataTrackEvent;
import com.thinking.analyselibrary.ThinkingDataTrackViewOnClick;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

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
        mButtonSend = (Button) findViewById(R.id.button_fragment);
        mButtonSend.setOnClickListener(this);
        // 自定义控件属性
        try {
            JSONObject viewProperties = new JSONObject();
            viewProperties.put("viewProperty", "test");
            TDTracker.getInstance().setViewProperties(mButtonSend, viewProperties);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ToggleButton toggleButton = findViewById(R.id.enableButton);
        toggleButton.setChecked(TDTracker.getInstance().isEnabled());
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ToggleButton toggleButton = (ToggleButton) view;
                if (toggleButton.isChecked()) TDTracker.getInstance().enableTracking(true);
                else TDTracker.getInstance().enableTracking(false);
            }
        });

        ToggleButton toggleButton1 = findViewById(R.id.buttonOptOut);
        toggleButton1.setChecked(TDTracker.getInstance().hasOptOut());
        toggleButton1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ToggleButton toggleButton = (ToggleButton) view;
                if (toggleButton.isChecked()) TDTracker.getInstance().optOutTrackingAndDeleteUser();
                else TDTracker.getInstance().optInTracking();

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_fragment:
                fragmentTest(v);
                break;
            default: break;
        }
    }


    /** Called when the user taps the Login button */
    public void login(View view) {
        EditText editText = (EditText) findViewById(R.id.editText_username);
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
            TDTracker.getInstance().timeEvent("user_login");
            try {
                Thread.currentThread().sleep(1000);
            } catch (Exception e) {
                // ignore
            }

            // 设置账号ID
            TDTracker.getInstance().login(username);

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
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // 清除 account id
    @ThinkingDataTrackEvent(eventName = "log_out", properties = "{\"paramString\":\"value\",\"paramNumber\":123,\"paramBoolean\":true}", appId = "debug-appid")
    public void logout(View view) {
        TDTracker.getInstance().logout();
    }

    public void webViewTest(View view) {
        Intent intent = new Intent(this, WebviewActivity.class);
        startActivity(intent);
    }

    public void fragmentTest(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayActivity.class);
        startActivity(intent);
    }

    // 记录一个事件
    public void trackTestEvent(View view) {
        JSONObject properties = new JSONObject();
        try {
            properties.put("KEY_STRING", "A string value");
            properties.put("KEY_DATE", new Date());
            properties.put("KEY_BOOLEAN", true);
            properties.put("KEY_DOUBLE", 56.17);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        TDTracker.getInstance().track("test_event", properties);
        TDTracker.getInstance().track("test_event", properties, NtpTime.getCalibratedDate());
        TDTracker.getInstance().timeEvent("test_event");
        TDTracker.getDebugInstance().track("test_event", properties);
    }

    /** Called when the user taps the Set Super Properties button */
    //设置公共事件属性
    public void setSuperProperties(View view) {
        try {
            JSONObject superProperties = new JSONObject();
            superProperties.put("vip_level",2);
            superProperties.put("Channel","A1");
            TDTracker.getInstance().setSuperProperties(superProperties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 清除公共事件属性 Channel
    public void unsetChannelProperties(View view) {
        TDTracker.getInstance().unsetSuperProperty("Channel");
    }

    // 清空所有公共事件属性
    public void unsetAllSuperProperties(View view) {
        TDTracker.getInstance().clearSuperProperties();
    }

    public void flush(View view) {
        TDTracker.getInstance().flush();
    }

    public void clickTest(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, ClickTestActivity.class);
        startActivity(intent);
    }
}
