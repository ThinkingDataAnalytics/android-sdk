package cn.thinkingdata.android.demo;

import android.content.Context;

import cn.thinkingdata.android.ThinkingAnalyticsSDK;

public class TDTracker {
    /**
     * 项目APP_ID，在申请项目时会给出
     */
    private static final String TA_APP_ID = "b2a61feb9e56472c90c5bcb320dfb4ef";
    private static final String TA_APP_ID_DEBUG = "debug-appid";

    /**
     * 数据上传地址
     * 如果您使用的是云服务，请输入以下URL:
     * http://receiver.ta.thinkingdata.cn:9080
     * 如果您使用的是私有化部署的版本，请输入以下URL:
     * http://数据采集地址:9080
     */
    private static final String TA_SERVER_URL = "https://sdk.tga.thinkinggame.cn";

    private static ThinkingAnalyticsSDK mInstance;
    private static ThinkingAnalyticsSDK mDebugInstance;
    private static ThinkingAnalyticsSDK mLightInstance;

    public static ThinkingAnalyticsSDK getInstance() {
        return mInstance;
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
        setUp();
    }

    /** 初始化 TA SDK */
    static void initThinkingDataSDK(Context context) {
        Context mContext = context.getApplicationContext();
        mInstance = ThinkingAnalyticsSDK.sharedInstance(mContext, TA_APP_ID, TA_SERVER_URL);
        mDebugInstance = ThinkingAnalyticsSDK.sharedInstance(mContext, TA_APP_ID_DEBUG, TA_SERVER_URL);
        setUp();
    }

    private static void setUp() {

        //Log.d("ThinkingDataDemo","get distinct id: " + ThinkingAnalyticsSDK.sharedInstance(this).getDistinctId());

        // set distinct id
        mInstance.identify("instance_id");
        mDebugInstance.identify("debug_instance_id");
        mLightInstance = mInstance.createLightInstance();

        // enable auto track
        //List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        //mDebugInstance.enableAutoTrack(eventTypeList);

        //// enable fragment auto track
        ////mInstance.trackFragmentAppViewScreen();
        //mDebugInstance.trackFragmentAppViewScreen();

        // 设置动态属性
        //mInstance.setDynamicSuperPropertiesTracker(
        //        new ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker() {
        //    @Override
        //    public JSONObject getDynamicSuperProperties() {
        //        JSONObject dynamicSuperProperties = new JSONObject();
        //        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        //        SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern, Locale.CHINA);
        //        String timeString = sDateFormat.format(new Date());
        //        try {
        //            dynamicSuperProperties.put("dynamicTime", timeString);
        //        } catch (JSONException e) {
        //            e.printStackTrace();
        //        }
        //        return dynamicSuperProperties;
        //    }
        //});
    }
}
