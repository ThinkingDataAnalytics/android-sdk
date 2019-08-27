package cn.thinkingdata.android;

import android.content.Context;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import cn.thinkingdata.android.utils.RemoteService;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


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
    private static final Double DELTA =  0.0000;

    private static final int POLL_WAIT_SECONDS = 2;

    private static final int SIZE_OF_EVENT_DATA = 6;
    private static final int SIZE_OF_EVENT_DATA_LOGIN = 7;
    private static final int SIZE_OF_USER_DATA = 5;
    private static final int SIZE_OF_USER_DATA_LOGIN = 6;

    private static Context mAppContext;
    private final static String mVersionName = "1.0";
    private static TDConfig mConfig;

    @Before
    public void setUp() {
        ThinkingAnalyticsSDK.enableTrackLog(true);
        mAppContext = ApplicationProvider.getApplicationContext();
        mConfig = TDConfig.getInstance(mAppContext,TA_SERVER_URL, TA_APP_ID );
    }


    @Test
    public void useAppContext() {
        // Context of the app under test.
        assertEquals("cn.thinkingdata.analytics", mAppContext.getPackageName());
    }

    @Test
    public void trackBasic() throws InterruptedException, JSONException {
        final BlockingDeque<JSONObject> messages = new LinkedBlockingDeque<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mAppContext, TA_APP_ID, mConfig, false) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected RemoteService getPoster() {
                        return new RemoteService() {
                            @Override
                            public String performRequest(String endpointUrl, String params) throws IOException, ServiceUnavailableException {
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
        for (int i = 0; i <  20; i++) {
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
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mAppContext, TA_APP_ID, mConfig, false) {
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
        assertEquals(prop1.length(), 6);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), date.toString());
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
        assertEquals(prop1.length(), 10);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), date.toString());
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getString("SUPER_KEY_STRING"), "super string value");
        assertEquals(prop1.getInt("SUPER_KEY_INT"), 0);
        assertEquals(prop1.getString("SUPER_KEY_DATE"), super_date.toString());
        assertFalse(prop1.getBoolean("SUPER_KEY_BOOLEAN"));

        // test setSuperProperties with same KEY
        JSONObject superProperties_new = new JSONObject();
        superProperties_new.put("SUPER_KEY_STRING", "super string new");
        superProperties_new.put("SUPER_KEY_BOOLEAN", true);
        instance.setSuperProperties(superProperties_new);
        instance.track("test_event1", properties);

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), 10);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), date.toString());
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getString("SUPER_KEY_STRING"), "super string new");
        assertEquals(prop1.getInt("SUPER_KEY_INT"), 0);
        assertEquals(prop1.getString("SUPER_KEY_DATE"), super_date.toString());
        assertTrue(prop1.getBoolean("SUPER_KEY_BOOLEAN"));

        // test setSuperProperties with same key as event properties
        properties.put("SUPER_KEY_STRING", "super key in event property");
        instance.track("test_event1", properties);

        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), 10);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), date.toString());
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getString("SUPER_KEY_STRING"), "super key in event property");
        assertEquals(prop1.getInt("SUPER_KEY_INT"), 0);
        assertEquals(prop1.getString("SUPER_KEY_DATE"), super_date.toString());
        assertTrue(prop1.getBoolean("SUPER_KEY_BOOLEAN"));
        properties.remove("SUPER_KEY_STRING");

        // test unsetSuperProperties
        instance.unsetSuperProperty("SUPER_KEY_STRING");
        instance.unsetSuperProperty("SUPER_KEY_BOOLEAN");
        instance.track("test_event1", properties);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        prop1 = event.getJSONObject("properties");
        assertEquals(prop1.length(), 8);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), date.toString());
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getInt("SUPER_KEY_INT"), 0);
        assertEquals(prop1.getString("SUPER_KEY_DATE"), super_date.toString());

        // test dynamic super properties
        // 设置动态公共属性，在事件上报时动态获取事件发生时刻
        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern, Locale.CHINA);
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
        assertEquals(prop1.length(), 8);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), date.toString());
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
        assertEquals(prop1.length(), 7);
        assertEquals(prop1.getString("#app_version"), mVersionName);
        assertTrue(prop1.has("#network_type"));
        assertEquals(prop1.getString("KEY_STRING"), "string value");
        assertEquals(prop1.getInt("KEY_INT"), 6);
        assertEquals(prop1.getString("KEY_DATE"), date.toString());
        assertTrue(prop1.getBoolean("KEY_BOOLEAN"));
        assertEquals(prop1.getString("SUPER_KEY_DATE"), timeString);

    }

    @Test
    public void testUserId() throws InterruptedException, JSONException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mAppContext, TA_APP_ID, mConfig, false) {
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
        JSONObject event =  messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(), SIZE_OF_EVENT_DATA);
        assertEquals(event.getString("#distinct_id"), distinctId1);
        assertFalse(event.has("#account_id"));

        String accountId = "test_account";
        instance.login(accountId);
        instance.track("test_event");
        event =  messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(), SIZE_OF_EVENT_DATA_LOGIN);
        assertEquals(event.getString("#distinct_id"), distinctId1);
        assertEquals(event.getString("#account_id"), accountId);

        instance.logout();
        instance.track("test_event");
        event =  messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#distinct_id"), distinctId1);
        assertFalse(event.has("#account_id"));
    }

    @Test
    public void testAutomaticData() throws InterruptedException, JSONException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mAppContext, TA_APP_ID, mConfig, false) {
            @Override
            protected DataHandle getDataHandleInstance(Context context) {
                return new DataHandle(context) {
                    @Override
                    protected RemoteService getPoster() {
                        return new RemoteService() {
                            @Override
                            public String performRequest(String endpointUrl, String params) throws IOException, ServiceUnavailableException {
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
        assertEquals(automaticData.getString("#lib_version"), BuildConfig.TDSDK_VERSION);
        assertEquals(automaticData.getString("#lib"), "Android");
        assertEquals(automaticData.getString("#os"), "Android");
        assertEquals(automaticData.getString("#device_id"), instance.getDeviceId());
    }

    @Test
    public void testUserSet() throws JSONException, InterruptedException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mAppContext, TA_APP_ID, mConfig, false) {
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

        JSONObject  event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.getString("#type"), "user_set");
        assertEquals(event.length(), SIZE_OF_USER_DATA);
        assertTrue(event.has("#time"));
        assertTrue(event.has("#distinct_id"));
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
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
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
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
        assertEquals(event.length(),SIZE_OF_USER_DATA);
        assertEquals(event.getString("#type"), "user_add");
        assertTrue(event.has("#distinct_id"));
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertTrue(event.has("#time"));
        prop = event.getJSONObject("properties");
        assertEquals(prop.length(),1);
        assertEquals(prop.getInt("amount"), 50);

        properties = new JSONObject();
        properties.put("KEY_1", 60);
        properties.put("KEY_2", 40.569);
        instance.user_add(properties);
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(),SIZE_OF_USER_DATA);
        assertEquals(event.getString("#type"), "user_add");
        assertTrue(event.has("#distinct_id"));
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertTrue(event.has("#time"));
        prop = event.getJSONObject("properties");
        assertEquals(prop.length(),2);
        assertEquals(prop.getInt("KEY_1"), 60);
        assertEquals(prop.getDouble("KEY_2"), 40.569, DELTA);

        instance.user_delete();
        event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        assertEquals(event.length(),SIZE_OF_USER_DATA);
        assertEquals(event.getString("#type"), "user_del");
        assertTrue(event.has("#distinct_id"));
        assertTrue(!TextUtils.isEmpty(event.getString(TDConstants.DATA_ID)));
        assertTrue(event.has("#time"));
        assertEquals(event.getJSONObject("properties").length(), 0);

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
            assertEquals(all.get(key).toString(), part.get(key).toString());
        }
    }

    @Test
    public void testEnableTracking() throws JSONException, InterruptedException {
        final BlockingQueue<JSONObject> messages = new LinkedBlockingQueue<>();
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mAppContext, TA_APP_ID, mConfig, false) {
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

        JSONObject  event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
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
        ThinkingAnalyticsSDK instance = new ThinkingAnalyticsSDK(mAppContext, TA_APP_ID, mConfig, false) {
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

        JSONObject  event = messages.poll(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
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
        assertTrue(event.getJSONObject("properties").length() == 2);
    }
}

