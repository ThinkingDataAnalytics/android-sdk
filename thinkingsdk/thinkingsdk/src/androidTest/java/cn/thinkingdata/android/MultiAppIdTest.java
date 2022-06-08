package cn.thinkingdata.android;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import cn.thinkingdata.android.utils.TDLog;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MultiAppIdTest {

    private static final String TA_APP_ID = "b2a61feb9e56472c90c5bcb320dfb4ef";
    private static final String TA_APP_ID_DEBUG = "debug-appid";
    private static final String TA_SERVER_URL = "https://sdk.tga.thinkinggame.cn";
    private static final Double DELTA =  0.1;

    private static final int POLL_WAIT_SECONDS = 5;

    private ThinkingAnalyticsSDK mInstance;
    private ThinkingAnalyticsSDK mInstanceDebug;
    private final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();

    @Before
    public void setUp() {
        ThinkingAnalyticsSDK.enableTrackLog(true);
        final Context mAppContext = ApplicationProvider.getApplicationContext();
        final TDConfig mConfig = TDConfig.getInstance(mAppContext, TA_APP_ID, TA_SERVER_URL);
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
        mInstance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }

            @Override
            public ThinkingAnalyticsSDK createLightInstance() {
                return new LightThinkingAnalyticsSDK(mConfig) {
                    @Override
                    protected  DataHandle getDataHandleInstance(Context context) {return dataHandle;}
                };
            }
        };
        mInstanceDebug = new ThinkingAnalyticsSDK(TDConfig.getInstance(mAppContext, TA_APP_ID_DEBUG, TA_SERVER_URL)) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }
        };
    }

    @Test
    public void testDistinctId() throws InterruptedException, JSONException {
        String distinctId = mInstance.getDistinctId();
        String distinctId2 = mInstanceDebug.getDistinctId();
        assertEquals(distinctId, distinctId2);
        mInstance.track("test_instance");
        mInstanceDebug.track("test_instance_debug");
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance");
        assertEquals(event.getString("#distinct_id"), distinctId);

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance_debug");
        assertEquals(event.getString("#distinct_id"), distinctId);

        mInstanceDebug.identify("id_debug");
        assertEquals(mInstanceDebug.getDistinctId(), "id_debug");
        assertNotEquals(mInstance.getDistinctId(), mInstanceDebug.getDistinctId());
        assertEquals(mInstance.getDistinctId(), distinctId);

        mInstance.track("test_instance");
        mInstanceDebug.track("test_instance_debug");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance");
        assertEquals(event.getString("#distinct_id"), distinctId);

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance_debug");
        assertEquals(event.getString("#distinct_id"), "id_debug");
    }

    @Test
    public void testAccountId() throws InterruptedException, JSONException {
        mInstance.login("test_account");
        mInstance.track("test_instance");
        mInstanceDebug.track("test_instance_debug");
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance");
        assertEquals(event.getString("#account_id"), "test_account");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance_debug");
        assertFalse(event.has("#account_id"));

        mInstanceDebug.login("test_account_debug");
        mInstance.track("test_instance");
        mInstanceDebug.track("test_instance_debug");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance");
        assertEquals(event.getString("#account_id"), "test_account");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance_debug");
        assertEquals(event.getString("#account_id"), "test_account_debug");

        mInstance.logout();
        mInstance.track("test_instance");
        mInstanceDebug.track("test_instance_debug");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance");
        assertFalse(event.has("#account_id"));
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance_debug");
        assertEquals(event.getString("#account_id"), "test_account_debug");
    }

    @Test
    public void testSuperProperties() throws InterruptedException, JSONException {
        JSONObject superProperties = new JSONObject();
        superProperties.put("SUPER_VIP", 1);
        superProperties.put("SUPER_CHANNEL", "B1");

        mInstance.setSuperProperties(superProperties);
        mInstance.track("test_instance");
        mInstanceDebug.track("test_instance_debug");
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance");
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("SUPER_CHANNEL"), "B1");
        assertEquals(properties.getInt("SUPER_VIP"), 1);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance_debug");
        properties = event.getJSONObject("properties");
        assertFalse(properties.has("SUPER_VIP"));
        assertFalse(properties.has("SUPER_CHANNEL"));

        JSONObject superProperties2 = new JSONObject();
        superProperties2.put("SUPER_VIP", 2);
        superProperties2.put("SUPER_CHANNEL", "A1");

        mInstanceDebug.setSuperProperties(superProperties2);
        mInstance.track("test_instance");
        mInstanceDebug.track("test_instance_debug");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("SUPER_CHANNEL"), "B1");
        assertEquals(properties.getInt("SUPER_VIP"), 1);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance_debug");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("SUPER_CHANNEL"), "A1");
        assertEquals(properties.getInt("SUPER_VIP"), 2);

        mInstance.clearSuperProperties();
        mInstance.track("test_instance");
        mInstanceDebug.track("test_instance_debug");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance");
        properties = event.getJSONObject("properties");
        assertFalse(properties.has("SUPER_VIP"));
        assertFalse(properties.has("SUPER_CHANNEL"));
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_instance_debug");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("SUPER_CHANNEL"), "A1");
        assertEquals(properties.getInt("SUPER_VIP"), 2);
    }

    @Test
    public void testTimeEvent() throws InterruptedException, JSONException {
        mInstance.timeEvent("test_time_event");
        mInstanceDebug.track("test_time_event");
        mInstance.track("test_time_event");
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertFalse(event.getJSONObject("properties").has("#duration"));
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertTrue(event.getJSONObject("properties").has("#duration"));

        mInstance.timeEvent("test_time_event");
        mInstanceDebug.timeEvent("test_time_event");
        Thread.sleep(1000);
        mInstanceDebug.track("test_time_event");
        Thread.sleep(2000);
        mInstance.track("test_time_event");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getJSONObject("properties").getDouble("#duration"), 1.0, DELTA);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getJSONObject("properties").getDouble("#duration"), 3.0, DELTA);
    }

    @Test
    public void testLightInstance() throws JSONException, InterruptedException {
        ThinkingAnalyticsSDK lightInstance = mInstance.createLightInstance();
        assertEquals(lightInstance.getDistinctId(), mInstance.getDistinctId());
        lightInstance.identify("id_light");
        lightInstance.login("account_light");

        mInstance.identify("id_instance");
        JSONObject superProperties = new JSONObject();
        superProperties.put("SUPER_VIP", 1);
        superProperties.put("SUPER_CHANNEL", "B1");
        mInstance.setSuperProperties(superProperties);

        mInstance.track("test_event_light");
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#distinct_id"), "id_instance");
        assertFalse(event.has("#account_id"));
        assertTrue(event.getJSONObject("properties").has("SUPER_VIP"));
        assertTrue(event.getJSONObject("properties").has("SUPER_CHANNEL"));


        lightInstance.track("test_event_light");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals(event.getString("#distinct_id"), "id_light");
        assertEquals(event.getString("#account_id"), "account_light");
        assertFalse(event.getJSONObject("properties").has("SUPER_VIP"));
        assertFalse(event.getJSONObject("properties").has("SUPER_CHANNEL"));
    }
}
