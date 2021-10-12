package cn.thinkingdata.android.demo;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import cn.thinkingdata.android.TDConfig;
import cn.thinkingdata.android.ThinkingAnalyticsSDK;

public class TDTracker {
    /**
     * 项目APP_ID，在申请项目时会给出
     */
//   private static final String TA_APP_ID = "22e445595b0f42bd8c5fe35bc44b88d6";
   private static final  String TA_APP_ID = "1b1c1fef65e3   482bad5c9d0e6a82335  6 ";
   private static final String TA_SERVER_URL = "https://receiver.ta.thinkingdata.cn/";
//    private static final String TA_APP_ID_DEBUG = "4326b258b3914aeb826bb5865fc729ff";
//    private static final String TA_APP_ID = "22e445595b0f42bd8c5fe35bc44b88d6";

    /**
     * 数据上传地址
     * 如果您使用的是云服务，请输入以下URL:
     * http://receiver.ta.thinkingdata.cn:9080
     * 如果您使用的是私有化部署的版本，请输入以下URL:
     * http://数据采集地址:9080
     */
//   private static final String TA_SERVER_URL = "https://receiver.ta.thinkingdata.cn";
//   private static final String TA_SERVER_URL = "https://receiver-ta-dev.thinkingdata.cn";

    private static ThinkingAnalyticsSDK mInstance1;
    private static ThinkingAnalyticsSDK mInstance2;
    private static ThinkingAnalyticsSDK mDebugInstance;
    private static ThinkingAnalyticsSDK mLightInstance;
    private static ThinkingAnalyticsSDK mLightInstance2;

    public static ThinkingAnalyticsSDK getInstance() {
        return mInstance1;
    }

    public static ThinkingAnalyticsSDK getLightInstance() {
        return mLightInstance;
    }

    public static ThinkingAnalyticsSDK getDebugInstance() {
        return mDebugInstance;
    }

    /**
     * 仅在自动测试中使用，Demo App 自身不会调用此函数
     * @param instance
     * @param debugInstance
     */
    public static void initThinkingDataSDK(ThinkingAnalyticsSDK instance, ThinkingAnalyticsSDK debugInstance) {
        mInstance1 = instance;
        mDebugInstance = debugInstance;
        setUp();
    }

    /** 初始化 TA SDK */
    static void initThinkingDataSDK(Context context) {
        Context mContext = context.getApplicationContext();
        TDConfig config1 = TDConfig.getInstance(mContext,TA_APP_ID,TA_SERVER_URL);
        config1.setName(context,TA_APP_ID,"instance1");
        TDConfig config2 = TDConfig.getInstance(mContext,TA_APP_ID,TA_SERVER_URL);
        config2.setName(context,TA_APP_ID,"instance2");

//        config.setMode(TDConfig.ModeEnum.DEBUG);
        config1.setMutiprocess(true);
        config2.setMutiprocess(true);
        mInstance1 = ThinkingAnalyticsSDK.sharedInstance(context,"instance1",TA_SERVER_URL);
        mInstance2 = ThinkingAnalyticsSDK.sharedInstance(context,"instance2",TA_SERVER_URL);
        Log.d("ThinkingAnalytics","mInstance1 name "+ mInstance1.getName());
        Log.d("ThinkingAnalytics","mInstance2 name "+ mInstance2.getName());
        setUp();
        enableAutoTrack();


    }
    public  static  void enableAutoTrack()
    {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> typeList = new ArrayList<>();
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        mInstance1.enableAutoTrack(typeList);
        mInstance2.enableAutoTrack(typeList);
    }

    private static void setUp() {

        //Log.d("ThinkingDataDemo","get distinct id: " + ThinkingAnalyticsSDK.sharedInstance(this).getDistinctId());
        // set distinct id
        mInstance1.identify("instance_id1");
        mInstance2.identify("instance_id2");
        mInstance1.track("justTest1");
        mInstance2.track("justTest2");
        ThinkingAnalyticsSDK.enableTrackLog(true);
        mLightInstance = mInstance1.createLightInstance();
        mLightInstance2 = mInstance2.createLightInstance();

    }
}
