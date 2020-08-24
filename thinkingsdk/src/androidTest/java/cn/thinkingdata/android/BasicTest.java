package cn.thinkingdata.android;

import android.content.Context;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import cn.thinkingdata.android.utils.RemoteService;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDConstants.DataType;
import cn.thinkingdata.android.utils.TDLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


import javax.net.ssl.SSLSocketFactory;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class BasicTest {
    private static final String TA_APP_ID = "b2a61feb9e56472c90c5bcb320dfb4ef";
    //private static final String TA_APP_ID = "debug-appid";
    private static final String TA_SERVER_URL = "https://sdk.tga.thinkinggame.cn";
    private static final String TAG = "THINKING_TEST";
    private static final Double DELTA = 0.0000;

    private static final int POLL_WAIT_SECONDS = 2;

    private static final int SIZE_OF_EVENT_DATA = 6;
    private static final int SIZE_OF_EVENT_DATA_LOGIN = 7;
    private static final int SIZE_OF_USER_DATA = 5;
    private static final int SIZE_OF_USER_DATA_LOGIN = 6;
    private static final int SIZE_OF_SYSTEM_PROPERTY = 3;

    private static Context mAppContext;
    private final static String mVersionName = "1.0";
    private static TDConfig mConfig;

    @Before
    public void setUp() {
        ThinkingAnalyticsSDK.enableTrackLog(true);
        mAppContext = ApplicationProvider.getApplicationContext();
        mConfig = TDConfig.getInstance(mAppContext, TA_APP_ID, TA_SERVER_URL);
    }


    @Test
    public void useAppContext() {
        // Context of the app under test.
        assertEquals("cn.thinkingdata.analytics", mAppContext.getPackageName());
    }

    @Test
    public void trackBasic() throws InterruptedException, JSONException {
        final BlockingDeque<JSONObject> messages = new LinkedBlockingDeque<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected RemoteService getPoster() {
                        return new RemoteService() {
                            @Override
                            public String performRequest(String endpointUrl, String params, boolean debug, SSLSocketFactory socketFactory, Map<String, String> extraHeaders) throws IOException, ServiceUnavailableException {
                                try {
                                    JSONObject jsonObject = new JSONObject(params);
                                    JSONArray events = new JSONArray(jsonObject.getString("data"));
                                    for (int i = 0; i < events.length(); i++) {
                                        JSONObject event = events.getJSONObject(i);
                                        messages.add(event);
                                    }
                                } catch (JSONException e) {

                                }
                                return "{code:0}";
                            }
                        };

                    }
                };
            }
        };
        for (int i = 0; i < 20; i++) {
            instance.track("test_event");
        }
        try {
            Thread.sleep(3000);
        } catch (Exception e) {

        }
        instance.flush();
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(), SIZE_OF_EVENT_DATA);
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertEquals(event.getString("#event_name"), "test_event");
        assertEquals(event.getJSONObject("properties").getString("#app_version"), mVersionName);
        assertTrue(event.getJSONObject("properties").has("#network_type"));
    }

    @Test
    public void testSuperProperties() throws InterruptedException, JSONException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        // test basic track event
        instance.track("test_event");
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_event");

        // test track event with properties
        JSONObject properties = new JSONObject();

        Date date = new Date();
        properties.put("KEY_STRING", "string value");
        properties.put("KEY_DATE", date);
        properties.put("KEY_INT", 6);
        properties.put("KEY_BOOLEAN", true);

        instance.track("test_event1", properties);

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_event1");
        assertEquals(event.getString("#distinct_id"), instance.getDistinctId());
        assertEquals(event.getString("#type"), "track");
        assertFalse(event.has("#account"));

        JSONObject prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), SIZE_OF_SYSTEM_PROPERTY + 4);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);

        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);
        assertEquals(prop1.getString("KEY_DATE"), sDateFormat.format(date));
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));

        // test track events with super properties
        JSONObject superProperties = new JSONObject();
        Date super_date = new Date();
        superProperties.put("SUPER_KEY_STRING", "super string value");
        superProperties.put("SUPER_KEY_DATE", super_date);
        superProperties.put("SUPER_KEY_INT", 0);
        superProperties.put("SUPER_KEY_BOOLEAN", false);
        instance.setSuperProperties(superProperties);
        instance.track("test_event1", properties);

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), SIZE_OF_SYSTEM_PROPERTY + 8);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), sDateFormat.format(date));
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getString("SUPER_KEY_STRING"), "super string value");
        assertEquals(prop1.getInt("SUPER_KEY_INT"), 0);
        assertEquals(prop1.getString("SUPER_KEY_DATE"), sDateFormat.format(super_date));
        assertFalse(prop1.getBoolean("SUPER_KEY_BOOLEAN"));

        // test setSuperProperties with same KEY
        JSONObject superProperties_new = new JSONObject();
        superProperties_new.put("SUPER_KEY_STRING", "super string new");
        superProperties_new.put("SUPER_KEY_BOOLEAN", true);
        instance.setSuperProperties(superProperties_new);
        instance.track("test_event1", properties);

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), SIZE_OF_SYSTEM_PROPERTY + 8);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), sDateFormat.format(date));
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getString("SUPER_KEY_STRING"), "super string new");
        assertEquals(prop1.getInt("SUPER_KEY_INT"), 0);
        assertEquals(prop1.getString("SUPER_KEY_DATE"), sDateFormat.format(super_date));
        assertTrue(prop1.getBoolean("SUPER_KEY_BOOLEAN"));

        // test setSuperProperties with same key as event properties
        properties.put("SUPER_KEY_STRING", "super key in event property");
        instance.track("test_event1", properties);

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), SIZE_OF_SYSTEM_PROPERTY + 8);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), sDateFormat.format(date));
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getString("SUPER_KEY_STRING"), "super key in event property");
        assertEquals(prop1.getInt("SUPER_KEY_INT"), 0);
        assertEquals(prop1.getString("SUPER_KEY_DATE"), sDateFormat.format(super_date));
        assertTrue(prop1.getBoolean("SUPER_KEY_BOOLEAN"));
        properties.remove("SUPER_KEY_STRING");

        // test unsetSuperProperties
        instance.unsetSuperProperty("SUPER_KEY_STRING");
        instance.unsetSuperProperty("SUPER_KEY_BOOLEAN");
        instance.track("test_event1", properties);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), SIZE_OF_SYSTEM_PROPERTY + 6);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), sDateFormat.format(date));
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getInt("SUPER_KEY_INT"), 0);
        assertEquals(prop1.getString("SUPER_KEY_DATE"), sDateFormat.format(super_date));

        // test dynamic super properties
        // 设置动态公共属性，在事件上报时动态获取事件发生时刻
        final String timeString = sDateFormat.format(new Date());
        instance.setDynamicSuperPropertiesTracker(
                new ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker() {
                    @Override
                    public JSONObject getDynamicSuperProperties() {
                        JSONObject dynamicSuperProperties = new JSONObject();
                        try {
                            dynamicSuperProperties.put("SUPER_KEY_DATE", timeString);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return dynamicSuperProperties;
                    }
                });

        instance.track("test_event1", properties);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), SIZE_OF_SYSTEM_PROPERTY + 6);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), sDateFormat.format(date));
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getInt("SUPER_KEY_INT"), 0);
        assertEquals(prop1.getString("SUPER_KEY_DATE"), timeString);

        // test user operation with superproperties
        JSONObject userProperties = new JSONObject();
        userProperties.put("USER_KEY_STRING", "user value");
        userProperties.put("USER_KEY_DOUBLE", 56.6);
        instance.user_set(userProperties);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "user_set");
        assertFalse(event.has("#event_name"));
        assertEquals(event.getString("#distinct_id"), instance.getDistinctId());
        assertFalse(event.has("#account"));

        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), 2);
        assertFalse(prop1.has("#app_version"));
        assertFalse(prop1.has("#network_type"));
        assertEquals(prop1.getString("USER_KEY_STRING"), "user value");
        assertEquals(prop1.getDouble("USER_KEY_DOUBLE"), 56.6, DELTA);


        // test clear super properties
        instance.clearSuperProperties();
        instance.track("test_event1", properties);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), SIZE_OF_SYSTEM_PROPERTY + 5);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), sDateFormat.format(date));
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getString("SUPER_KEY_DATE"), timeString);

    }

    @Test
    public void testUserId() throws InterruptedException, JSONException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        String distinctId = instance.getDistinctId();
        assertTrue(distinctId != null);

        String distinctId1 = "test_distinct_id";
        instance.identify(distinctId1);
        assertEquals(distinctId1, instance.getDistinctId());
        instance.track("test_event");
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(), SIZE_OF_EVENT_DATA);
        assertEquals(event.getString("#distinct_id"), distinctId1);
        assertFalse(event.has("#account_id"));

        String accountId = "test_account";
        instance.login(accountId);
        instance.track("test_event");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(), SIZE_OF_EVENT_DATA_LOGIN);
        assertEquals(event.getString("#distinct_id"), distinctId1);
        assertEquals(event.getString("#account_id"), accountId);

        instance.logout();
        instance.track("test_event");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#distinct_id"), distinctId1);
        assertFalse(event.has("#account_id"));
    }

    @Test
    public void testAutomaticData() throws InterruptedException, JSONException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected RemoteService getPoster() {
                        return new RemoteService() {
                            @Override
                            public String performRequest(String endpointUrl, String params, boolean debug, SSLSocketFactory socketFactory, Map<String, String> extraHeaders) throws IOException, ServiceUnavailableException {
                                try {
                                    JSONObject jsonObject = new JSONObject(params);
                                    messages.add(jsonObject.getJSONObject("automaticData"));
                                } catch (JSONException e) {

                                }
                                return "{code:0}";
                            }
                        };

                    }
                };
            }
        };

        instance.track("test_event");
        instance.flush();
        JSONObject automaticData = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(automaticData.getString("#lib_version"), TDConfig.VERSION);
        assertEquals(automaticData.getString("#lib"), "Android");
        assertEquals(automaticData.getString("#os"), "Android");
        assertEquals(automaticData.getString("#device_id"), instance.getDeviceId());
    }

    @Test
    public void testUserSet() throws JSONException, InterruptedException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        JSONObject properties = new JSONObject();
        Date date = new Date();
        properties.put("KEY_STRING", "string value");
        properties.put("KEY_DATE", date);
        properties.put("KEY_INT", 6);
        properties.put("KEY_BOOLEAN", true);

        instance.user_set(properties);

        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "user_set");
        assertEquals(event.length(), SIZE_OF_USER_DATA);
        assertTrue(event.has("#time"));
        assertTrue(event.has("#distinct_id"));
        assertFalse(TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        JSONObject prop = event.getJSONObject("properties");
        assertEquals(prop.length(), 4);
        properties.put("KEY_STRING", "string value");
        properties.put("KEY_DATE", date);
        properties.put("KEY_INT", 6);
        properties.put("KEY_BOOLEAN", true);

        String accountId = "test_account";
        instance.login(accountId);
        instance.user_setOnce(properties);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "user_setOnce");
        assertEquals(event.length(), SIZE_OF_USER_DATA_LOGIN);
        assertTrue(event.has("#time"));
        assertTrue(event.has("#distinct_id"));
        assertFalse(TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertEquals(event.getString("#account_id"), accountId);
        prop = event.getJSONObject("properties");
        assertEquals(prop.length(), 4);
        properties.put("KEY_STRING", "string value");
        properties.put("KEY_DATE", date);
        properties.put("KEY_INT", 6);
        properties.put("KEY_BOOLEAN", true);

        instance.logout();
        instance.user_add("amount", 50);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(), SIZE_OF_USER_DATA);
        assertEquals(event.getString("#type"), "user_add");
        assertTrue(event.has("#distinct_id"));
        assertFalse(TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertTrue(event.has("#time"));
        prop = event.getJSONObject("properties");
        assertEquals(prop.length(), 1);
        assertEquals(prop.getInt("amount"), 50);

        properties = new JSONObject();
        properties.put("KEY_1", 60);
        properties.put("KEY_2", 40.569);
        instance.user_add(properties);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(), SIZE_OF_USER_DATA);
        assertEquals(event.getString("#type"), "user_add");
        assertTrue(event.has("#distinct_id"));
        assertFalse(TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertTrue(event.has("#time"));
        prop = event.getJSONObject("properties");
        assertEquals(prop.length(), 2);
        assertEquals(prop.getInt("KEY_1"), 60);
        assertEquals(prop.getDouble("KEY_2"), 40.569, DELTA);

        properties = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("apple");
        jsonArray.put("ball");
        properties.put("KEY_LIST", jsonArray);
        instance.user_append(properties);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(), SIZE_OF_USER_DATA);
        assertEquals(event.getString("#type"), "user_append");
        assertTrue(event.has("#distinct_id"));
        assertFalse(TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertTrue(event.has("#time"));
        prop = event.getJSONObject("properties");
        assertEquals(prop.length(), 1);
        assertEquals(prop.getJSONArray("KEY_LIST"), jsonArray);

        instance.user_delete();
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(), SIZE_OF_USER_DATA);
        assertEquals(event.getString("#type"), "user_del");
        assertTrue(event.has("#distinct_id"));
        assertFalse(TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertTrue(event.has("#time"));
    }

    private JSONObject generateSuperProperties() throws JSONException {
        JSONObject superProperties = new JSONObject();

        Date super_date = new Date();
        superProperties.put("SUPER_KEY_STRING", "super string value");
        superProperties.put("SUPER_KEY_DATE", super_date);
        superProperties.put("SUPER_KEY_INT", 0);
        superProperties.put("SUPER_KEY_BOOLEAN", false);

        return superProperties;
    }

    private JSONObject generateEventProperties() throws JSONException {
        JSONObject properties = new JSONObject();

        Date date = new Date();
        properties.put("KEY_STRING", "string value");
        properties.put("KEY_DATE", date);
        properties.put("KEY_INT", 6);
        properties.put("KEY_BOOLEAN", true);

        return properties;
    }

    private void assertProperties(JSONObject all, JSONObject part) throws JSONException {
        for (Iterator<String> it = part.keys(); it.hasNext(); ) {
            String key = it.next();
            assertTrue(all.has(key));
            if (part.get(key) instanceof Date) {
                SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);
                final String timeString = sDateFormat.format(part.get(key));
                assertEquals(all.get(key).toString(), timeString);

            } else {
                assertEquals(all.get(key).toString(), part.get(key).toString());
            }
        }
    }

    private void assertPresetEventProperties(JSONObject properties) throws JSONException {
        assertEquals(properties.getString("#app_version"), mVersionName);
        assertTrue(properties.has("#zone_offset"));
        assertTrue(properties.has("#network_type"));
    }

    @Test
    public void testEnableTracking() throws JSONException, InterruptedException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        JSONObject superProperties = generateSuperProperties();
        instance.setSuperProperties(superProperties);
        instance.login("account_enable");
        instance.identify("id_enable");
        instance.track("test_event");

        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#distinct_id"), "id_enable");
        assertEquals(event.getString("#account_id"), "account_enable");
        assertProperties(event.getJSONObject("properties"), superProperties);

        instance.enableTracking(false);
        instance.track("test_event");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertNull(event);

        instance.enableTracking(true);
        instance.track("test_event");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#distinct_id"), "id_enable");
        assertEquals(event.getString("#account_id"), "account_enable");
        assertProperties(event.getJSONObject("properties"), superProperties);
    }

    @Test
    public void testOptOutIn() throws JSONException, InterruptedException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        JSONObject superProperties = generateSuperProperties();
        instance.setSuperProperties(superProperties);
        instance.login("account_enable");
        instance.identify("id_enable");
        instance.track("test_event");

        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#distinct_id"), "id_enable");
        assertEquals(event.getString("#account_id"), "account_enable");
        assertProperties(event.getJSONObject("properties"), superProperties);

        instance.optOutTracking();
        instance.track("test_event");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertNull(event);

        instance.optInTracking();
        instance.track("test_event");
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertNotEquals(event.getString("#distinct_id"), "id_enable");
        assertFalse(event.has("#account_id"));
    }

    @Test
    public void testZoneOffset() throws JSONException, InterruptedException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        instance.track("test_event");
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals(event.getString("#type"), DataType.TRACK.getType());
        assertEquals(event.length(), SIZE_OF_EVENT_DATA);
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertEquals(event.getString("#event_name"), "test_event");
        assertEquals(event.getJSONObject("properties").getString(TDConstants.KEY_APP_VERSION), mVersionName);
        assertTrue(event.getJSONObject("properties").has(TDConstants.KEY_NETWORK_TYPE));
        assertEquals(event.getJSONObject("properties").getDouble(TDConstants.KEY_ZONE_OFFSET), TimeZone.getDefault().getOffset(System.currentTimeMillis())/(1000.0 * 60 * 60), DELTA);

        instance.track("test_event", null, new Date());
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals(event.getString("#type"), DataType.TRACK.getType());
        assertEquals(event.length(), SIZE_OF_EVENT_DATA);
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertEquals(event.getString("#event_name"), "test_event");
        assertEquals(event.getJSONObject("properties").getString(TDConstants.KEY_APP_VERSION), mVersionName);
        assertTrue(event.getJSONObject("properties").has(TDConstants.KEY_NETWORK_TYPE));
        assertFalse(event.getJSONObject("properties").has(TDConstants.KEY_ZONE_OFFSET));

        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        instance.track("test_event", null, new Date(), tz);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals(event.getString("#type"), DataType.TRACK.getType());
        assertEquals(event.length(), SIZE_OF_EVENT_DATA);
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertEquals(event.getString("#event_name"), "test_event");
        assertEquals(event.getJSONObject("properties").getString(TDConstants.KEY_APP_VERSION), mVersionName);
        assertTrue(event.getJSONObject("properties").has(TDConstants.KEY_NETWORK_TYPE));
        assertEquals(event.getJSONObject("properties").getDouble(TDConstants.KEY_ZONE_OFFSET), tz.getOffset(System.currentTimeMillis())/(1000.0 * 60 * 60), DELTA);

    }

    @Test
    public void testUserUnSet() throws JSONException, InterruptedException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        instance.user_unset("key1");

        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), DataType.USER_UNSET.getType());
        assertEquals(event.length(), SIZE_OF_USER_DATA);
        assertTrue(event.has("#time"));
        assertTrue(event.has("#distinct_id"));
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        JSONObject prop = event.getJSONObject("properties");
        assertEquals(prop.length(), 1);
        assertTrue(prop.has("key1"));

        instance.user_unset("key1", "key2");

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), DataType.USER_UNSET.getType());
        assertEquals(event.length(), SIZE_OF_USER_DATA);
        assertTrue(event.has("#time"));
        assertTrue(event.has("#distinct_id"));
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        prop = event.getJSONObject("properties");
        assertEquals(prop.length(), 2);
        assertTrue(prop.has("key1"));
        assertTrue(prop.has("key2"));

        String[] keys = {"key1", "key2"};
        instance.user_unset(keys);

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), DataType.USER_UNSET.getType());
        assertEquals(event.length(), SIZE_OF_USER_DATA);
        assertTrue(event.has("#time"));
        assertTrue(event.has("#distinct_id"));
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        prop = event.getJSONObject("properties");
        assertEquals(prop.length(), 2);
        assertTrue(prop.has("key1"));
        assertTrue(prop.has("key2"));
    }

    @Test
    public void testCalibrateTime() throws JSONException, InterruptedException, ParseException {
        long timestamp = 1554687000000L;
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        ThinkingAnalyticsSDK.calibrateTime(timestamp);
        assertTime(instance, messages, timestamp);
    }

    private void assertTime(ThinkingAnalyticsSDK instance,  final BlockingQueue<JSONObject> messages, long timestamp)
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

    @Test
    public void testUniqueEvent() throws JSONException, InterruptedException, ParseException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        instance.track(new TDFirstEvent("test_unique", null));
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "track");
        assertEquals(event.getString("#event_name"), "test_unique");
        assertEquals(event.getString("#first_check_id"), instance.getDeviceId());
        JSONObject properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);

        JSONObject eventProp = generateEventProperties();
        instance.track(new TDFirstEvent("test_unique", eventProp));
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "track");
        assertEquals(event.getString("#event_name"), "test_unique");
        assertEquals(event.getString("#first_check_id"), instance.getDeviceId());
        properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);
        assertProperties(properties, eventProp);

        String firstCheckId = "ABC";
        TDFirstEvent uniqueEvent = new TDFirstEvent("test_unique", eventProp);
        uniqueEvent.setFirstCheckId(firstCheckId);
        instance.track(uniqueEvent);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "track");
        assertEquals(event.getString("#event_name"), "test_unique");
        assertEquals(event.getString("#first_check_id"), firstCheckId);
        properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);
        assertProperties(properties, eventProp);

        TDFirstEvent uniqueEvent1 = new TDFirstEvent("test_unique", eventProp);
        uniqueEvent1.setFirstCheckId("");
        instance.track(uniqueEvent1);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "track");
        assertEquals(event.getString("#event_name"), "test_unique");
        assertEquals(event.getString("#first_check_id"), instance.getDeviceId());
        properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);
        assertProperties(properties, eventProp);

        TDFirstEvent uniqueEvent2 = new TDFirstEvent("test_unique", eventProp);
        uniqueEvent2.setFirstCheckId(null);
        instance.track(uniqueEvent2);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "track");
        assertEquals(event.getString("#event_name"), "test_unique");
        assertEquals(event.getString("#first_check_id"), instance.getDeviceId());
        properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);
        assertProperties(properties, eventProp);
    }

    @Test
    public void testUpdatableEvent() throws JSONException, InterruptedException, ParseException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        instance.track(new TDUpdatableEvent("test_update", null, null));
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_update");
        assertEquals(event.getString("#type"), "track_update");
        assertFalse(event.has("#event_id"));
        JSONObject properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);

        instance.track(new TDUpdatableEvent("test_update", null, ""));
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "track_update");
        assertEquals(event.getString("#event_name"), "test_update");
        assertEquals(event.getString("#event_id"), "");
        properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);

        String eventId = "sample_event_id";
        JSONObject eventProp = generateEventProperties();
        instance.track(new TDUpdatableEvent("test_update", eventProp, eventId));
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "track_update");
        assertEquals(event.getString("#event_name"), "test_update");
        assertEquals(event.getString("#event_id"), eventId);
        properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);
        assertProperties(properties, eventProp);
    }

    @Test
    public void testOverWritableEvent() throws JSONException, InterruptedException, ParseException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mConfig) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected DatabaseAdapter getDbAdapter(Context context) {
                        return new DatabaseAdapter(context) {
                            @Override
                            public int addJSON(JSONObject j, Table table, String token) {
                                try {
                                    TDLog.i(TAG, j.toString(4));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                messages.add(j);
                                return 1;
                            }
                        };
                    }
                };
            }
        };

        instance.track(new TDOverWritableEvent("test_overwrite", null, null));
        JSONObject event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#event_name"), "test_overwrite");
        assertEquals(event.getString("#type"), "track_overwrite");
        assertFalse(event.has("#event_id"));
        JSONObject properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);

        instance.track(new TDOverWritableEvent("test_overwrite", null, ""));
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "track_overwrite");
        assertEquals(event.getString("#event_name"), "test_overwrite");
        assertEquals(event.getString("#event_id"), "");
        properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);

        String eventId = "sample_event_id";
        JSONObject eventProp = generateEventProperties();
        instance.track(new TDOverWritableEvent("test_overwrite", eventProp, eventId));
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "track_overwrite");
        assertEquals(event.getString("#event_name"), "test_overwrite");
        assertEquals(event.getString("#event_id"), eventId);
        properties = event.getJSONObject("properties");
        assertPresetEventProperties(properties);
        assertProperties(properties, eventProp);
    }
}