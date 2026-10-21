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

    @Test
    public void track_withProperties_propsInRequest() throws Exception {
        JSONObject props = new JSONObject();
        props.put("product_id", "SKU-001");
        props.put("quantity", 3);
        props.put("is_gift", false);
        trackAndWait(sdk, "add_to_cart", props);

        RecordedRequest request = flushAndAssertRequest(sdk, server);
        assertTrue(request.getPath().contains("/sync"));
        assertNotNull(request.getHeader("Content-Type"));
        assertTrue(request.getHeader("Content-Type").contains("text/plain"));
        assertFalse(request.getBody().exhausted());

        JSONArray events = decodeRequestBody(request);
        JSONObject event = findEventByName(events, "add_to_cart");
        assertNotNull(event);
        assertEquals("track", event.getString("#type"));
        assertEquals(sdk.getDistinctId(), event.getString("#distinct_id"));
        assertTrue(event.getString("#time")
                .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));

        JSONObject resultProps = event.getJSONObject("properties");
        assertEquals("SKU-001", resultProps.getString("product_id"));
        assertEquals(3, resultProps.getInt("quantity"));
        assertFalse(resultProps.getBoolean("is_gift"));
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

    @Test
    public void flush_withNoEvents_noRequestSent() throws Exception {
        assertNoUploadAfterFlush(sdk, server);
    }
}
