package cn.thinkingdata.android;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import cn.thinkingdata.android.utils.RemoteService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FlushStrategyTest {
    private static final String TA_APP_ID = "b2a61feb9e56472c90c5bcb320dfb4ef";
    private static final String TA_APP_ID_DEBUG = "debug-appid";
    private static final String TA_SERVER_URL = "https://sdk.tga.thinkinggame.cn";
    private static final Double DELTA =  0.0000;

    private static final int POLL_WAIT_SECONDS = 2;
    private static final int FLUSH_INTERVAL = 5000;
    private static final int FLUSH_BULK_SIZE = 40;

    private static Context mAppContext;
    private final static String mVersionName = "1.0";
    private static TDConfig mConfig;
    private SystemInformation mSystemInformation;

    @Before
    public void setUp() {
        ThinkingAnalyticsSDK.enableTrackLog(true);
        mAppContext = ApplicationProvider.getApplicationContext();

        mSystemInformation = SystemInformation.getInstance(mAppContext);

        mConfig = TDConfig.getInstance(mAppContext, TA_APP_ID, TA_SERVER_URL);

    }

    private void assertAutomaticData(JSONObject automaticData) throws JSONException {
        assertEquals(automaticData.getString("#lib_version"), TDConfig.VERSION);
        assertEquals(automaticData.getString("#lib"), "Android");
        assertEquals(automaticData.getString("#os"), "Android");
        Map<String, Object> deviceInfo = mSystemInformation.getDeviceInfo();
        assertEquals(automaticData.getString("#device_id"), deviceInfo.get("#device_id"));
        assertEquals(automaticData.getString("#carrier"), deviceInfo.get("#carrier"));
        assertEquals(automaticData.getString("#manufacturer"), deviceInfo.get("#manufacturer"));
        assertEquals(automaticData.getString("#device_model"), deviceInfo.get("#device_model"));
        assertEquals(automaticData.getInt("#screen_height"), deviceInfo.get("#screen_height"));
        assertEquals(automaticData.getInt("#screen_width"), deviceInfo.get("#screen_width"));
    }

    @Test
    public void testFlushInterval() throws InterruptedException, JSONException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {

                    @Override
                    protected int getFlushInterval(String token) {
                        return FLUSH_INTERVAL;
                    }

                    @Override
                    protected RemoteService getPoster() {
                        return new RemoteService() {
                            @Override
                            public String performRequest(String endpointUrl, String params, boolean debug, SSLSocketFactory socketFactory) throws IOException, ServiceUnavailableException {
                                try {
                                    JSONObject jsonObject = new JSONObject(params);
                                    messages.add(jsonObject);
                                } catch (JSONException e) {

                                }
                                return "{code:0}";
                            }
                        };

                    }
                };
            }
        };

        // test Flush interval
        for(int i = 0; i < 10; i++) {
            instance.track("test_flush" + i);
        }

        Thread.sleep(FLUSH_INTERVAL);
        JSONObject data = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(data.length(), 3);
        assertEquals(data.getString("#app_id"), TA_APP_ID);
        assertAutomaticData(data.getJSONObject("automaticData"));

        JSONArray events = data.getJSONArray("data");
        assertEquals(events.length(), 10);
        for (int i = 0; i < events.length(); i++) {
            assertEquals(events.getJSONObject(i).getString("#event_name"), "test_flush" + i);
        }

        Thread.sleep(1000);
        for(int i = 0; i < 5; i++) {
            instance.track("test_flush_2_" + i);
        }
        Thread.sleep(FLUSH_INTERVAL);
        data = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(data.length(), 3);
        assertEquals(data.getString("#app_id"), TA_APP_ID);
        assertAutomaticData(data.getJSONObject("automaticData"));
        events = data.getJSONArray("data");
        assertEquals(events.length(), 5);
        for (int i = 0; i < events.length(); i++) {
            assertEquals(events.getJSONObject(i).getString("#event_name"), "test_flush_2_" + i);
        }

        instance.flush();
        assertNull(messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testFlushBulkSize() throws InterruptedException, JSONException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected int getFlushBulkSize(String token) {
                        return FLUSH_BULK_SIZE;
                    }

                    @Override
                    protected int getFlushInterval(String token) {
                        return 100*1000;
                    }

                    @Override
                    protected RemoteService getPoster() {
                        return new RemoteService() {
                            @Override
                            public String performRequest(String endpointUrl, String params, boolean debug, SSLSocketFactory socketFactory) throws IOException, ServiceUnavailableException {
                                try {
                                    JSONObject jsonObject = new JSONObject(params);
                                    messages.add(jsonObject);
                                } catch (JSONException e) {

                                }
                                return "{code:0}";
                            }
                        };

                    }
                };
            }
        };

        // test Flush bulk size
        for(int i = 0; i < FLUSH_BULK_SIZE; i++) {
            instance.track("test_flush_bulk" + i);
        }

        JSONObject data1 = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(data1.length(), 3);
        assertAutomaticData(data1.getJSONObject("automaticData"));
        assertEquals(data1.getString("#app_id"), TA_APP_ID);
        JSONArray events = data1.getJSONArray("data");
        assertEquals(events.length(), 40);
        for (int i = 0; i < events.length() - 1; i++) {
            assertEquals(events.getJSONObject(i).getString("#event_name"), "test_flush_bulk" + i);
        }
        instance.flush();
        assertNull(messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testFlushIntervalMultiAppId() throws InterruptedException, JSONException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        final DataHandle dataHandle = new DataHandle(mAppContext) {
            @Override
            protected int getFlushInterval(String token) {
                return FLUSH_INTERVAL;
            }

            @Override
            protected RemoteService getPoster() {
                return new RemoteService() {
                    @Override
                    public String performRequest(String endpointUrl, String params, boolean debug, SSLSocketFactory socketFactory) throws IOException, ServiceUnavailableException {
                        try {
                            JSONObject jsonObject = new JSONObject(params);
                            messages.add(jsonObject);
                        } catch (JSONException e) {

                        }
                        return "{code:0}";
                    }
                };
            }

        };
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }
        };

        ThinkingAnalyticsSDK instance_debug = new ThinkingAnalyticsSDK(TDConfig.getInstance(mAppContext, TA_APP_ID_DEBUG, TA_SERVER_URL)) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }
        };

        instance.track("test_instance");
        instance_debug.track("test_instance_debug");
        for (int i = 0; i < 5; i++) {
            instance.track("test_instance_" + i);
            instance.user_add("key" + i, i);
        }
        for (int i = 0; i < 5; i++) {
            instance_debug.track("test_instance_debug_" + i);
        }
        Thread.sleep(FLUSH_INTERVAL);
        JSONObject data = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(data.length(), 3);
        assertEquals(data.getString("#app_id"), TA_APP_ID);
        assertAutomaticData(data.getJSONObject("automaticData"));
        JSONArray events = data.getJSONArray("data");
        assertEquals(events.length(), 11);
        assertEquals(events.getJSONObject(0).getString("#event_name"), "test_instance");
        for (int i = 1; i < events.length(); i++) {
            assertEquals(events.getJSONObject(i).getString("#event_name"), "test_instance_" + (i / 2));
            assertEquals(events.getJSONObject(++i).getString("#type"), "user_add");
        }

        data = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(data.length(), 3);
        assertEquals(data.getString("#app_id"), TA_APP_ID_DEBUG);
        assertAutomaticData(data.getJSONObject("automaticData"));
        events = data.getJSONArray("data");
        assertEquals(events.length(), 6);
        assertEquals(events.getJSONObject(0).getString("#event_name"), "test_instance_debug");
        for (int i = 1; i < events.length(); i++) {
            assertEquals(events.getJSONObject(i).getString("#event_name"), "test_instance_debug_" + (i - 1));
        }
        instance.flush();
        instance_debug.flush();
        assertNull(messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testFlushBulkSizeMultiAppId() throws InterruptedException, JSONException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        final DataHandle dataHandle = new DataHandle(mAppContext) {
            @Override
            protected int getFlushBulkSize(String token) {
                return FLUSH_BULK_SIZE;
            }

            @Override
            protected int getFlushInterval(String token) {
                return 100 * 1000;
            }

            @Override
            protected RemoteService getPoster() {
                return new RemoteService() {
                    @Override
                    public String performRequest(String endpointUrl, String params, boolean debug, SSLSocketFactory socketFactory) throws IOException, ServiceUnavailableException {
                        try {
                            JSONObject jsonObject = new JSONObject(params);
                            messages.add(jsonObject);
                        } catch (JSONException e) {

                        }
                        return "{code:0}";
                    }
                };
            }

        };
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }
        };

        ThinkingAnalyticsSDK instance_debug = new ThinkingAnalyticsSDK(TDConfig.getInstance(mAppContext, TA_APP_ID_DEBUG, TA_SERVER_URL)) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return dataHandle;
            }
        };

        // test Flush bulk size
        for(int i = 0; i < FLUSH_BULK_SIZE - 1; i++) {
            instance.track("test_flush_bulk" + i);
        }

        for(int i = 0; i < FLUSH_BULK_SIZE - 1; i++) {
            instance_debug.track("test_flush_bulk_debug" + i);
        }

        instance_debug.track("test_flush_bulk_debug");
        instance.track("test_flush_bulk");

        JSONObject data = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(data.length(), 3);
        assertAutomaticData(data.getJSONObject("automaticData"));
        assertEquals(data.getString("#app_id"), TA_APP_ID_DEBUG);
        JSONArray events = data.getJSONArray("data");
        assertEquals(events.length(), 40);
        for (int i = 0; i < events.length() - 1; i++) {
            assertEquals(events.getJSONObject(i).getString("#event_name"), "test_flush_bulk_debug" + i);
        }
        assertEquals(events.getJSONObject(events.length() - 1).getString("#event_name"), "test_flush_bulk_debug");

        data = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(data.length(), 3);
        assertAutomaticData(data.getJSONObject("automaticData"));
        assertEquals(data.getString("#app_id"), TA_APP_ID);
        events = data.getJSONArray("data");
        assertEquals(events.length(), 40);
        for (int i = 0; i < events.length() - 1; i++) {
            assertEquals(events.getJSONObject(i).getString("#event_name"), "test_flush_bulk" + i);
        }
        assertEquals(events.getJSONObject(events.length() - 1).getString("#event_name"), "test_flush_bulk");
        instance.flush();
        instance_debug.flush();
        assertNull(messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS));
    }

}
