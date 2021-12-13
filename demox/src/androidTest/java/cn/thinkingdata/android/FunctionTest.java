package cn.thinkingdata.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static cn.thinkingdata.android.TestUtils.KEY_DATA;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.thinkingdata.android.demo.MainActivity;
import cn.thinkingdata.android.demo.TDTracker;
import cn.thinkingdata.android.demo.subprocess.TDSubprocessActivity;
import cn.thinkingdata.android.utils.TDConstants;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class FunctionTest {

    private static final String TAG = "TA_TEST.FunctionTest";

    /**
     * 项目APP_ID，在申请项目时会给出
     */
    private static final String TA_APP_ID = "1b1c1fef65e3482bad5c9d0e6a823356";
    private static final String TA_APP_ID_ = "1b1c1f  ef65e3482bad5c9d0e6  a823356";
    private static final String TA_APP_ID_DEBUG = "debug-appid";

    /**
     * 数据上传地址
     * 如果您使用的是云服务，请输入以下URL:
     * http://receiver.ta.thinkingdata.cn:9080
     * 如果您使用的是私有化部署的版本，请输入以下URL:
     * http://数据采集地址:9080
     */
    private static final String TA_SERVER_URL = "https://receiver.ta.thinkingdata.cn";
    private static final String TA_SERVER_URL_DEBUG = "https://receiver.ta.thinkingdatadebug.cn";

    private static final int POLL_WAIT_SECONDS = 5;
    // 阻塞队列，存放本来要入库的事件json串
    private static final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
    private static ThinkingAnalyticsSDK mInstance;
    private static ThinkingAnalyticsSDK mDebugInstance;
    private final Context mAppContext = ApplicationProvider.getApplicationContext();

    public static JSONObject getEvent() throws InterruptedException {
        //取队列的首位元素，允许工作500毫秒延迟，超时或者取不到返回null
        return messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
    }

    public static ThinkingAnalyticsSDK getInstance() {
        return mInstance;
    }

    public static ThinkingAnalyticsSDK getDebugInstance() {
        return mDebugInstance;
    }


    /**
     * 初始化 TA SDK
     */
    public void initThinkingDataSDK() {
        Log.d(TAG, "initThinkingDataSDK ->");
        ThinkingAnalyticsSDK.enableTrackLog(true);
        TDConfig mConfig = TDConfig.getInstance(mAppContext, TA_APP_ID, TA_SERVER_URL);
        mConfig.setMutiprocess(true);
        final DataHandle dataHandle = new DataHandle(mAppContext) {
            @Override
            protected DatabaseAdapter getDbAdapter(Context context) {
                return new DatabaseAdapter(context) {
                    @Override
                    public int addJSON(JSONObject j, Table table, String token) {
                        //把原本要入库的事件插入到阻塞队列
                        messages.add(j);
                        return 1;
                    }
                };
            }


        };

        mInstance = new ThinkingAnalyticsSDK(mConfig) {
            //重写 设置dataHandle
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }
        };
        ThinkingAnalyticsSDK.addInstance(mInstance, mAppContext, TA_APP_ID);

        mDebugInstance = new ThinkingAnalyticsSDK(TDConfig.getInstance(mAppContext, TA_APP_ID_DEBUG, TA_SERVER_URL)) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }
        };
        ThinkingAnalyticsSDK.addInstance(mDebugInstance, mAppContext, TA_APP_ID_DEBUG);
        //初始化TDTracker中的instance debugInstance和lightInstance
        TDTracker.initThinkingDataSDK(mInstance, mDebugInstance);
        Log.d(TAG, "initThinkingDataSDK <-");
    }

    private void checkPresetEventProperties(JSONObject properties) {
        assertEquals(properties.optString("#lib_version"), TDConfig.VERSION);
        assertEquals(properties.optString("#lib"), "Android");
        assertEquals(properties.optString("#os"), "Android");
        assertTrue(properties.has("#zone_offset"));
        assertTrue(properties.has("#network_type"));
    }

    private void assertTime(ThinkingAnalyticsSDK instance, final BlockingQueue<JSONObject> messages, long timestamp)
            throws JSONException, InterruptedException, ParseException {
        int DEFAULT_INTERVAL = 50;
        SimpleDateFormat dateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);

        instance.track("test_event");
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        Date time = dateFormat.parse(event.getString("#time"));
        assert time != null;
        assertTrue(time.getTime() - timestamp < DEFAULT_INTERVAL);
        assertEquals(event.getString("#event_name"), "test_event");

        instance.user_set(new JSONObject());
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        time = dateFormat.parse(event.getString("#time"));
        assert time != null;
        assertTrue(time.getTime() - timestamp < 2 * DEFAULT_INTERVAL);
        assertEquals(event.getString("#type"), "user_set");

        instance.user_setOnce(new JSONObject());
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        time = dateFormat.parse(event.getString("#time"));
        assert time != null;
        assertTrue(time.getTime() - timestamp < 3 * DEFAULT_INTERVAL);
        assertEquals(event.getString("#type"), "user_setOnce");

        instance.user_add(new JSONObject());
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        time = dateFormat.parse(event.getString("#time"));
        assert time != null;
        assertTrue(time.getTime() - timestamp < 4 * DEFAULT_INTERVAL);
        assertEquals(event.getString("#type"), "user_add");

        instance.user_append(new JSONObject());
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        time = dateFormat.parse(event.getString("#time"));
        assert time != null;
        assertTrue(time.getTime() - timestamp < 5 * DEFAULT_INTERVAL);
        assertEquals(event.getString("#type"), "user_append");

        instance.user_unset("");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        time = dateFormat.parse(event.getString("#time"));
        assert time != null;
        assertTrue(time.getTime() - timestamp < 6 * DEFAULT_INTERVAL);
        assertEquals(event.getString("#type"), "user_unset");

        instance.user_delete();
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        time = dateFormat.parse(event.getString("#time"));
        assert time != null;
        assertTrue(time.getTime() - timestamp < 7 * DEFAULT_INTERVAL);
        assertEquals(event.getString("#type"), "user_del");
    }

    @After
    public void afterTest() {
        TestUtils.postToServer(testProperties);
    }

    TestProperties testProperties = new TestProperties();

    @Test
    public void Test_00000() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_00000 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_00000");
        testProperties.setName("验证install事件的正确性");
        testProperties.setStep("step1:模拟应用第一次启动，初始化SDK，启用install事件；step2:模拟应用第二次启动，初始化SDK，启用install事件");
        testProperties.setExcept("step1:事件对象中包含install事件；step2:事件对象中不包含install事件");
        Looper.prepare();
        Context mAppContext = ApplicationProvider.getApplicationContext();
        TestUtils.clearData(mAppContext);
        TDTracker.initThinkingDataSDK(mAppContext);
        TDTracker.getInstance().setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType.NETWORKTYPE_WIFI);
        List<ThinkingAnalyticsSDK.AutoTrackEventType> list = new ArrayList<>();
        list.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        TDTracker.getInstance().enableAutoTrack(list);
        // check
        Thread.sleep(500);
        JSONObject jsonObject = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject.optString("#type"));
        assertEquals("ta_app_install", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        checkPresetEventProperties(properties);
        testProperties.setResult(true);
        Log.d(TAG, "Test_00000 -> 验证install事件的正确性 <-");
    }

    @Test
    public void Test_00001() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_00001 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_00001");
        testProperties.setName("验证start事件的正确性-冷启动");
        testProperties.setStep("step1:模拟应用启动，初始化SDK，启用start事件");
        testProperties.setExcept("step1:事件对象中包含start事件，#resume_from_background 为false");
        List<ThinkingAnalyticsSDK.AutoTrackEventType> list = new ArrayList<>();
        list.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        TDTracker.getInstance().enableAutoTrack(list);
        ActivityScenario.launch(MainActivity.class);
        // check
        Thread.sleep(500);
        JSONObject jsonObject = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject.optString("#type"));
        assertEquals("ta_app_start", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertFalse(properties.optBoolean("#resume_from_background"));
        checkPresetEventProperties(properties);
        testProperties.setResult(true);
        Log.d(TAG, "Test_00001 -> 验证start事件的正确性 冷启动<-");
    }

    @Test
    public void Test_00002() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_00002->");
        testProperties.setResult(false);
        testProperties.setId("Test_00002");
        testProperties.setName("验证start事件的正确性-热启动");
        testProperties.setStep("step1:模拟应用启动，初始化SDK，进入后台再进入前台");
        testProperties.setExcept("step1:事件对象中包含start事件,#resume_from_background 为true");
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.RESUMED);
        // check
        Thread.sleep(500);
        JSONObject jsonObject = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject.optString("#type"));
        assertEquals("ta_app_start", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertTrue(properties.optBoolean("#resume_from_background"));
        checkPresetEventProperties(properties);
        testProperties.setResult(true);
        Log.d(TAG, "Test_00002 -> 验证start事件的正确性 热启动 <-");
    }

    @Test
    public void Test_00003() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_00003->");
        testProperties.setResult(false);
        testProperties.setId("Test_00003");
        testProperties.setName("验证end事件的正确性");
        testProperties.setStep("step1:模拟应用启动，初始化SDK，进入后台");
        testProperties.setExcept("step1:事件对象中包含end事件");
        List<ThinkingAnalyticsSDK.AutoTrackEventType> list = new ArrayList<>();
        list.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        TDTracker.getInstance().enableAutoTrack(list);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.DESTROYED);
        // check
        Thread.sleep(500);
        JSONObject jsonObject = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject.optString("#type"));
        assertEquals("ta_app_end", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        checkPresetEventProperties(properties);
        testProperties.setResult(true);
        Log.d(TAG, "Test_00003 -> 验证end事件的正确性 <-");
    }

    @Test
    public void Test_00004() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_00004->");
        testProperties.setResult(false);
        testProperties.setId("Test_00004");
        testProperties.setName("自动采集自定义属性");
        testProperties.setStep("step1:初始化实例1，开启自动采集，设置自定义属性，触发事件，验证发送数据中是否存在自定义属性；step2:发送普通事件");
        testProperties.setExcept("step1:自定义事件属性内包含自定义属性；step2:普通事件内不包含自定义属性");
        List<ThinkingAnalyticsSDK.AutoTrackEventType> list = new ArrayList<>();
        list.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        JSONObject prop = new JSONObject();
        prop.put("autoEventKey1", "autoEventValue1");
        TDTracker.getInstance().enableAutoTrack(list, prop);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.RESUMED);
        // check
        Thread.sleep(500);
        JSONObject jsonObject = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject.optString("#type"));
        assertEquals("ta_app_start", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("autoEventValue1", properties.optString("autoEventKey1"));
        checkPresetEventProperties(properties);

        //普通事件
        TDTracker.getInstance().track("testEvent");
        Thread.sleep(500);
        JSONObject jsonObject1 = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject1.optString("#type"));
        assertEquals("testEvent", jsonObject1.optString("#event_name"));
        JSONObject properties1 = jsonObject1.optJSONObject("properties");
        assertFalse(properties1.has("autoEventKey1"));
        checkPresetEventProperties(properties1);
        testProperties.setResult(true);
        Log.d(TAG, "Test_00004 -> 验证自动采集事件自定义属性的正确性 <-");
    }

    @Test
    public void Test_00005() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_00005->");
        testProperties.setResult(false);
        testProperties.setId("Test_00005");
        testProperties.setName("验证自动采集自定义属性覆盖");
        testProperties.setStep("step1:为app_start事件设置相同key的自定义属性");
        testProperties.setExcept("step1:autoEventKey1对应的value变成autoEventValue2");
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.RESUMED);
        // check
        Thread.sleep(500);
        JSONObject jsonObject = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject.optString("#type"));
        assertEquals("ta_app_start", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("autoEventValue1", properties.optString("autoEventKey1"));
        checkPresetEventProperties(properties);
        //覆盖
        List<ThinkingAnalyticsSDK.AutoTrackEventType> list = new ArrayList<>();
        list.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        JSONObject prop = new JSONObject();
        prop.put("autoEventKey1", "autoEventValue2");
        TDTracker.getInstance().enableAutoTrack(list, prop);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.DESTROYED);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.RESUMED);
        // check
        Thread.sleep(500);
        JSONObject jsonObject1 = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject1.optString("#type"));
        assertEquals("ta_app_start", jsonObject1.optString("#event_name"));
        JSONObject properties1 = jsonObject1.optJSONObject("properties");
        assertEquals("autoEventValue2", properties1.optString("autoEventKey1"));
        checkPresetEventProperties(properties1);
        testProperties.setResult(true);
        Log.d(TAG, "Test_00005 -> 验证自动采集事件自定义属性覆盖 <-");
    }

    @Test
    public void Test_00006() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_00006->");
        testProperties.setResult(false);
        testProperties.setId("Test_00006");
        testProperties.setName("验证子进程自动采集事件不带主进程自定义属性");
        testProperties.setStep("step1:在主进程设置自定义属性autoEventKey1后启动子进程并设置自定义属性后模拟app_start");
        testProperties.setExcept("step1:子进程app_start事件的属性中只有子进程的自定义属性，没有autoEventKey1");
        ActivityScenario.launch(MainActivity.class).onActivity(activity ->
        {
            Intent intent = new Intent(activity, TDSubprocessActivity.class);
            intent.putExtra("enableAuto", true);
            activity.startActivity(intent);
        });
        //子进程
        // check
        Thread.sleep(1000);
        JSONObject jsonObject = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject.optString("#type"));
        assertEquals("ta_app_start", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("cn.thinkingdata.android.demo:xx", properties.optString("#bundle_id"));
        assertEquals("value2", properties.optString("SUB_AUTO_EVENT_PROP2"));
        assertFalse( properties.has("autoEventKey1"));
        checkPresetEventProperties(properties);

        testProperties.setResult(true);
        Log.d(TAG, "Test_00006 -> 验证子进程自动采集事件不带主进程自定义属性 <-");
    }

    @Test
    public void Test_00007() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_00007->");
        testProperties.setResult(false);
        testProperties.setId("Test_00007");
        testProperties.setName("验证自动采集自定义属性，静态公共属性，动态公共属性，预置属性，传入属性优先级");
        testProperties.setStep("step1:设置静态公共属性#app_version为2.0 覆盖预置属性1.0；step2:设置自动采集事件的自定义属性3.0覆盖公共属性#app_version 的2.0 ；step3:设置动态公共属性4.0覆盖自动采集事件的自定义属性#app_version 的3.0 ；step4:设置传入属性5.0覆盖动态公共属性#app_version 的4.0 ");
        testProperties.setExcept("step1:事件Properties内#app_version为2.0；step2:事件Properties内#app_version为3.0；step3:事件Properties内#app_version为4.0；step4:事件Properties内#app_version为5.0");
        //#app_version 1.0
        //静态属性
        JSONObject superProp = new JSONObject();
        superProp.put("#app_version", "2.0");
        TDTracker.getInstance().setSuperProperties(superProp);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.RESUMED);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.DESTROYED);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.RESUMED);
        // check
        Thread.sleep(500);
        JSONObject jsonObject = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject.optString("#type"));
        assertEquals("ta_app_start", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("2.0", properties.optString("#app_version"));
        //自定义事件自定义属性
        JSONObject staticProp = new JSONObject();
        staticProp.put("#app_version", "3.0");
        List<ThinkingAnalyticsSDK.AutoTrackEventType> list = new ArrayList<>();
        list.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        TDTracker.getInstance().enableAutoTrack(list, staticProp);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.DESTROYED);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.RESUMED);
        // check
        Thread.sleep(500);
        JSONObject jsonObject1 = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject1.optString("#type"));
        assertEquals("ta_app_start", jsonObject1.optString("#event_name"));
        JSONObject properties1 = jsonObject1.optJSONObject("properties");
        assertEquals("3.0", properties1.optString("#app_version"));
        //动态自定义属性
        TDTracker.getInstance().setDynamicSuperPropertiesTracker(() -> {
            JSONObject dynamicProp = new JSONObject();
            try {
                dynamicProp.put("#app_version", "4.0");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return dynamicProp;
        });
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.DESTROYED);
        ActivityScenario.launch(MainActivity.class).moveToState(Lifecycle.State.RESUMED);
        // check
        Thread.sleep(500);
        JSONObject jsonObject2 = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject2.optString("#type"));
        assertEquals("ta_app_start", jsonObject2.optString("#event_name"));
        JSONObject properties2 = jsonObject2.optJSONObject("properties");
        assertEquals("4.0", properties2.optString("#app_version"));
        //传入属性
        JSONObject prop = new JSONObject();
        prop.put("#app_version", "5.0");
        TDTracker.getInstance().track("testTrack", prop);
        // check
        Thread.sleep(500);
        JSONObject jsonObject3 = new JSONObject(new TestUtils.DatabaseHelper(mAppContext, "thinkingdata").getFirstEvent(TA_APP_ID).optJSONObject(0).optString(KEY_DATA));
        assertEquals("track", jsonObject3.optString("#type"));
        assertEquals("testTrack", jsonObject3.optString("#event_name"));
        JSONObject properties3 = jsonObject3.optJSONObject("properties");
        assertEquals("5.0", properties3.optString("#app_version"));
        testProperties.setResult(true);
        Log.d(TAG, "Test_00007 -> 验证自动采集预置属性<静态公共属性<自定义属性<动态公共属性<传入属性 的优先级 <-");
    }



    @Test
    public void Test_10000() {
        Log.d(TAG, "Test_10000 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_10000");
        testProperties.setName("验证SDK实例初始化正确性");
        testProperties.setStep("step1:使用测试AppId和Url完成SDK实例的初始化");
        testProperties.setExcept("step1:TDConfig实例和SDK实例内的参数应与输入的测试参数一致");
        TestUtils.clearData(mAppContext);
        //测试创建实例的正确性
        TDConfig mConfig = TDConfig.getInstance(mAppContext, TA_APP_ID, TA_SERVER_URL);
        ThinkingAnalyticsSDK thinkingAnalyticsSDK = ThinkingAnalyticsSDK.sharedInstance(mConfig);
        assertEquals(TA_APP_ID, thinkingAnalyticsSDK.mConfig.mToken);
        assertEquals(TA_APP_ID, thinkingAnalyticsSDK.mConfig.getName());
        assertEquals(TA_SERVER_URL + "/sync", thinkingAnalyticsSDK.mConfig.getServerUrl());
        testProperties.setResult(true);
        Log.d(TAG, "Test_10000 -> 测试创建实例的正确性 <-");
    }

    @Test
    public void Test_10001() {
        Log.d(TAG, "Test_10001 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_10001");
        testProperties.setName("验证SDK多实例初始化的正确性");
        testProperties.setStep("step1:使用2个相同的AppId，相同的Url，Name为空，进行SDK实例的初始化；step2:使用2个相同的AppId，不同的Url，Name为空，进行SDK实例的初始化；step3:使用2个不相同的AppId，相同的Url，Name为空，进行SDK实例的初始化；step4:使用2个相同的AppId，相同的Url，不同的Name，进行SDK实例的初始化");
        testProperties.setExcept("step1:只存在一个实例，并且实例中参数与输入一致；step2:只存在一个实例，并且实例中参数与首先初始化的输入一致；step3:存在2个实例，并且实例中参数与对应的输入一致；step4:存在2个实例，并且实例中参数与对应的输入一致");
        //测试和10000相同的appId和Url
        TDConfig mConfig = TDConfig.getInstance(mAppContext, TA_APP_ID, TA_SERVER_URL);
        ThinkingAnalyticsSDK thinkingAnalyticsSDK = ThinkingAnalyticsSDK.sharedInstance(mConfig);
        Map<String, ThinkingAnalyticsSDK> instances = ThinkingAnalyticsSDK.getInstanceMap(mAppContext);
        assertEquals(1, instances.size());
        ThinkingAnalyticsSDK instance = instances.get(TA_APP_ID);
        assertEquals(TA_APP_ID, instance.mConfig.mToken);
        assertEquals(TA_APP_ID, instance.mConfig.getName());
        assertEquals(TA_SERVER_URL + "/sync", instance.mConfig.getServerUrl());
        Log.d(TAG, "Test_10001 -> 测试和10000相同的appId和Url <-");
        //测试相同的appId、不同Url
        TDConfig mConfig1 = TDConfig.getInstance(mAppContext, TA_APP_ID, TA_SERVER_URL_DEBUG);
        ThinkingAnalyticsSDK thinkingAnalyticsSDK1 = ThinkingAnalyticsSDK.sharedInstance(mConfig1);
        Map<String, ThinkingAnalyticsSDK> instances1 = ThinkingAnalyticsSDK.getInstanceMap(mAppContext);
        assertEquals(1, instances1.size());
        ThinkingAnalyticsSDK instance1 = instances1.get(TA_APP_ID);
        assertEquals(TA_APP_ID, instance1.mConfig.mToken);
        assertEquals(TA_APP_ID, instance1.mConfig.getName());
        assertEquals(TA_SERVER_URL + "/sync", instance1.mConfig.getServerUrl());
        Log.d(TAG, "Test_10001 -> 测试相同的appId、不同Url <-");
        //测试不同的appId、相同Url
        TDConfig mConfig2 = TDConfig.getInstance(mAppContext, TA_APP_ID_DEBUG, TA_SERVER_URL);
        ThinkingAnalyticsSDK thinkingAnalyticsSDK2 = ThinkingAnalyticsSDK.sharedInstance(mConfig2);
        Map<String, ThinkingAnalyticsSDK> instances2 = ThinkingAnalyticsSDK.getInstanceMap(mAppContext);
        assertEquals(2, instances2.size());
        ThinkingAnalyticsSDK instance001 = instances2.get(TA_APP_ID);
        assertEquals(TA_APP_ID, instance001.mConfig.mToken);
        assertEquals(TA_APP_ID, instance001.mConfig.getName());
        assertEquals(TA_SERVER_URL + "/sync", instance001.mConfig.getServerUrl());
        ThinkingAnalyticsSDK instance002 = instances2.get(TA_APP_ID_DEBUG);
        assertEquals(TA_APP_ID_DEBUG, instance002.mConfig.mToken);
        assertEquals(TA_APP_ID_DEBUG, instance002.mConfig.getName());
        assertEquals(TA_SERVER_URL + "/sync", instance002.mConfig.getServerUrl());
        //移除debugApp 实例
        ThinkingAnalyticsSDK.getInstanceMap(mAppContext).remove(TA_APP_ID_DEBUG);
        Log.d(TAG, "Test_10001 -> 测试不同的appId、相同Url <-");
        //测试相同appId和url，不同的name
        TDConfig mConfig3 = TDConfig.getInstance(mAppContext, TA_APP_ID, TA_SERVER_URL, "testName");
        ThinkingAnalyticsSDK thinkingAnalyticsSDK3 = ThinkingAnalyticsSDK.sharedInstance(mConfig3);
        Map<String, ThinkingAnalyticsSDK> instances3 = ThinkingAnalyticsSDK.getInstanceMap(mAppContext);
        assertEquals(2, instances3.size());
        ThinkingAnalyticsSDK instance003 = instances3.get(TA_APP_ID);
        assertEquals(TA_APP_ID, instance003.mConfig.mToken);
        assertEquals(TA_APP_ID, instance003.mConfig.getName());
        assertEquals(TA_SERVER_URL + "/sync", instance003.mConfig.getServerUrl());
        ThinkingAnalyticsSDK instance004 = instances3.get("testName");
        assertEquals(TA_APP_ID, instance004.mConfig.mToken);
        assertEquals("testName", instance004.mConfig.getName());
        assertEquals(TA_SERVER_URL + "/sync", instance004.mConfig.getServerUrl());
        ThinkingAnalyticsSDK.getInstanceMap(mAppContext).clear();
        testProperties.setResult(true);
        Log.d(TAG, "Test_10001 -> 测试相同appId和url，不同的name <-");
    }

    @Test
    public void Test_10002() {
        Log.d(TAG, "Test_10002 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_10002");
        testProperties.setName("验证SDK轻实例初始化的正确性");
        testProperties.setStep("step1:使用测试AppId和Url创建轻实例");
        testProperties.setExcept("step1:轻实例不为空，并且参数与输入一致");
        //测试创建轻实例的正确性
        TDConfig mConfig = TDConfig.getInstance(mAppContext, TA_APP_ID, TA_SERVER_URL);
        ThinkingAnalyticsSDK thinkingAnalyticsSDK = ThinkingAnalyticsSDK.sharedInstance(mConfig);
        ThinkingAnalyticsSDK lightInstance = thinkingAnalyticsSDK.createLightInstance();
        assertEquals(TA_APP_ID, lightInstance.mConfig.mToken);
        assertEquals(TA_APP_ID, lightInstance.mConfig.getName());
        assertEquals(TA_SERVER_URL + "/sync", lightInstance.mConfig.getServerUrl());
        testProperties.setResult(true);
        Log.d(TAG, "Test_10002 -> 测试创建轻实例的正确性 <-");
        thinkingAnalyticsSDK.identify("111111su");
        TestUtils.clearData(mAppContext);
    }


    @Test
    public void Test_10003() throws InterruptedException {
        Log.d(TAG, "Test_10003 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_10003");
        testProperties.setName("验证SDK子进程实例初始化的正确性");
        testProperties.setStep("step1:在子进程中使用测试AppId和Url进行SDK实例初始化");
        testProperties.setExcept("step1:子进程实例存在，并且参数与输入一致");
        initThinkingDataSDK();
        ActivityScenario.launch(MainActivity.class).onActivity(activity ->
        {
            Intent intent = new Intent(activity, TDSubprocessActivity.class);
            activity.startActivity(intent);
        });
        Thread.sleep(500);
        JSONObject jsonObject = getEvent();
        assertEquals("subprocessTestEvent", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("cn.thinkingdata.android.demo:xx", properties.optString("#bundle_id"));

        testProperties.setResult(true);
        Log.d(TAG, "Test_10003 -> 测试子进程创建实例的正确性 <-");

    }

    @Test
    public void Test_11000() throws JSONException, InterruptedException {
        Log.d(TAG, "Test_11000 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11000");
        testProperties.setName("验证track方法的正确性");
        testProperties.setStep("step1:传入eventName、property，验证内部创建的事件对象");
        testProperties.setExcept("step1:事件对象有值、eventName、property和传入的一致；timeZone使用默认时区;预置属性的键值和预期的一致");
        TestUtils.clearData(mAppContext);
        messages.clear();
        JSONObject prop = new JSONObject();
        JSONObject ob = new JSONObject();
        JSONObject ob1 = new JSONObject();
        JSONArray arr = new JSONArray();
        //properties
        prop.put("KEY_STRING", "A string value");
        prop.put("KEY_DATE", new Date());
        prop.put("KEY_BOOLEAN", true);
        prop.put("KEY_DOUBLE", 56.17);
        prop.put("KEY_G", "哈哈MainActivity");
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
        ob.put("date", new Date());
        arr.put(ob);
        prop.put("arr", arr);
        prop.put("ob1", ob1);
        //send
        TDTracker.getInstance().track("test", prop);
        Thread.sleep(500);
        //check
        JSONObject jsonObject = getEvent();
        assertTrue(TestUtils.convertStamp(jsonObject.optString("#time")));
        assertEquals("test", jsonObject.optString("#event_name"));
        //properties
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("A string value", properties.optString("KEY_STRING"));
        assertTrue(properties.optBoolean("KEY_BOOLEAN"));
        assertEquals(56.17, properties.optDouble("KEY_DOUBLE"), 0);
        assertEquals("哈哈MainActivity", properties.optString("KEY_G"));
        assertTrue(TestUtils.convertStamp(properties.optString("KEY_DATE")));
        checkPresetEventProperties(properties);
        //jsonArray
        JSONArray jsonArray = properties.optJSONArray("arr");
        assertTrue(TestUtils.convertStamp(jsonArray.getString(0)));
        assertEquals("string", jsonArray.getString(1));
        assertEquals(1, jsonArray.getInt(2));
        assertTrue(jsonArray.getBoolean(3));
        JSONObject dateObj = jsonArray.getJSONObject(4);
        assertTrue(TestUtils.convertStamp(dateObj.optString("date")));
        //jsonObject
        JSONObject jsonObj = properties.optJSONObject("ob1");
        assertEquals("A string value", jsonObj.optString("KEY_STRING"));
        assertTrue(jsonObj.optBoolean("KEY_BOOLEAN"));
        assertEquals(56.17, jsonObj.optDouble("KEY_DOUBLE"), 0);
        assertEquals("哈哈", jsonObj.optString("KEY_G"));
        assertTrue(TestUtils.convertStamp(jsonObj.optString("KEY_DATE")));

        testProperties.setResult(true);
        Log.d(TAG, "Test_11000 -> 测试普通track事件的正确性 <-");
    }

    @Test
    public void Test_11001() throws JSONException, InterruptedException {
        Log.d(TAG, "Test_11001 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11001");
        testProperties.setName("验证首次事件的正确性");
        testProperties.setStep("step1:传入eventName、firstCheckID，验证内部创建的事件对象；step2:传入eventName、不传入firstCheckID，验证内部创建的事件对象");
        testProperties.setExcept("step1:事件对象的eventName和传入的一致、入库数据的firstCheckID和传入的一致、事件类型为track,预置属性的键值和预期的一致；step2:事件对象的eventName和传入的一致、入库数据的firstCheckID和设备ID一致、事件类型为track,预置属性的键值和预期的一致");
        //默认以设备id为firstCheckID
        JSONObject prop = new JSONObject();
        prop.put("Test_11001", "111111");
        TDTracker.getInstance().track(new TDFirstEvent("DEVICE_FIRST", prop));
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("track", jsonObject.optString("#type"));
        assertEquals("DEVICE_FIRST", jsonObject.optString("#event_name"));
        assertEquals(TDTracker.getInstance().getDeviceId(), jsonObject.optString("#first_check_id"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        checkPresetEventProperties(properties);

        //自定义firstCheckID
        JSONObject prop1 = new JSONObject();
        prop1.put("Test_11001", "111111");
        TDFirstEvent firstEvent = new TDFirstEvent("USER_FIRST", prop1);
        firstEvent.setFirstCheckId("my_account_id");
        TDTracker.getInstance().track(firstEvent);
        //check
        JSONObject jsonObject1 = getEvent();
        assertEquals("track", jsonObject1.optString("#type"));
        assertEquals("USER_FIRST", jsonObject1.optString("#event_name"));
        assertEquals("my_account_id", jsonObject1.optString("#first_check_id"));
        JSONObject properties1 = jsonObject1.optJSONObject("properties");
        checkPresetEventProperties(properties1);

        testProperties.setResult(true);
        Log.d(TAG, "Test_11001 -> 验证首次事件的正确性 <-");
    }

    @Test
    public void Test_11002() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_11002 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11002");
        testProperties.setName("验证更新事件的正确性");
        testProperties.setStep("step1:传入eventName、eventID，验证内部创建的事件对象；step2:验证eventName、eventID是空字符串或是null的情况");
        testProperties.setExcept("step1:事件对象的事件类型为track_update、event_name和event_id和传入的一致，执行了入库方法；step2:程序不崩溃");
        //正常更新
        JSONObject prop = new JSONObject();
        prop.put("Test_11002", "121121");
        TDTracker.getInstance().track(new TDUpdatableEvent("UPDATABLE_EVENT", prop, "test_event_id"));
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("track_update", jsonObject.optString("#type"));
        assertEquals("UPDATABLE_EVENT", jsonObject.optString("#event_name"));
        assertEquals("test_event_id", jsonObject.optString("#event_id"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        checkPresetEventProperties(properties);
        //null 事件名称 null event_id
        TDTracker.getInstance().track(new TDUpdatableEvent(null, prop, null));
        //check
        JSONObject jsonObject1 = getEvent();
        assertEquals("track_update", jsonObject1.optString("#type"));
        assertEquals("", jsonObject1.optString("#event_name"));
        assertEquals("", jsonObject1.optString("#event_id"));
        JSONObject properties1 = jsonObject1.optJSONObject("properties");
        checkPresetEventProperties(properties1);


        testProperties.setResult(true);
        Log.d(TAG, "Test_11002 -> 验证更新事件的正确性 <-");
    }

    @Test
    public void Test_11003() throws JSONException, InterruptedException {
        Log.d(TAG, "Test_11003 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11003");
        testProperties.setName("验证重写事件的正确性");
        testProperties.setStep("step1:传入eventName、eventID，验证内部创建的事件对象；step2:验证eventName、eventID是空字符串或是null的情况");
        testProperties.setExcept("step1:事件对象的事件类型为track_overwrite、event_name和event_id和传入的一致，并执行了入库的方法；step2:程序不崩溃");
        //正常重写
        JSONObject prop = new JSONObject();
        prop.put("Test_11003", "131313");
        TDTracker.getInstance().track(new TDOverWritableEvent("test_overwrite", prop, "test_event_id"));
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("track_overwrite", jsonObject.optString("#type"));
        assertEquals("test_overwrite", jsonObject.optString("#event_name"));
        assertEquals("test_event_id", jsonObject.optString("#event_id"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        checkPresetEventProperties(properties);
        //null eventName null eventId
        TDTracker.getInstance().track(new TDOverWritableEvent(null, prop, null));
        //check
        JSONObject jsonObject1 = getEvent();
        assertEquals("track_overwrite", jsonObject1.optString("#type"));
        assertEquals("", jsonObject1.optString("#event_name"));
        assertEquals("", jsonObject1.optString("#event_id"));
        JSONObject properties1 = jsonObject1.optJSONObject("properties");
        checkPresetEventProperties(properties1);


        testProperties.setResult(true);
        Log.d(TAG, "Test_11003 -> 验证重写事件的正确性 <-");
    }

    @Test
    public void Test_11004() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_11004 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11004");
        testProperties.setName("验证统计事件时长功能");
        testProperties.setStep("step1:传入eventName，等待5秒，调用track方法，获取内部创建的事件对象，比较duration时长");
        testProperties.setExcept("step1:事件对象的eventName和传入的一致；包含#duration字段，并且值-5为<±1，内部执行了入库的方法");
        TDTracker.getInstance().timeEvent("test_time_event");
        JSONObject prop = new JSONObject();
        prop.put("Test_11004", "22222");
        Thread.sleep(4000);
        TDTracker.getInstance().track("test_time_event", prop);
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("test_time_event", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("22222", properties.optString("Test_11004"));
        assertEquals(4.00, properties.optDouble("#duration"), 1);
        checkPresetEventProperties(properties);


        testProperties.setResult(true);
        Log.d(TAG, "Test_11004 -> 验证统计事件时长功能 <-");
    }


    @Test
    public void Test_11005() throws InterruptedException {
        Log.d(TAG, "Test_11005 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11005");
        testProperties.setName("验证设置访客ID的正确性");
        testProperties.setStep("step1:传入访客ID，并使用track方法，验证内部事件对象中的访客ID；step2:使用API获取访客ID，验证访客ID；step3:传入访客ID为空字符串或空");
        testProperties.setExcept("step1:内部事件对象的访客ID和传入的一致；step2:得到的访客ID和传入的一致；step3:程序不会crash");
        //正常访客id
        TDTracker.getInstance().identify("Test_11005");
        TDTracker.getInstance().track("Test_identify");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("Test_identify", jsonObject.optString("#event_name"));
        assertEquals("Test_11005", jsonObject.optString("#distinct_id"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        checkPresetEventProperties(properties);

        //Api验证
        assertEquals("Test_11005", TDTracker.getInstance().getDistinctId());

        //null 访客id
        TDTracker.getInstance().identify(null);
        TDTracker.getInstance().track("Test_identify1");
        //check
        JSONObject jsonObject1 = getEvent();
        assertEquals("Test_identify1", jsonObject1.optString("#event_name"));
        //访客id 为null 或者 空串 不会改变
        assertEquals("Test_11005", jsonObject1.optString("#distinct_id"));
        JSONObject properties1 = jsonObject1.optJSONObject("properties");
        checkPresetEventProperties(properties1);

        testProperties.setResult(true);
        Log.d(TAG, "Test_11005 -> 验证设置访客ID的正确性 <-");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
    }

    @Test
    public void Test_11006() throws InterruptedException {
        Log.d(TAG, "Test_11006 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11006");
        testProperties.setName("验证设置账号 ID 的正确性");
        testProperties.setStep("step1:验证设置账号 ID 的正确性");
        testProperties.setExcept("step1:内部事件对象的账号 ID 和传入的一致");
        //正常账号id
        TDTracker.getInstance().login("Test_11006");
        TDTracker.getInstance().track("Test_login");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("Test_login", jsonObject.optString("#event_name"));
        assertEquals("Test_11006", jsonObject.optString("#account_id"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        checkPresetEventProperties(properties);

        //null 账号id
        TDTracker.getInstance().login(null);
        TDTracker.getInstance().track("Test_login1");
        //check
        JSONObject jsonObject1 = getEvent();
        assertEquals("Test_login1", jsonObject1.optString("#event_name"));
        //账号id 为null 或者 空串 不会改变
        assertEquals("Test_11006", jsonObject1.optString("#account_id"));
        JSONObject properties1 = jsonObject1.optJSONObject("properties");
        checkPresetEventProperties(properties1);

        testProperties.setResult(true);
        Log.d(TAG, "Test_11006 -> 验证设置账号 ID 的正确性 <-");
    }

    @Test
    public void Test_11007() throws InterruptedException {
        Log.d(TAG, "Test_11007 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11007");
        testProperties.setName("验证移除账号 ID 的正确性");
        testProperties.setStep("step1:调用登出，再调用track事件，验证内部事件对象中的账号 ID ");
        testProperties.setExcept("step1:事件对象的不存在#account_id字段");
        //移除账号id
        TDTracker.getInstance().logout();
        TDTracker.getInstance().track("Test_logout");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("Test_logout", jsonObject.optString("#event_name"));
        assertFalse(jsonObject.has("#account_id"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        checkPresetEventProperties(properties);

        testProperties.setResult(true);
        Log.d(TAG, "Test_11007 -> 验证移除账号 ID 的正确性 <-");
    }

    @Test
    public void Test_11008() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_11008 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11008");
        testProperties.setName("验证设置静态公共属性正确性");
        testProperties.setStep("step1:传入静态公共属性，再调用track方法，验证内部事件对象;step2:验证静态公共属性的持久化情况");
        testProperties.setExcept("step1:内部事件对象包含静态公共属性;step2:调用了持久化方法");
        //设置静态公共属性
        JSONObject pubProp = new JSONObject();
        pubProp.put("super_pub_key", "super_pub_value");
        pubProp.put("super_pub_key1", "super_pub_value1");
        pubProp.put("super_pub_key2", "super_pub_value2");
        TDTracker.getInstance().setSuperProperties(pubProp);
        TDTracker.getInstance().track("testSetSuper");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("testSetSuper", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("super_pub_value", properties.optString("super_pub_key"));
        assertEquals("super_pub_value1", properties.optString("super_pub_key1"));
        assertEquals("super_pub_value2", properties.optString("super_pub_key2"));
        checkPresetEventProperties(properties);
        //check 持久化
        SharedPreferences sharedPreferences = mAppContext.getSharedPreferences("com.thinkingdata.analyse_" + TDTracker.getInstance().mConfig.getName(), Context.MODE_PRIVATE);
        assertEquals("super_pub_value", new JSONObject(sharedPreferences.getString("superProperties", "")).optString("super_pub_key"));
        assertEquals("super_pub_value1", new JSONObject(sharedPreferences.getString("superProperties", "")).optString("super_pub_key1"));
        assertEquals("super_pub_value2", new JSONObject(sharedPreferences.getString("superProperties", "")).optString("super_pub_key2"));

        testProperties.setResult(true);
        Log.d(TAG, "Test_11008 -> 验证设置静态公共属性正确性 <-");
    }

    @Test
    public void Test_11009() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_11009 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11009");
        testProperties.setName("验证清除一条静态公共属性正确性");
        testProperties.setStep("step1:传入superkey1属性名，再调用track方法，验证内部事件对象;step2:内部事件对象中不包含该属性");
        testProperties.setExcept("step1:传入空字符串或者空;step2:程序不崩溃");
        //移除静态公共属性
        TDTracker.getInstance().unsetSuperProperty("super_pub_key");
        TDTracker.getInstance().track("testUnSetSuper");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("testUnSetSuper", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertFalse(properties.has("super_pub_key"));
        assertEquals("super_pub_value1", properties.optString("super_pub_key1"));
        assertEquals("super_pub_value2", properties.optString("super_pub_key2"));
        checkPresetEventProperties(properties);
        //check 持久化
        SharedPreferences sharedPreferences = mAppContext.getSharedPreferences("com.thinkingdata.analyse_" + TDTracker.getInstance().mConfig.getName(), Context.MODE_PRIVATE);
        assertFalse(new JSONObject(sharedPreferences.getString("superProperties", "")).has("super_pub_key"));
        assertEquals("super_pub_value1", new JSONObject(sharedPreferences.getString("superProperties", "")).optString("super_pub_key1"));
        assertEquals("super_pub_value2", new JSONObject(sharedPreferences.getString("superProperties", "")).optString("super_pub_key2"));

        testProperties.setResult(true);
        Log.d(TAG, "Test_11009 -> 验证清除一条静态公共属性正确性 <-");
    }

    @Test
    public void Test_11010() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_11010 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11010");
        testProperties.setName("验证清除所有静态公共属性的正确性");
        testProperties.setStep("step1:调用API，再调用track方法，验证内部事件对象");
        testProperties.setExcept("step1:内部事件对象中不包含superkey1、superkey2、superkey3属性");
        //移除静态公共属性
        TDTracker.getInstance().clearSuperProperties();
        TDTracker.getInstance().track("testUnSetSuperAll");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("testUnSetSuperAll", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertFalse(properties.has("super_pub_key"));
        assertFalse(properties.has("super_pub_key1"));
        assertFalse(properties.has("super_pub_key2"));
        checkPresetEventProperties(properties);
        //check 持久化
        SharedPreferences sharedPreferences = mAppContext.getSharedPreferences("com.thinkingdata.analyse_" + TDTracker.getInstance().mConfig.getName(), Context.MODE_PRIVATE);
        assertFalse(new JSONObject(sharedPreferences.getString("superProperties", "")).has("super_pub_key"));
        assertFalse(new JSONObject(sharedPreferences.getString("superProperties", "")).has("super_pub_key1"));
        assertFalse(new JSONObject(sharedPreferences.getString("superProperties", "")).has("super_pub_key2"));

        testProperties.setResult(true);
        Log.d(TAG, "Test_11010 -> 验证清除所有静态公共属性正确性 <-");
    }

    @Test
    public void Test_11011() throws InterruptedException {
        Log.d(TAG, "Test_11011 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11011");
        testProperties.setName("验证设置动态公共属性的正确性");
        testProperties.setStep("step1:传入动态公共属性，再调用track方法，验证内部事件对象");
        testProperties.setExcept("step1:内部事件对象包含该动态公共属性");
        //设置动态公共属性
        TDTracker.getInstance().setDynamicSuperPropertiesTracker(() -> {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("dynamic_key", "dynamic_value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        });
        TDTracker.getInstance().track("testDynamicProp");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("testDynamicProp", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("dynamic_value", properties.optString("dynamic_key"));
        checkPresetEventProperties(properties);

        testProperties.setResult(true);
        Log.d(TAG, "Test_11011 -> 验证设置动态公共属性正确性 <-");
    }


    @Test
    public void Test_11012() {
        Log.d(TAG, "Test_11012 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11012");
        testProperties.setName("验证获取预置属性的正确性");
        testProperties.setStep("step1:调用此API，验证预制属性");
        testProperties.setExcept("step1:预制属性和预期的一致");
        TDPresetProperties presetProperties = TDTracker.getInstance().getPresetProperties();
        assertEquals("Android", presetProperties.os);
        testProperties.setResult(true);
        Log.d(TAG, "Test_11012 -> 验证获取预置属性的正确性 <-");
    }

    @Test
    public void Test_11013() {
        Log.d(TAG, "Test_11013 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11013");
        testProperties.setName("验证设置上传的网络条件的正确性");
        testProperties.setStep("step1:设置上传的网络条件为wifi，修改网络状态为wifi，再调用track事件和flush方法，验证事件发送情况;step2:设置上传的网络条件为wifi，修改网络为4G，再调用track事件和flush方法，验证事件发送情况");
        testProperties.setExcept("step1:config对象调用了setNetworkType方法；网络请求对象调用网络请求方法;step2:config对象调用了setNetworkType方法；网络请求对象不会调用网络请求方法");
//        initThinkingDataSDK();
//        TDTracker.getInstance().track("testEvent");
        testProperties.setResult(true);
        Log.d(TAG, "Test_11013 -> 验证设置上传的网络条件的正确性 <-");
    }

    @Test
    public void Test_11014() throws InterruptedException, ParseException {
        Log.d(TAG, "Test_11014 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11014");
        testProperties.setName("验证时区偏移的正确性");
        testProperties.setStep("step1:在config中未设置时区，发送track事件，验证内部创建的事件对象;step2:在config中设置自定义时区America/Sao_Paulo，发送track事件，验证内部创建的事件对象;step3:使用track方法，传入自定义的time、timeZone，验证内部创建的事件对象;step4:验证在config中设置不标准的时区的情况");
        testProperties.setExcept("step1:事件对象中的#zone_offset为8;step2:事件对象中的#zone_offset为-3;step3:事件对象中的#zone_offset为-7，#time为2016-01-05 18:33:40.000;step4:事件对象使用默认时区，#zone_offset为8");
        //在config中未设置时区，发送track事件，验证内部创建的事件对象
        TDTracker.getInstance().track("testNoTimeZone");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("testNoTimeZone", jsonObject.optString("#event_name"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals(8, properties.optInt("#zone_offset"));
        checkPresetEventProperties(properties);

        //在config中设置不正确格式时区，发送track事件，验证内部创建的事件对象
        TDTracker.getInstance().mConfig.setDefaultTimeZone(TimeZone.getTimeZone("21dsfa"));
        TDTracker.getInstance().track("testTimeZoneUnknown");
        //check
        JSONObject jsonObject1 = getEvent();
        assertEquals("testTimeZoneUnknown", jsonObject1.optString("#event_name"));
        JSONObject properties1 = jsonObject1.optJSONObject("properties");
        assertEquals(0, properties1.optInt("#zone_offset"));
        checkPresetEventProperties(properties1);

        //在config中设置时区，发送track事件，验证内部创建的事件对象  America/Sao_Paulo  -3
        TDTracker.getInstance().mConfig.setDefaultTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
        TDTracker.getInstance().track("testTimeZone-3");
        //check
        JSONObject jsonObject2 = getEvent();
        assertEquals("testTimeZone-3", jsonObject2.optString("#event_name"));
        JSONObject properties2 = jsonObject2.optJSONObject("properties");
        assertEquals(-3, properties2.optInt("#zone_offset"));
        checkPresetEventProperties(properties2);

        //在track 中设置时间和时区，发送track事件，验证内部创建的事件对象  Europe/London  0
        TDTracker.getInstance().track("testTimeZone0", null, new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).parse("2016-01-05 18:33:40.000"), TimeZone.getTimeZone("Europe/London"));
        //check
        JSONObject jsonObject3 = getEvent();
        assertEquals("testTimeZone0", jsonObject3.optString("#event_name"));
        assertEquals("2016-01-05 10:33:40.000", jsonObject3.optString("#time"));
        JSONObject properties3 = jsonObject3.optJSONObject("properties");
        assertEquals(0, properties3.optInt("#zone_offset"));
        checkPresetEventProperties(properties3);

        testProperties.setResult(true);
        Log.d(TAG, "Test_11014 -> 验证时区偏移的正确性 <-");
    }


    @Test
    public void Test_11015() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_11015 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11015");
        testProperties.setName("验证暂停、开启上报的正确性");
        testProperties.setStep("step1:调用暂停方法enableTracking(false)，发送track事件，再调用flush方法，验证暂停状态的持久化、内部事件对象的创建;step2:调用暂停方法enableTracking(false)，发送track事件，再调用flush方法，验证暂停状态的持久化、内部事件对象的创建");
        testProperties.setExcept("step1:调用了存储暂停状态的持久化方法；不会调用入库方法;step2:调用了存储暂停状态的持久化方法，入参是true； 调用事件对象创建方法；调用入库方法；调用了网络请求方法");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
        //init data
        TDTracker.getInstance().identify("test_identify");
        TDTracker.getInstance().login("test_account");
        JSONObject superProp = new JSONObject();
        superProp.put("superKey1", "superValue");
        TDTracker.getInstance().setSuperProperties(superProp);
        //enableTracking(false)，发送track事件
        TDTracker.getInstance().enableTracking(false);
        TDTracker.getInstance().track("testTrackingFalse");
        //check
        assertNull(getEvent());
        assertEquals("test_identify", TDTracker.getInstance().getDistinctId());
        assertEquals("test_account", TDTracker.getInstance().getLoginId());
        assertEquals("superValue", TDTracker.getInstance().getSuperProperties().optString("superKey1"));
        //check 持久化
        SharedPreferences sharedPreferences = mAppContext.getSharedPreferences("com.thinkingdata.analyse_" + TDTracker.getInstance().mConfig.getName(), Context.MODE_PRIVATE);
        assertEquals("test_account", sharedPreferences.getString("loginID", "unknown"));
        assertEquals("test_identify", sharedPreferences.getString("identifyID", "unknown"));
        assertFalse(sharedPreferences.getBoolean("enableFlag", true));
        assertTrue(new JSONObject(sharedPreferences.getString("superProperties", "")).has("superKey1"));

        //enableTracking(true)，发送track事件
        TDTracker.getInstance().enableTracking(true);
        TDTracker.getInstance().track("testTrackingTrue");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("testTrackingTrue", jsonObject.optString("#event_name"));
        assertEquals("test_identify", TDTracker.getInstance().getDistinctId());
        assertEquals("test_account", TDTracker.getInstance().getLoginId());
        assertEquals("superValue", TDTracker.getInstance().getSuperProperties().optString("superKey1"));
        //check 持久化
        assertTrue(sharedPreferences.getBoolean("enableFlag", false));
        testProperties.setResult(true);
        Log.d(TAG, "Test_11015 -> 验证暂停、开启上报的正确性 <-");
    }

    @Test
    public void Test_11016() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_11016 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11016");
        testProperties.setName("验证停止上报、允许上报的正确性");
        testProperties.setStep("step1:调用optOutTracking，发送track事件，再调用flush方法，验证暂停状态的持久化、事件对象入库情况;step2:调用optInTracking，发送track事件，再调用flush方法，验证暂停状态的持久化、事件对象入库情况");
        testProperties.setExcept("step1:调用停止上报状态的持久化方法，不会调用入库方法；不会调用网络请求方法;step2:调用停止上报状态的持久化方法，会调用入库方法；会调用网络请求方法");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
        //init data
        TDTracker.getInstance().identify("test_identify");
        TDTracker.getInstance().login("test_account");
        JSONObject superProp = new JSONObject();
        superProp.put("superKey1", "superValue");
        TDTracker.getInstance().setSuperProperties(superProp);
        //optOutTracking()，发送track事件
        TDTracker.getInstance().optOutTracking();
        TDTracker.getInstance().track("testOptOutTracking");
        //check
        assertNull(getEvent());
        assertNotEquals("test_identify", TDTracker.getInstance().getDistinctId());
        assertNull(TDTracker.getInstance().getLoginId());
        assertFalse(TDTracker.getInstance().getSuperProperties().has("superKey1"));
        //check 持久化
        SharedPreferences sharedPreferences = mAppContext.getSharedPreferences("com.thinkingdata.analyse_" + TDTracker.getInstance().mConfig.getName(), Context.MODE_PRIVATE);
        assertEquals("unknown", sharedPreferences.getString("loginID", "unknown"));
        assertEquals("unknown", sharedPreferences.getString("identifyID", "unknown"));
        assertTrue(sharedPreferences.getBoolean("optOutFlag", false));
        assertFalse(new JSONObject(sharedPreferences.getString("superProperties", "")).has("superKey1"));
        //optInTracking()，发送track事件
        TDTracker.getInstance().optInTracking();
        TDTracker.getInstance().track("testOptInTracking");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("testOptInTracking", jsonObject.optString("#event_name"));
        //check 持久化
        assertFalse(sharedPreferences.getBoolean("optOutFlag", true));

        testProperties.setResult(true);
        Log.d(TAG, "Test_11016 -> 验证停止、重启上报的正确性 <-");
    }


    @Test
    public void Test_11017() throws InterruptedException, ParseException, JSONException {
        Log.d(TAG, "Test_11017 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11017");
        testProperties.setName("验证时间校准的正确性-时间戳");
        testProperties.setStep("step1:调用calibrateTime方法后立马调用track方法，验证事件对象的#time");
        testProperties.setExcept("step1:#time对应的时间戳和校准输入的事件戳误差应小于50");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
        long timestamp = 1554687000000L;
        ThinkingAnalyticsSDK.calibrateTime(timestamp);
        assertTime(TDTracker.getInstance(), messages, timestamp);
        testProperties.setResult(true);
        Log.d(TAG, "Test_11017 -> 验证时间校准的准确性 时间戳 <-");
    }

    @Test
    public void Test_11018() {
        Log.d(TAG, "Test_11018 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_11018");
        testProperties.setName("验证时间校准的正确性-ntp");
        testProperties.setStep("step1:传入ntp server，验证时间戳的设置情况");
        testProperties.setExcept("step1:调用开启NTP服务的方法");
        ThinkingAnalyticsSDK.calibrateTimeWithNtp("time.apple.com");
        testProperties.setResult(true);
        Log.d(TAG, "Test_11018 -> 验证时间校准的准确性 NTP <-");
    }

    @Test
    public void Test_12000() throws JSONException, InterruptedException, ParseException {
        Log.d(TAG, "Test_12000 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_12000");
        testProperties.setName("验证设置用户属性的正确性");
        testProperties.setStep("step1:传入用户属性，验证内部创建的事件对象;step2:传入用户属性和时间，验证内部创建的事件对象（默认时区）;step3:传入用户属性和时间，验证内部创建的事件对象（自定义时区）");
        testProperties.setExcept("step1:事件对象中包含userkey1、userkey2、userkey3的键值对; #type为user_set；属性中不包含预置属性、静态公共属性、动态公共属性;step2:事件对象的#time为 2016-01-06 09:33:40.000;step3:事件对象的#time : 2016-01-05 17:33:40.000");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
        //set 静态公共属性
        JSONObject superProp = new JSONObject();
        superProp.put("superKey1", "superValue1");
        TDTracker.getInstance().setSuperProperties(superProp);
        //set 动态公共属性
        TDTracker.getInstance().setDynamicSuperPropertiesTracker(() -> {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("dynamic_key", "dynamic_value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        });
        // 设置用户属性
        JSONObject prop = new JSONObject();
        prop.put("userKey1", "userValue1");
        prop.put("userKey2", "userValue2");
        prop.put("userKey3", "userValue3");
        TDTracker.getInstance().user_set(prop);
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("user_set", jsonObject.optString("#type"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("userValue1", properties.optString("userKey1"));
        assertEquals("userValue2", properties.optString("userKey2"));
        assertEquals("userValue3", properties.optString("userKey3"));
        //check 预置属性
        assertFalse(properties.has("#lib_version"));
        assertFalse(properties.has("#lib"));
        assertFalse(properties.has("#os"));
        assertFalse(properties.has("#zone_offset"));
        assertFalse(properties.has("#network_type"));
        //check 静态公共属性
        assertFalse(properties.has("superKey1"));
        //check 动态公共属性
        assertFalse(properties.has("dynamic_key"));

        //设置属性 + 时间
        JSONObject prop1 = new JSONObject();
        prop1.put("userKey4", "userValue4");
        TDTracker.getInstance().user_set(prop1, new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).parse("2016-01-05 18:33:40.000"));
        //check
        JSONObject jsonObject1 = getEvent();
        assertEquals("user_set", jsonObject1.optString("#type"));
        assertEquals("2016-01-05 18:33:40.000", jsonObject1.optString("#time"));

        //设置属性 + 时间 + 时区
        JSONObject prop2 = new JSONObject();
        prop2.put("userKey5", "userValue5");
        TDTracker.getInstance().mConfig.setDefaultTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
        TDTracker.getInstance().user_set(prop2, new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).parse("2016-01-05 18:33:40.000"));
        //check
        JSONObject jsonObject2 = getEvent();
        assertEquals("user_set", jsonObject2.optString("#type"));
        assertEquals("2016-01-05 08:33:40.000", jsonObject2.optString("#time"));
        testProperties.setResult(true);
        Log.d(TAG, "Test_12000 -> 验证设置用户属性的正确性 <-");
    }

    @Test
    public void Test_12001() throws InterruptedException {
        Log.d(TAG, "Test_12001 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_12001");
        testProperties.setName("证重置用户属性的正确性");
        testProperties.setStep("step1:传入用户属性字符串，验证内部创建的事件对象;step2:传入用户属性jsonObject,时区为null，验证内部创建的事件对象");
        testProperties.setExcept("step1:事件对象包含属性userkey1，其值为0; #type为user_unset;step2:事件对象包含属性userkey1，其值为uservalue2; #type为user_unset");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
        // 重置用户属性
        TDTracker.getInstance().user_unset("userKey1");
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("user_unset", jsonObject.optString("#type"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals(0, properties.optInt("userKey1"));
        testProperties.setResult(true);
        Log.d(TAG, "Test_12001 -> 验证重置用户属性的正确性 <-");
    }

    @Test
    public void Test_12002() throws JSONException, InterruptedException, ParseException {
        Log.d(TAG, "Test_12002 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_12002");
        testProperties.setName("验证设置单次用户属性的正确性");
        testProperties.setStep("step1:传入单次用户属性，验证内部创建的事件对象;step2:传入单次用户属性和时间，验证内部创建的事件对象（默认时区）;step3:传入单次用户属性和时间，验证内部创建的事件对象（自定义时区）");
        testProperties.setExcept("step1:事件对象中包含onceEventKey1、onceEventKey2、onceEventKey3的键值对; #type为user_setOnce; 事件对象中不包含预制属性的;step2:事件对象的#time为 2016-01-05 18:33:40.000 ;step3:事件对象的#time 2016-01-05 08:33:40.000");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
        //set 静态公共属性
        JSONObject superProp = new JSONObject();
        superProp.put("superKey1", "superValue1");
        TDTracker.getInstance().setSuperProperties(superProp);
        //set 动态公共属性
        TDTracker.getInstance().setDynamicSuperPropertiesTracker(() -> {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("dynamic_key", "dynamic_value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        });
        // 设置用户属性
        JSONObject prop = new JSONObject();
        prop.put("onceEventKey1", "onceEventValue1");
        prop.put("onceEventKey2", "onceEventValue2");
        prop.put("onceEventKey3", "onceEventValue3");
        TDTracker.getInstance().user_setOnce(prop);
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("user_setOnce", jsonObject.optString("#type"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("onceEventValue1", properties.optString("onceEventKey1"));
        assertEquals("onceEventValue2", properties.optString("onceEventKey2"));
        assertEquals("onceEventValue3", properties.optString("onceEventKey3"));
        //check 预置属性
        assertFalse(properties.has("#lib_version"));
        assertFalse(properties.has("#lib"));
        assertFalse(properties.has("#os"));
        assertFalse(properties.has("#zone_offset"));
        assertFalse(properties.has("#network_type"));
        //check 静态公共属性
        assertFalse(properties.has("superKey1"));
        //check 动态公共属性
        assertFalse(properties.has("dynamic_key"));

        //设置属性 + 时间
        JSONObject prop1 = new JSONObject();
        prop1.put("onceEventKey4", "onceEventValue4");
        TDTracker.getInstance().user_setOnce(prop1, new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).parse("2016-01-05 18:33:40.000"));
        //check
        JSONObject jsonObject1 = getEvent();
        assertEquals("user_setOnce", jsonObject1.optString("#type"));
        assertEquals("2016-01-05 18:33:40.000", jsonObject1.optString("#time"));

        //设置属性 + 时间 + 时区
        JSONObject prop2 = new JSONObject();
        prop2.put("onceEventKey5", "onceEventValue5");
        TDTracker.getInstance().mConfig.setDefaultTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
        TDTracker.getInstance().user_setOnce(prop2, new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).parse("2016-01-05 18:33:40.000"));
        //check
        JSONObject jsonObject2 = getEvent();
        assertEquals("user_setOnce", jsonObject2.optString("#type"));
        assertEquals("2016-01-05 08:33:40.000", jsonObject2.optString("#time"));
        testProperties.setResult(true);
        Log.d(TAG, "Test_12002 -> 验证设置一次用户属性的正确性 <-");
    }


    @Test
    public void Test_12003() throws JSONException, InterruptedException, ParseException {
        Log.d(TAG, "Test_12003 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_12003");
        testProperties.setName("验证对数值类型用户属性进行累加操作的正确性");
        testProperties.setStep("step1:传入标准的数值类型用户属性，验证内部创建的事件对象;step2:传入时间，验证内部创建的事件对象（默认时区）;step3:传入时间，验证内部创建的事件对象（自定义时区）");
        testProperties.setExcept("step1:事件对象中包含useradd1、useradd2、useradd3的键值对; #type为user_add; 事件对象中不包含预制属性，静态公共属性，动态公共属性;step2:事件对象的#time为 2016-01-05 18:33:40.000;step3:事件对象的#time 为 2016-01-05 08:33:40.000");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
        //set 静态公共属性
        JSONObject superProp = new JSONObject();
        superProp.put("superKey1", "superValue1");
        TDTracker.getInstance().setSuperProperties(superProp);
        //set 动态公共属性
        TDTracker.getInstance().setDynamicSuperPropertiesTracker(() -> {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("dynamic_key", "dynamic_value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        });
        // 设置用户属性
        JSONObject prop = new JSONObject();
        prop.put("useradd1", 1);
        prop.put("useradd2", -10);
        prop.put("useradd3", 100);
        TDTracker.getInstance().user_add(prop);
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("user_add", jsonObject.optString("#type"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals(1, properties.optInt("useradd1"));
        assertEquals(-10, properties.optInt("useradd2"));
        assertEquals(100, properties.optInt("useradd3"));
        //check 预置属性
        assertFalse(properties.has("#lib_version"));
        assertFalse(properties.has("#lib"));
        assertFalse(properties.has("#os"));
        assertFalse(properties.has("#zone_offset"));
        assertFalse(properties.has("#network_type"));
        //check 静态公共属性
        assertFalse(properties.has("superKey1"));
        //check 动态公共属性
        assertFalse(properties.has("dynamic_key"));

        //设置属性 + 时间
        JSONObject prop1 = new JSONObject();
        prop1.put("useradd4", 90);
        TDTracker.getInstance().user_add(prop1, new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).parse("2016-01-05 18:33:40.000"));
        //check
        JSONObject jsonObject1 = getEvent();
        assertEquals("user_add", jsonObject1.optString("#type"));
        assertEquals("2016-01-05 18:33:40.000", jsonObject1.optString("#time"));

        //设置属性 + 时间 + 时区
        JSONObject prop2 = new JSONObject();
        prop2.put("useradd5", 11);
        TDTracker.getInstance().mConfig.setDefaultTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
        TDTracker.getInstance().user_add(prop2, new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).parse("2016-01-05 18:33:40.000"));
        //check
        JSONObject jsonObject2 = getEvent();
        assertEquals("user_add", jsonObject2.optString("#type"));
        assertEquals("2016-01-05 08:33:40.000", jsonObject2.optString("#time"));
        testProperties.setResult(true);
        Log.d(TAG, "Test_12003 -> 验证数值属性累加的正确性 <-");
    }

    @Test
    public void Test_12004() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_12004 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_12004");
        testProperties.setName("验证删除用户的正确性");
        testProperties.setStep("step1:传入API，验证内部创建的事件对象");
        testProperties.setExcept("step1:#type为user_del; 事件对象中不包含预制属性的");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
        //set 静态公共属性
        JSONObject superProp = new JSONObject();
        superProp.put("superKey1", "superValue1");
        TDTracker.getInstance().setSuperProperties(superProp);
        //set 动态公共属性
        TDTracker.getInstance().setDynamicSuperPropertiesTracker(() -> {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("dynamic_key", "dynamic_value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        });
        // 删除用户属性
        TDTracker.getInstance().user_delete();
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("user_del", jsonObject.optString("#type"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        //check 预置属性
        assertFalse(properties.has("#lib_version"));
        assertFalse(properties.has("#lib"));
        assertFalse(properties.has("#os"));
        assertFalse(properties.has("#zone_offset"));
        assertFalse(properties.has("#network_type"));
        //check 静态公共属性
        assertFalse(properties.has("superKey1"));
        //check 动态公共属性
        assertFalse(properties.has("dynamic_key"));
        testProperties.setResult(true);
        Log.d(TAG, "Test_12004 -> 验证删除用户的正确性 <-");
    }

    @Test
    public void Test_12005() throws InterruptedException, JSONException {
        Log.d(TAG, "Test_12005 ->");
        testProperties.setResult(false);
        testProperties.setId("Test_12005");
        testProperties.setName("验证对 Array 类型的用户属性进行追加操作的正确性");
        testProperties.setStep("step1:传入属性，验证内部创建的事件对象");
        testProperties.setExcept("step1:事件对象中包含user_append1、user_append2的键值对; #type为user_append; 事件对象中不包含预制属性的");
        TestUtils.clearData(mAppContext);
        initThinkingDataSDK();
        //set 静态公共属性
        JSONObject superProp = new JSONObject();
        superProp.put("superKey1", "superValue1");
        TDTracker.getInstance().setSuperProperties(superProp);
        //set 动态公共属性
        TDTracker.getInstance().setDynamicSuperPropertiesTracker(() -> {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("dynamic_key", "dynamic_value");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject;
        });
        JSONObject prop = new JSONObject();
        prop.put("user_append1", "user_append_value1");
        prop.put("user_append2", "user_append_value2");
        prop.put("user_append3", "user_append_value3");
        //追加
        TDTracker.getInstance().user_append(prop);
        //check
        JSONObject jsonObject = getEvent();
        assertEquals("user_append", jsonObject.optString("#type"));
        JSONObject properties = jsonObject.optJSONObject("properties");
        assertEquals("user_append_value1", properties.optString("user_append1"));
        assertEquals("user_append_value2", properties.optString("user_append2"));
        assertEquals("user_append_value3", properties.optString("user_append3"));
        //check 预置属性
        assertFalse(properties.has("#lib_version"));
        assertFalse(properties.has("#lib"));
        assertFalse(properties.has("#os"));
        assertFalse(properties.has("#zone_offset"));
        assertFalse(properties.has("#network_type"));
        //check 静态公共属性
        assertFalse(properties.has("superKey1"));
        //check 动态公共属性
        assertFalse(properties.has("dynamic_key"));
        testProperties.setResult(true);
        Log.d(TAG, "Test_12005 -> 验证对array类型属性追加的正确性 <-");
    }


   /* @Test
    public void testAnnotationEvent() throws InterruptedException, JSONException {
        TestRunner.getInstance().enableAutoTrack(null);
        TestRunner.getDebugInstance().enableAutoTrack(null);

        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.button_logout)).perform(click());

        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "log_out");
        assertEquals(event.getString("#distinct_id"), TestRunner.getDebugInstance().getDistinctId());
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("paramString"), "value");
        assertEquals(properties.getInt("paramNumber"), 123);
        assertTrue(properties.getBoolean("paramBoolean"));

        event = TestRunner.getEvent();
        assertNull(event);
    }

    @Test
    public void testStartEndEvents() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_start");
        JSONObject properties = event.getJSONObject("properties");
        assertFalse(properties.getBoolean("#resume_from_background"));
        assertEquals("cn.thinkingdata.android.demo.MainActivity", properties.getString("#screen_name"));
        assertEquals(ApplicationProvider.getApplicationContext().getString(R.string.app_name), properties.getString("#title"));
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_view");
        properties = event.getJSONObject("properties");
        assertEquals("cn.thinkingdata.android.demo.MainActivity", properties.getString("#screen_name"));
        assertEquals(ApplicationProvider.getApplicationContext().getString(R.string.app_name), properties.getString("#title"));

        scenario.moveToState(Lifecycle.State.DESTROYED);

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_end");
        properties = event.getJSONObject("properties");
        assertEquals("cn.thinkingdata.android.demo.MainActivity", properties.getString("#screen_name"));
        assertEquals(ApplicationProvider.getApplicationContext().getString(R.string.app_name), properties.getString("#title"));
        assertTrue(properties.has("#duration"));
    }

    @Test
    public void testFragment() throws InterruptedException, JSONException {
        TestRunner.getDebugInstance().trackFragmentAppViewScreen();
        ActivityScenario.launch(DisplayActivity.class);
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_view");
        assertEquals(event.getString("#distinct_id"), TestRunner.getDebugInstance().getDistinctId());
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.RecyclerViewFragment");

        onView(withId(R.id.navigation_expandable)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_view");
        assertEquals(event.getString("#distinct_id"), TestRunner.getDebugInstance().getDistinctId());
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ExpandableListFragment");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.ExpandableListFragment");

        TestRunner.getInstance().trackFragmentAppViewScreen();
        TestRunner.getInstance().identify("test_fragment_id_instance");
        onView(withId(R.id.navigation_list)).perform(click());
        for (int i = 0; i < 2; i++) {
            event = TestRunner.getEvent();
            assertEquals(event.getString("#event_name"), "ta_app_view");
            properties = event.getJSONObject("properties");
            if (event.getString("#distinct_id").equals(TestRunner.getDebugInstance().getDistinctId())) {
                assertEquals(properties.getString("#title"), "ListViewFragment");
            } else {
                assertEquals(properties.getString("#title"), "DisplayActivity");
            }
            assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.ListViewFragment");
        }
    }

    @Test
    public void testWebView() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario.launch(WebViewActivity.class);

        onWebView().withElement(findElement(Locator.ID, "test_button")).perform(webClick());
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_pageview");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#type"), "user_set");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#type"), "user_add");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "Purchase");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "Test");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "click");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("name"), "元素标识名");
        assertEquals(properties.getString("production"), "产品名");

        onView(withId(R.id.fab)).perform(click());
        event = TestRunner.getEvent();
        assertNull(event);
    }

    @Test
    public void testListItemClick() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario.launch(DisplayActivity.class);

        onView(withText("模拟数据 2")).perform(click());
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#element_content"), "模拟数据 2-1002");
        // TODO
        assertEquals(properties.getString("#element_type"), "android.widget.LinearLayout");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.RecyclerViewFragment");
        assertNotNull(properties.getString("#element_selector"));
        // TODO
        //assertNotNull(properties.getString("#element_id"));

        onView(withId(R.id.navigation_expandable)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#element_content"), "Expandable");
        assertEquals(properties.getString("#element_id"), "navigation_expandable");
        // TODO
        assertEquals(properties.getString("#element_type"), "com.google.android.material.bottomnavigation.BottomNavigationItemView");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity");
        assertNotNull(properties.getString("#element_selector"));

        onView(withText("西游记")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        // TODO fragment title?
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#element_content"), "西游记");
        assertEquals(properties.getString("#element_id"), "expandable_list");
        assertEquals(properties.getString("#element_type"), "ExpandableListView");
        assertEquals(properties.getString("#element_position"), "0");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.ExpandableListFragment");
        assertNotNull(properties.getString("#element_selector"));

        onView(withText("孙悟空")).perform(click());
        event = TestRunner.getEvent();
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#element_content"), "孙悟空");
        assertEquals(properties.getString("#element_position"), "0:1");
        assertEquals(properties.getString("#element_id"), "expandable_list");
        assertEquals(properties.getString("#element_type"), "ExpandableListView");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.ExpandableListFragment");
        assertNotNull(properties.getString("#element_selector"));
    }

    @Test
    public void testAlertDialog() throws InterruptedException, JSONException {
        // TODO Dialog 与 Activity 绑定
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario.launch(MainActivity.class);

        onView(withId(R.id.button_login)).perform(click());
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ThinkingDataDemo");
        assertEquals(properties.getString("#element_content"), "Login");
        assertEquals(properties.getString("#element_type"), "Button");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.MainActivity");
        assertEquals(properties.getString("#element_id"), "button_login");
        assertNotNull(properties.getString("#element_selector"));


        onView(withText(R.string.ok)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ThinkingDataDemo");
        assertEquals(properties.getString("#element_content"), "OK");
        assertEquals(properties.getString("#element_type"), "Button");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.MainActivity");
        assertNotNull(properties.getString("#element_id"));
        assertNotNull(properties.getString("#element_selector"));

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ThinkingDataDemo");
        assertEquals(properties.getString("#element_content"), "OK");
        assertEquals(properties.getString("#element_type"), "Dialog");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.MainActivity");
        assertEquals(properties.getString("#element_id"), "test_id");
        assertFalse(properties.has("#element_selector"));
    }

    @Test
    public void testClickEvents() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario.launch(ClickTestActivity.class);

        onView(withId(R.id.checkbox_button)).perform(click());
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        JSONObject properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "checkbox_button");
        assertEquals(properties.getString("#element_content"), "MyCheckBox");
        assertEquals(properties.getString("#element_type"), "CheckBox");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        onView(withId(R.id.radio_button2)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "radio_group");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "MyRadioButton2");
        assertEquals(properties.getString("#element_type"), "RadioGroup");
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "radio_button2");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "MyRadioButton2");
        assertEquals(properties.getString("#element_type"), "RadioButton");

        onView(withId(R.id.switch_button)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "switch_button");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "ON");
        assertEquals(properties.getString("#element_type"), "SwitchButton");

        onView(withId(R.id.toggle_button)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "toggle_button");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "ON");
        assertEquals(properties.getString("#element_type"), "ToggleButton");

        onView(withText("Widgets")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "TabHost");
        assertEquals(properties.getString("#element_content"), "tab2");

        onView(withId(R.id.seekBar)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "seekBar");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertTrue(properties.getInt("#element_content") > 0);
        assertEquals(properties.getString("#element_type"), "SeekBar");

        onView(withId(R.id.ratingBar)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "ratingBar");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "3.0");
        assertEquals(properties.getString("#element_type"), "RatingBar");

        onView(withId(R.id.imageView)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "imageView");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "ImageView");

        onView(withText("Others")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "TabHost");
        assertEquals(properties.getString("#element_content"), "tab3");

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "spinner");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "Spinner");
        assertEquals(properties.getString("#element_position"), "0");
        assertEquals(properties.getString("#element_content"), "Android");

        onView(withId(R.id.button_show_timepicker)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "button_show_timepicker");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "Button");
        assertEquals(properties.getString("#element_content"), "Select Time");

        onView(withText("OK")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "timePicker");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "TimePicker");
        assertTrue(properties.has("#element_content"));

        onView(withId(R.id.button_show_datepicker)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "button_show_datepicker");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "Button");
        assertEquals(properties.getString("#element_content"), "Show Date Picker");

        onView(withText("OK")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "datePicker");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "DatePicker");
        assertTrue(properties.has("#element_content"));

        openContextualActionModeOverflowMenu();
        onView(withText("menu2")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_id"), "menu_item2");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "MenuItem");
        assertEquals(properties.getString("#element_content"), "menu2");

        // TODO remove redundant events
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "ListView");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_content"), "menu2");
        assertEquals(properties.getString("#element_position"), "1");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        onView(withId(R.id.button_multi_choice_dialog)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "Button");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "button_multi_choice_dialog");
        assertEquals(properties.getString("#element_content"), "Test MultiChoice Dialog");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        // TODO remove redundant events
        onView(withText("java")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "Dialog");
        assertEquals(properties.getString("#element_content"), "java");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "ListView");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_position"), "1");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        onView(withText("OK")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "Button");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "button1");
        assertEquals(properties.getString("#element_content"), "OK");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "Dialog");
        assertEquals(properties.getString("#element_content"), "OK");

    }*/
}
