package cn.thinkingdata.android.demo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.thinkingdata.android.TDFirstEvent;
import cn.thinkingdata.android.ThinkingAnalyticsSDK;
import cn.thinkingdata.android.ThinkingDataTrackEvent;
import cn.thinkingdata.android.demo.subprocess.TDSubprocessActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mButtonSend;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("hh","MainActivity onCreate");
        super.onCreate(savedInstanceState);
        TDTracker.initThinkingDataSDK(this.getApplicationContext());
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
//        toggleButton.setChecked(TDTracker.getInstance().isEnabled());
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startService(new Intent(MainActivity.this, TDSubService.class));
                Intent intent = new Intent(MainActivity.this, TDSubprocessActivity.class);
                intent.putExtra("pid", android.os.Process.myPid());
                intent.putExtra("enableAuto", true);
                startActivity(intent);
              /*  ToggleButton toggleButton = (ToggleButton) view;
                if (toggleButton.isChecked()) TDTracker.getInstance().enableTracking(true);
                else TDTracker.getInstance().enableTracking(false);*/
            }
        });

        ToggleButton toggleButton1 = findViewById(R.id.buttonOptOut);
//        toggleButton1.setChecked(TDTracker.getInstance().hasOptOut());
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
            TDTracker.getLightInstance().timeEvent("user_login");
            try {
                Thread.currentThread().sleep(1000);
            } catch (Exception e) {
                // ignore
            }

            // 设置账号ID
            TDTracker.getInstance().login(username);
            TDTracker.getLightInstance().login("child_" + username);

            // 设置用户属性
            try {
                JSONObject properties = new JSONObject();
                properties.put("UserName",username);
                TDTracker.getInstance().user_set(properties);
                TDTracker.getInstance().track(new TDFirstEvent("DEVICE_FIRST",properties));
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

            try {
                Thread.currentThread().sleep(1000);
            } catch (Exception e) {
                // ignore
            }

            // 记录登录事件
            try {
                JSONObject properties = new JSONObject();
                properties.put("user_name", username);
                TDTracker.getLightInstance().track("user_login", properties);
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
        Intent intent = new Intent(this, WebViewActivity.class);
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
        JSONObject ob = new JSONObject();
        JSONObject ob1 = new JSONObject();
        JSONArray arr = new JSONArray();

        try {
            //properties
            properties.put("KEY_STRING", "A string value");
            properties.put("KEY_DATE", new Date());
            properties.put("KEY_BOOLEAN", true);
            properties.put("KEY_DOUBLE", 56.17);
            properties.put("KEY_G", "哈哈MainActivity");
            //jsonObj
            ob1.put("KEY_STRING", "A string value");
            ob1.put("KEY_DATE", new Date());
            ob1.put("KEY_BOOLEAN", true);
            ob1.put("KEY_DOUBLE", 56.17);
            ob1.put("KEY_G", "哈哈");
            //jsonArray
            arr.put(new Date());
            arr.put("string");
            arr.put(1);
            arr.put(true);
            ob.put("date",new Date());
            arr.put(ob);
            properties.put("arr",arr);
            properties.put("ob1",ob1);

            Log.i("hh",properties+"");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        TDTracker.getInstance().track("test", properties);
    }

    /** Called when the user taps the Set Super Properties button */
    //设置公共事件属性
    public void setSuperProperties(View view) {
        try {
            JSONObject superProperties = new JSONObject();
            superProperties.put("SUPER_PROPERTY_INT",2);
            superProperties.put("SUPER_PROPERTY_CHANNEL","B1");
            TDTracker.getInstance().setSuperProperties(superProperties);
            superProperties.put("SUPER_PROPERTY_STRING","A1");
            TDTracker.getLightInstance().setSuperProperties(superProperties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    //设置自动采集事件自定义属性
    public void setAutoTrackEventProperties(View view) {
        try {
            List<ThinkingAnalyticsSDK.AutoTrackEventType> typeList = new ArrayList<>();
            typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
            typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
            typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
            JSONObject properties = new JSONObject();
            properties.put("key1",2);
            TDTracker.getInstance().setAutoTrackProperties(typeList,properties);
            properties.remove("AUTO_EVENT_PROP1");
            typeList.remove(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
            properties.put("AUTO_EVENT_PROP1","value1");
            properties.put("AUTO_EVENT_PROP2","value2");
            TDTracker.getInstance().setAutoTrackProperties(typeList, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    //模拟crash
    public void crashTest(View view){
        new Thread(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException(" this is a crash Test");
            }
        }).start();
    }

    // 清除公共事件属性 Channel
    public void unsetChannelProperties(View view) {
        TDTracker.getInstance().unsetSuperProperty("SUPER_PROPERTY_CHANNEL");
        TDTracker.getLightInstance().unsetSuperProperty("SUPER_PROPERTY_CHANNEL");
    }

    // 清空所有公共事件属性
    public void unsetAllSuperProperties(View view) {
        TDTracker.getInstance().clearSuperProperties();
        TDTracker.getLightInstance().clearSuperProperties();
    }

    public void flush(View view) {
        TDTracker.getInstance().flush();
    }

    public void clickTest(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, ClickTestActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.i("hh","MainActivity onPause");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("hh","MainActivity onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("hh","MainActivity onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("hh","MainActivity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("hh","MainActivity onDestroy");
    }
}
