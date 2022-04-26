package cn.thinkingdata.android.demo;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.thinkingdata.android.TDConfig;
import cn.thinkingdata.android.TDPresetProperties;
import cn.thinkingdata.android.ThinkingAnalyticsSDK;
import cn.thinkingdata.android.encrypt.TDSecreteKey;

public class TDTracker {
    /**
     * 项目APP_ID，在申请项目时会给出
     */
///   private static final String TA_APP_ID = "22e445595b0f42bd8c5fe35bc44b88d6";
    private static final  String TA_APP_ID = "1b1c1fef65e3482bad5c9d0e6a823356";
    private static final String TA_SERVER_URL = "https://receiver.ta.thinkingdata.cn/";

    //    private static final String TA_APP_ID_DEBUG = "4326b258b3914aeb826bb5865fc729ff";
//    private static final String TA_APP_ID = "22e445595b0f42bd8c5fe35bc44b88d6";
    private static final String TA_APP_ID_tmp = "d265efeedb2d469ca275fc3bfe569631";
    private static final String TA_APP_ID_ = "1b1c1f  ef65e3482bad5c9d0e6  a823356";
    private static final String TA_APP_ID_DEBUG = "debug-appid";
    /**
     * 数据上传地址
     * 如果您使用的是云服务，请输入以下URL:
     * http://receiver.ta.thinkingdata.cn:9080
     * 如果您使用的是私有化部署的版本，请输入以下URL:
     * http://数据采集地址:9080
     */
//   private static final String TA_SERVER_URL = "https://receiver.ta.thinkingdata.cn";
//   private static final String TA_SERVER_URL = "https://receiver-ta-dev.thinkingdata.cn";

    private static ThinkingAnalyticsSDK mInstance;
    private static ThinkingAnalyticsSDK mDebugInstance;
    private static ThinkingAnalyticsSDK mLightInstance;
    private static ThinkingAnalyticsSDK mDiffNameInstance;

    public static ThinkingAnalyticsSDK getInstance() {
        return mInstance;
    }

    public static ThinkingAnalyticsSDK getInstanceDiffName() {
        return mDiffNameInstance;
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
        mInstance = instance;
        mDebugInstance = debugInstance;
//        setUp();
    }

    /** 初始化 TA SDK */
    public static void initThinkingDataSDK(Context context) {
        Context mContext = context;
        TDConfig config = TDConfig.getInstance(mContext,TA_APP_ID,TA_SERVER_URL);
        config.setMutiprocess(true);
//        config.setMode(TDConfig.ModeEnum.DEBUG);
//        config.enableEncrypt(true);
//        config.enableEncrypt(true);
//        TDSecreteKey secreteKey = new TDSecreteKey();
//        secreteKey.publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCzAKEGsq67Yd03/RF77VKJ/cQ3zfSboK1wzlQfH2E1fr504WCJHHL/UVgjfUGUjMLIN15FNEelp7TXLToqtYlqqMbEXCfSc14ulRatKQioYnJ8EzgUhG0HcRlulni6vxGJHR9iq4weDNyJFRaZuwIQSrUzIaiVq/3hYijxxhhFqQIDAQAB";
//        secreteKey.version = 1;
//        secreteKey.symmetricEncryption = "AES";
//        secreteKey.asymmetricEncryption = "RSA";
//        config.setSecretKey(secreteKey);
//        config.setDefaultTimeZone(TimeZone.getTimeZone("GMT+00:00"));
//        config.setMode(TDConfig.ModeEnum.DEBUG);
        mInstance = ThinkingAnalyticsSDK.sharedInstance(config);
//        TDConfig config1 = TDConfig.getInstance(mContext, TA_APP_ID, TA_SERVER_URL, "instance1");
//        ThinkingAnalyticsSDK instance1 = ThinkingAnalyticsSDK.sharedInstance(config1);
//        Log.d("ThinkingAnalyticsSDK", "token =====> " + mInstance.getToken());
//        Log.d("ThinkingAnalyticsSDK", "token1 =====> " + instance1.getToken());
        setUp();
        enableAutoTrack();
        mInstance.timeEvent("test");
        mInstance.user_set(new JSONObject());
//        setUp();
//        enableAutoTrack();
//        mInstance.timeEvent("test");
//        mInstance.user_set(new JSONObject());
    }

    public static void initThinkingDataSDKWithName(Context context) {
        Context mContext = context;
        TDConfig config = TDConfig.getInstance(mContext,TA_APP_ID,TA_SERVER_URL, "test");
        config.setMutiprocess(true);
//        config.setDefaultTimeZone(TimeZone.getTimeZone("GMT+00:00"));
//        config.setMode(TDConfig.ModeEnum.DEBUG);
        mDiffNameInstance = ThinkingAnalyticsSDK.sharedInstance(config);
//        TDConfig config1 = TDConfig.getInstance(mContext, TA_APP_ID, TA_SERVER_URL, "instance1");
//        ThinkingAnalyticsSDK instance1 = ThinkingAnalyticsSDK.sharedInstance(config1);
//        Log.d("ThinkingAnalyticsSDK", "token =====> " + mInstance.getToken());
//        Log.d("ThinkingAnalyticsSDK", "token1 =====> " + instance1.getToken());
//        setUp();
//        enableAutoTrack();
//        mInstance.timeEvent("test");
//        mInstance.user_set(new JSONObject());
    }

    public  static  void enableAutoTrack()
    {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> typeList = new ArrayList<>();
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        typeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        //测试自动采集事件自定义属性
        JSONObject properties = new JSONObject();
        JSONObject properties1 = new JSONObject();
        try {
            properties.put("key1", "self value1");
            properties1.put("key1", "super value1");
            mInstance.setSuperProperties(properties1);
            //
//            mInstance.enableAutoTrack(typeList, new ThinkingAnalyticsSDK.AutoTrackEventListener() {
//                @Override
//                public JSONObject eventCallback(ThinkingAnalyticsSDK.AutoTrackEventType eventType, JSONObject properties) {
//                    try {
//                        return new JSONObject("{\"keykey\":\"value1111\"}");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        return null;
//                    }
//                }
//            });
//            mInstance.enableAutoTrack(typeList, new ThinkingAnalyticsSDK.AutoTrackEventListener() {
//                @Override
//                public JSONObject eventCallback(ThinkingAnalyticsSDK.AutoTrackEventType eventType, JSONObject properties) {
//                    try {
//                        return new JSONObject("{\"keykey\":\"value2222\"}");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                        return null;
//                    }
//                }
//            });
            mInstance.enableAutoTrack(typeList, properties);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void setUp() {
        // set distinct id
        mInstance.identify("instance_id");
        ThinkingAnalyticsSDK.enableTrackLog(true);
        mLightInstance = mInstance.createLightInstance();
    }
}
