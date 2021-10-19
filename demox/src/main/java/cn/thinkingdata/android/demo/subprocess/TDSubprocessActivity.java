package cn.thinkingdata.android.demo.subprocess;

import android.content.Intent;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.thinkingdata.android.TDConfig;
import cn.thinkingdata.android.ThinkingAnalyticsSDK;
import cn.thinkingdata.android.demo.TDTracker;
import cn.thinkingdata.android.demo.subprocess.model.TDAction;
import cn.thinkingdata.android.demo.subprocess.model.TDActionModel;

public class TDSubprocessActivity extends TDListActivity {
    @Override
    public void setData() {
        TDActionModel userIDModel = new TDActionModel("设置用户ID", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this,TDSubUserIDActivity.class));
            }
        });
        TDActionModel userPropertyModel = new TDActionModel("用户属性设置", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this,TDSubUserPropertyActivity.class));
            }
        });

        TDActionModel eventModel = new TDActionModel("事件发送", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this,TDSubEventActivity.class));
            }
        });
        TDActionModel otherModel = new TDActionModel("其他", new TDAction() {
            @Override
            public void doAction() {
                startActivity(new Intent(TDSubprocessActivity.this,TDSubOtherActivity.class));
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
        try {
            TDConfig config = TDConfig.getInstance(this.getApplicationContext(), "1b1c1fef65e3   482bad5c9d0e6a82335  6 ", "https://receiver.ta.thinkingdata.cn/");
            ThinkingAnalyticsSDK mInstance = ThinkingAnalyticsSDK.sharedInstance(config);
            List<ThinkingAnalyticsSDK.AutoTrackEventType> typeList = new ArrayList<>();
            typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
            typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
            typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
            typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
            JSONObject properties = new JSONObject();
            properties.put("SUB_AUTO_EVENT_PROP1",2);
            mInstance.setAutoTrackEventProperties(typeList,properties);
            mInstance.enableAutoTrack(typeList);
            properties.remove("SUB_AUTO_EVENT_PROP1");
            typeList.remove(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
            properties.put("SUB_AUTO_EVENT_PROP1","value1");
            properties.put("SUB_AUTO_EVENT_PROP2","value2");
            mInstance.setAutoTrackEventProperties(typeList, properties); } catch (JSONException e) {
            e.printStackTrace();
        }
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                throw new RuntimeException(" this is a crash Test");
//            }
//        }).start();

    }
}
