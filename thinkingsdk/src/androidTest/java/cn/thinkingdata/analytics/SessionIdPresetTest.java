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

import cn.thinkingdata.analytics.utils.TDConstants;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@code #session_id} is attached to tracked events and rotates after the configured timeout.
 */
@RunWith(AndroidJUnit4.class)
public class SessionIdPresetTest extends BaseSdkTest {

    private static final String APP_ID = "at_session_001";

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
        clearPresetDisableList();
        sdk.mConfig.setSessionTimeout(TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS);
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        clearPresetDisableList();
        sdk.mConfig.setSessionTimeout(TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS);
        prepareBetweenTests(context, APP_ID, server, sdk, 10);
        clearDbEvents(context, APP_ID);
    }

    @After
    public void tearDown() throws Exception {
        clearPresetDisableList();
        sdk.mConfig.setSessionTimeout(TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS);
        cleanupAfterTest(server);
    }

    @Test
    public void track_withinSession_reusesSessionId() throws Exception {
        sdk.mConfig.setSessionTimeout(60_000L);
        trackAndWait(sdk, "session_keep_a", null);
        trackAndWait(sdk, "session_keep_b", null);
        trackAndWait(sdk, "session_keep_c", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        String first = sessionIdOf(events, "session_keep_a");
        assertEquals(first, sessionIdOf(events, "session_keep_b"));
        assertEquals(first, sessionIdOf(events, "session_keep_c"));
        assertTrue(first.matches(UUID_V7));
    }

    @Test
    public void track_afterTimeout_usesNewSessionId() throws Exception {
        sdk.mConfig.setSessionTimeout(50L);
        trackAndWait(sdk, "session_before_timeout", null);
        Thread.sleep(80);
        trackAndWait(sdk, "session_after_timeout", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        String before = findEvent(events, "session_before_timeout")
                .getJSONObject("properties").getString(TDConstants.KEY_SESSION_ID);
        String after = findEvent(events, "session_after_timeout")
                .getJSONObject("properties").getString(TDConstants.KEY_SESSION_ID);

        assertNotEquals(before, after);
        assertTrue(timestampMillis(after) >= timestampMillis(before));
    }

    private static final String UUID_V7 =
            "^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

    private static long timestampMillis(String uuid) {
        return Long.parseLong(uuid.replace("-", "").substring(0, 12), 16);
    }

    private static String sessionIdOf(JSONArray events, String eventName) throws Exception {
        return findEvent(events, eventName).getJSONObject("properties")
                .getString(TDConstants.KEY_SESSION_ID);
    }

    private static JSONObject findEvent(JSONArray events, String eventName) throws Exception {
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            if (eventName.equals(event.optString(TDConstants.KEY_EVENT_NAME))) {
                return event;
            }
        }
        assertNotNull("missing event " + eventName, null);
        return null;
    }
}
