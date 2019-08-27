package cn.thinkingdata.android;

import android.app.Application;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnitRunner;

import cn.thinkingdata.android.demo.TDTracker;
import cn.thinkingdata.android.utils.TDLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestRunner extends AndroidJUnitRunner {


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

    private static final int POLL_WAIT_SECONDS = 2;
    private static final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();

    public static JSONObject getEvent() throws InterruptedException {
        return messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
    }

    private static ThinkingAnalyticsSDK mInstance;
    private static ThinkingAnalyticsSDK mDebugInstance;

    public static ThinkingAnalyticsSDK getInstance() {
        return  mInstance;
    }

    public static ThinkingAnalyticsSDK getDebugInstance() {
        return mDebugInstance;
    }

    /** 初始化 TA SDK */
    private void initThinkingDataSDK() {
        ThinkingAnalyticsSDK.enableTrackLog(true);
        Context mAppContext = ApplicationProvider.getApplicationContext();
        TDConfig mConfig = TDConfig.getInstance(mAppContext, TA_SERVER_URL, TA_APP_ID);
        final DataHandle dataHandle = new DataHandle(mAppContext) {
            @Override
            protected DatabaseAdapter getDbAdapter(Context context) {
                return new DatabaseAdapter(context) {
                    @Override
                    public int addJSON(JSONObject j, Table table, String token) {
                        try {
                            TDLog.i("THINKING_TEST", j.toString(4));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        messages.add(j);
                        return 1;
                    }
                };
            }
        };
        mInstance = new ThinkingAnalyticsSDK(mAppContext, TA_APP_ID, mConfig, false) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }
        };

        ThinkingAnalyticsSDK.addInstance(mInstance, mAppContext, TA_APP_ID);
        mDebugInstance = new ThinkingAnalyticsSDK(mAppContext, TA_APP_ID_DEBUG, mConfig, false) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }
        };
        ThinkingAnalyticsSDK.addInstance(mDebugInstance, mAppContext, TA_APP_ID_DEBUG);

        TDTracker.initThinkingDataSDK(mInstance, mDebugInstance);
    }

    @Override
    public void callApplicationOnCreate(Application app) {
        initThinkingDataSDK();
    }
}
