package cn.thinkingdata.android.demo.subprocess;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.thinkingdata.android.TDConfig;
import cn.thinkingdata.android.ThinkingAnalyticsSDK;
import cn.thinkingdata.android.demo.subprocess.model.TDAction;
import cn.thinkingdata.android.demo.subprocess.model.TDActionModel;

public class TDSubprocessActivity extends TDListActivity {

    public static final String TA_APP_ID = "1b1c1fef65e3   482bad5c9d0e6a82335  6 ";
    public static final String TA_SERVER_URL = "https://receiver.ta.thinkingdata.cn/";
    static Activity mActivity;

    public static ThinkingAnalyticsSDK initInstance(boolean enableAutoTrack) {
        ThinkingAnalyticsSDK mInstance = null;
        TDConfig config = TDConfig.getInstance(mActivity.getApplicationContext(), TA_APP_ID, TA_SERVER_URL);
        mInstance = ThinkingAnalyticsSDK.sharedInstance(config);
        mInstance.track("subprocessTestEvent");
        if (enableAutoTrack) {
            try {
                List<ThinkingAnalyticsSDK.AutoTrackEventType> typeList = new ArrayList<>();
                typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
                typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
                typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
                typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
                JSONObject properties = new JSONObject();
                properties.put("SUB_AUTO_EVENT_PROP1", 2);
                mInstance.setAutoTrackProperties(typeList, properties);
                mInstance.enableAutoTrack(typeList);
                properties.remove("SUB_AUTO_EVENT_PROP1");
                typeList.remove(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
                properties.put("SUB_AUTO_EVENT_PROP1", new Date());
                properties.put("SUB_AUTO_EVENT_PROP2", "value2");
                mInstance.setAutoTrackProperties(typeList, properties);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return mInstance;
    }

    @Override
    public void setData() {
        TDActionModel userIDModel = new TDActionModel("设置用户ID", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this, TDSubUserIDActivity.class));
            }
        });
        TDActionModel userPropertyModel = new TDActionModel("用户属性设置", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this, TDSubUserPropertyActivity.class));
            }
        });

        TDActionModel eventModel = new TDActionModel("事件发送", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this, TDSubEventActivity.class));
            }
        });
        TDActionModel otherModel = new TDActionModel("其他", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this, TDSubOtherActivity.class));
            }
        });

        models.add(userIDModel);
        models.add(userPropertyModel);
        models.add(eventModel);
        models.add(otherModel);


    }

    @Override
    public void setContentView() {
        super.setContentView();
        mNavigation.setTitle("子进程测试");
        mNavigation.setTitleVisible(View.VISIBLE);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                throw new RuntimeException(" this is a crash Test");
//            }
//        }).start();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        android.os.Process.killProcess(getIntent().getIntExtra("pid", 13330));
//        TDTracker.getInstance().track("111111");

        mActivity = this;
        initInstance(getIntent().getBooleanExtra("enableAuto", false));
    }
}
