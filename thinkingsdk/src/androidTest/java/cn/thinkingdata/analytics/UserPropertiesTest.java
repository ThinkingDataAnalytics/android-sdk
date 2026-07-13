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
import okhttp3.mockwebserver.RecordedRequest;

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
        clearDbEvents(context, APP_ID);
        enqueueResponses(server, 10);
        resetSdkState(sdk);
        // resetSdkState sets NORMAL which triggers an async flush — drain it so
        // flushAndAssertRequest() captures the user property upload, not stale traffic.
        Thread.sleep(400);
        drainPendingRequests(server);
        sdk.login("user_props_test");
        Thread.sleep(200);
    }

    @After
    public void tearDown() { /* nothing extra */ }

    // ── user_set ──────────────────────────────────────────────────────────────

    @Test
    public void userSet_requestSent() throws Exception {
        JSONObject props = new JSONObject();
        props.put("nickname", "Alice");
        sdk.user_set(props);
        Thread.sleep(300);

        assertNotNull(flushAndAssertRequest(sdk, server));
    }

    @Test
    public void userSet_typeIsUserSet() throws Exception {
        JSONObject props = new JSONObject();
        props.put("level", 5);
        sdk.user_set(props);
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Decoded payload must contain a 'user_set' event",
                findEventByType(events, "user_set"));
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
    }

    // ── user_setOnce ──────────────────────────────────────────────────────────

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

    // ── user_add ──────────────────────────────────────────────────────────────

    @Test
    public void userAdd_typeIsUserAdd() throws Exception {
        JSONObject props = new JSONObject();
        props.put("coins", 100);
        sdk.user_add(props);
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Decoded payload must contain a 'user_add' event",
                findEventByType(events, "user_add"));
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

    // ── user_append ───────────────────────────────────────────────────────────

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

    // ── user_uniqAppend ───────────────────────────────────────────────────────

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

    // ── user_unset ────────────────────────────────────────────────────────────

    @Test
    public void userUnset_typeIsUserUnset() throws Exception {
        sdk.user_unset("nickname");
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Decoded payload must contain a 'user_unset' event",
                findEventByType(events, "user_unset"));
    }

    // ── user_delete ───────────────────────────────────────────────────────────

    @Test
    public void userDelete_typeIsUserDel() throws Exception {
        sdk.user_delete();
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Decoded payload must contain a 'user_del' event",
                findEventByType(events, "user_del"));
    }

    @Test
    public void userDelete_noPropertiesRequired() throws Exception {
        sdk.user_delete();
        Thread.sleep(300);

        RecordedRequest req = flushAndAssertRequest(sdk, server);
        assertNotNull("user_delete() must still trigger an upload request", req);
    }

    // ── common fields ─────────────────────────────────────────────────────────

    @Test
    public void userOps_containLoginId_afterLogin() throws Exception {
        JSONObject props = new JSONObject();
        props.put("city", "Shanghai");
        sdk.user_set(props);
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = findEventByType(events, "user_set");
        assertNotNull(event);
        assertTrue("User property event must contain loginId in payload",
                event.has("#account_id") || event.has("#login_id"));
    }

    @Test
    public void userOps_containDistinctId() throws Exception {
        JSONObject props = new JSONObject();
        props.put("country", "China");
        sdk.user_set(props);
        Thread.sleep(300);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = findEventByType(events, "user_set");
        assertNotNull(event);
        assertTrue("User property event must contain #distinct_id", event.has("#distinct_id"));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private JSONObject findEventByType(JSONArray events, String type) throws Exception {
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.getJSONObject(i);
            if (type.equals(e.optString("#type"))) return e;
        }
        return null;
    }
}
