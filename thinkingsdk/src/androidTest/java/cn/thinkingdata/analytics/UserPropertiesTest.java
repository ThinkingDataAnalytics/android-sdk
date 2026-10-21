/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UserPropertiesTest extends BaseSdkTest {

    private static final String APP_ID = "at_user_001";
    private static Context context;
    private static MockWebServer server;
    private static ThinkingAnalyticsSDK sdk;

    @BeforeClass
    public static void classSetUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        server = new MockWebServer();
        server.start();
        sdk = initSdk(context, APP_ID, server);
    }

    @AfterClass
    public static void classTearDown() throws Exception {
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        prepareBetweenTests(context, APP_ID, server, sdk, 10);
        loginAndWait(sdk, "user_props_test");
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    @Test
    public void userSet_propertiesPresent() throws Exception {
        JSONObject props = new JSONObject();
        props.put("score", 9000);
        props.put("vip", true);
        sdk.user_set(props);
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = findEventByType(events, "user_set");
        assertNotNull(event);
        JSONObject resultProps = event.getJSONObject("properties");
        assertEquals(9000, resultProps.getInt("score"));
        assertTrue(resultProps.getBoolean("vip"));
        assertEquals(sdk.getDistinctId(), event.getString("#distinct_id"));
        assertEquals("user_props_test",
                event.optString("#account_id", event.optString("#login_id")));
    }

    @Test
    public void userSetOnce_typeIsUserSetOnce() throws Exception {
        JSONObject props = new JSONObject();
        props.put("register_date", "2024-01-01");
        sdk.user_setOnce(props);
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Decoded payload must contain a 'user_setOnce' event",
                findEventByType(events, "user_setOnce"));
    }

    @Test
    public void userAdd_numericValue_inRequest() throws Exception {
        JSONObject props = new JSONObject();
        props.put("points", 250);
        sdk.user_add(props);
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = findEventByType(events, "user_add");
        assertNotNull(event);
        assertEquals(250, event.getJSONObject("properties").getInt("points"));
    }

    @Test
    public void userAppend_typeIsUserAppend() throws Exception {
        JSONObject props = new JSONObject();
        JSONArray tags = new JSONArray();
        tags.put("sports");
        tags.put("music");
        props.put("interests", tags);
        sdk.user_append(props);
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Decoded payload must contain a 'user_append' event",
                findEventByType(events, "user_append"));
    }

    @Test
    public void userUniqAppend_typeIsUserUniqAppend() throws Exception {
        JSONObject props = new JSONObject();
        JSONArray tags = new JSONArray();
        tags.put("unique_tag");
        props.put("tags", tags);
        sdk.user_uniqAppend(props);
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Decoded payload must contain a 'user_uniq_append' event",
                findEventByType(events, "user_uniq_append"));
    }

    @Test
    public void userUnset_typeIsUserUnset() throws Exception {
        sdk.user_unset("nickname");
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Decoded payload must contain a 'user_unset' event",
                findEventByType(events, "user_unset"));
    }

    @Test
    public void userDelete_typeIsUserDel() throws Exception {
        sdk.user_delete();
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Decoded payload must contain a 'user_del' event",
                findEventByType(events, "user_del"));
    }

    private JSONObject findEventByType(JSONArray events, String type) throws Exception {
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.getJSONObject(i);
            if (type.equals(e.optString("#type"))) return e;
        }
        return null;
    }
}
