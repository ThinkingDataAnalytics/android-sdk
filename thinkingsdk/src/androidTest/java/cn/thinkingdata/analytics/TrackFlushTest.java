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

/**
 * Tests for the full track → flush → HTTP upload pipeline via MockWebServer.
 */
@RunWith(AndroidJUnit4.class)
public class TrackFlushTest extends BaseSdkTest {

    private static final String APP_ID = "at_track_001";
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
        prepareBetweenTests(context, APP_ID, server, sdk, 15);
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    // ── basic track + flush ───────────────────────────────────────────────────

    @Test
    public void track_withFlush_serverReceivesRequest() throws Exception {
        trackAndWait(sdk, "test_event", null);
        RecordedRequest req = flushAndAssertRequest(sdk, server);
        assertTrue("Request path must contain /sync", req.getPath().contains("/sync"));
    }

    @Test
    public void track_requestContentType_isTextPlain() throws Exception {
        trackAndWait(sdk, "test_event_ct", null);
        RecordedRequest req = flushAndAssertRequest(sdk, server);
        String ct = req.getHeader("Content-Type");
        assertNotNull("Content-Type header must be present", ct);
        assertTrue("Content-Type must be text/plain, got: " + ct, ct.contains("text/plain"));
    }

    @Test
    public void track_requestBody_isNonEmpty() throws Exception {
        trackAndWait(sdk, "test_event_body", null);
        RecordedRequest req = flushAndAssertRequest(sdk, server);
        assertFalse("Request body must not be empty", req.getBody().exhausted());
    }

    // ── event structure ───────────────────────────────────────────────────────

    @Test
    public void track_eventType_isTrack() throws Exception {
        trackAndWait(sdk, "purchase", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertTrue("At least one event must be in the payload", events.length() > 0);
        assertEquals("track", events.getJSONObject(0).getString("#type"));
    }

    @Test
    public void track_eventName_inRequestBody() throws Exception {
        trackAndWait(sdk, "button_click", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = findEventByName(events, "button_click");
        assertNotNull("Event 'button_click' must appear in the uploaded payload", event);
    }

    @Test
    public void track_requiredFields_allPresent() throws Exception {
        trackAndWait(sdk, "field_check", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = events.getJSONObject(0);

        assertTrue("Event must have #type",        event.has("#type"));
        assertTrue("Event must have #time",        event.has("#time"));
        assertTrue("Event must have #distinct_id", event.has("#distinct_id"));
        assertTrue("Event must have #event_name",  event.has("#event_name"));
        assertTrue("Event must have properties",   event.has("properties"));
    }

    @Test
    public void track_distinctId_matchesSdkGetDistinctId() throws Exception {
        String expectedDistinctId = sdk.getDistinctId();
        trackAndWait(sdk, "id_check", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertEquals(expectedDistinctId, events.getJSONObject(0).getString("#distinct_id"));
    }

    @Test
    public void track_time_isFormattedTimestamp() throws Exception {
        trackAndWait(sdk, "time_check", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        String time = events.getJSONObject(0).getString("#time");
        assertNotNull(time);
        assertTrue("Event #time must match yyyy-MM-dd HH:mm:ss.SSS, got: " + time,
                time.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }

    // ── custom properties ─────────────────────────────────────────────────────

    @Test
    public void track_withProperties_propsInRequest() throws Exception {
        JSONObject props = new JSONObject();
        props.put("product_id", "SKU-001");
        props.put("quantity", 3);
        props.put("is_gift", false);
        trackAndWait(sdk, "add_to_cart", props);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = findEventByName(events, "add_to_cart");
        assertNotNull(event);

        JSONObject resultProps = event.getJSONObject("properties");
        assertEquals("SKU-001", resultProps.getString("product_id"));
        assertEquals(3, resultProps.getInt("quantity"));
        assertFalse(resultProps.getBoolean("is_gift"));
    }

    @Test
    public void track_withNullProperties_noException() throws Exception {
        sdk.track("null_props_event");
        Thread.sleep(300);
        RecordedRequest req = flushAndAwaitRequest(sdk, server);
        assertNotNull("Even a track with null props must produce an upload request", req);
    }

    // ── login + track ─────────────────────────────────────────────────────────

    @Test
    public void track_afterLogin_accountIdInRequest() throws Exception {
        loginAndWait(sdk, "user_track_001");
        trackAndWait(sdk, "login_event_check", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = events.getJSONObject(0);
        assertTrue("Event must contain login id after login()",
                event.has("#account_id") || event.has("#login_id"));
    }

    @Test
    public void track_afterLogin_accountIdMatchesLoginId() throws Exception {
        loginAndWait(sdk, "user_track_account_check");
        trackAndWait(sdk, "login_account_match", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = events.getJSONObject(0);
        String accountId = event.optString("#account_id", event.optString("#login_id", null));
        assertEquals("user_track_account_check", accountId);
    }

    @Test
    public void track_afterIdentify_distinctIdInRequest() throws Exception {
        identifyAndWait(sdk, "custom_distinct_upload_id");
        trackAndWait(sdk, "identify_event_check", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = events.getJSONObject(0);
        assertEquals("custom_distinct_upload_id", event.getString("#distinct_id"));
    }

    @Test
    public void track_afterLoginAndIdentify_bothIdsInRequest() throws Exception {
        identifyAndWait(sdk, "distinct_after_login_test");
        loginAndWait(sdk, "account_after_login_test");
        trackAndWait(sdk, "login_identify_event_check", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = events.getJSONObject(0);

        assertEquals("distinct_after_login_test", event.getString("#distinct_id"));
        assertTrue("Event must contain account id after login()",
                event.has("#account_id") || event.has("#login_id"));
        String accountId = event.optString("#account_id", event.optString("#login_id"));
        assertEquals("account_after_login_test", accountId);
    }

    @Test
    public void track_afterLogout_noAccountIdInRequest() throws Exception {
        loginAndWait(sdk, "user_track_002");
        sdk.logout();
        waitForTrackTasks();
        trackAndWait(sdk, "logout_event_check", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = events.getJSONObject(0);
        assertFalse("Event must NOT contain #account_id after logout()", event.has("#account_id"));
        assertFalse("Event must NOT contain #login_id after logout()", event.has("#login_id"));
    }

    // ── multiple events ───────────────────────────────────────────────────────

    @Test
    public void track_multipleEvents_allInOneRequest() throws Exception {
        trackAndWait(sdk, "event_a", null);
        trackAndWait(sdk, "event_b", null);
        trackAndWait(sdk, "event_c", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertEquals("All 3 events must be batched in one request", 3, events.length());
    }

    @Test
    public void track_multipleEvents_orderPreserved() throws Exception {
        for (int i = 1; i <= 3; i++) {
            JSONObject p = new JSONObject();
            p.put("seq", i);
            trackAndWait(sdk, "ordered_event", p);
        }

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertEquals(3, events.length());
        for (int i = 0; i < events.length(); i++) {
            assertEquals(i + 1,
                    events.getJSONObject(i).getJSONObject("properties").getInt("seq"));
        }
    }

    // ── no-op when empty ──────────────────────────────────────────────────────

    @Test
    public void flush_withNoEvents_noRequestSent() throws Exception {
        assertNoUploadAfterFlush(sdk, server);
    }
}
