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

/**
 * Tests that state (loginId, distinctId, superProperties, DB rows) persists and behaves
 * correctly across different state mutations within the same SDK instance lifetime.
 */
@RunWith(AndroidJUnit4.class)
public class PersistenceTest extends BaseSdkTest {

    private static final String APP_ID = "at_persist_001";
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
        enqueueResponses(server, 15);
        resetSdkState(sdk);
        clearDbEvents(context, APP_ID);
    }

    @After
    public void tearDown() { /* nothing extra */ }

    // ── loginId ───────────────────────────────────────────────────────────────

    @Test
    public void login_loginId_readableAfterSet() throws Exception {
        sdk.login("persist_user_001");
        Thread.sleep(500);

        // A second handle to the same singleton should see the same loginId
        TDConfig config2 = TDConfig.getInstance(context, APP_ID, server.url("/").toString());
        ThinkingAnalyticsSDK sdk2 = ThinkingAnalyticsSDK.sharedInstance(config2);
        Thread.sleep(300);

        assertEquals("persist_user_001", sdk2.getLoginId());
    }

    @Test
    public void logout_loginId_clearedAndReadable() throws Exception {
        sdk.login("persist_user_002");
        Thread.sleep(300);
        sdk.logout();
        Thread.sleep(500);

        TDConfig config2 = TDConfig.getInstance(context, APP_ID, server.url("/").toString());
        ThinkingAnalyticsSDK sdk2 = ThinkingAnalyticsSDK.sharedInstance(config2);
        Thread.sleep(300);

        String loginId = sdk2.getLoginId();
        assertTrue("loginId must be empty after logout()",
                loginId == null || loginId.isEmpty());
    }

    // ── distinctId ────────────────────────────────────────────────────────────

    @Test
    public void identify_persistedAndReadable() throws Exception {
        sdk.identify("persist_distinct_xyz");
        Thread.sleep(500);

        TDConfig config2 = TDConfig.getInstance(context, APP_ID, server.url("/").toString());
        ThinkingAnalyticsSDK sdk2 = ThinkingAnalyticsSDK.sharedInstance(config2);
        Thread.sleep(300);

        assertEquals("persist_distinct_xyz", sdk2.getDistinctId());
    }

    @Test
    public void autoDistinctId_stableAcrossHandles() throws Exception {
        String originalId = sdk.getDistinctId();

        TDConfig config2 = TDConfig.getInstance(context, APP_ID, server.url("/").toString());
        ThinkingAnalyticsSDK sdk2 = ThinkingAnalyticsSDK.sharedInstance(config2);
        Thread.sleep(300);

        assertEquals("Auto-generated distinctId must be stable across handles",
                originalId, sdk2.getDistinctId());
    }

    // ── superProperties ───────────────────────────────────────────────────────

    @Test
    public void superProperties_persistedAndReadable() throws Exception {
        JSONObject props = new JSONObject();
        props.put("persist_key", "persist_value");
        sdk.setSuperProperties(props);
        Thread.sleep(500);

        TDConfig config2 = TDConfig.getInstance(context, APP_ID, server.url("/").toString());
        ThinkingAnalyticsSDK sdk2 = ThinkingAnalyticsSDK.sharedInstance(config2);
        Thread.sleep(300);

        JSONObject result = sdk2.getSuperProperties();
        assertNotNull("superProperties must be non-null", result);
        assertTrue(result.has("persist_key"));
        assertEquals("persist_value", result.getString("persist_key"));
    }

    @Test
    public void clearSuperProperties_persistedClearance() throws Exception {
        JSONObject props = new JSONObject();
        props.put("temp_key", "temp_value");
        sdk.setSuperProperties(props);
        Thread.sleep(300);
        sdk.clearSuperProperties();
        Thread.sleep(500);

        TDConfig config2 = TDConfig.getInstance(context, APP_ID, server.url("/").toString());
        ThinkingAnalyticsSDK sdk2 = ThinkingAnalyticsSDK.sharedInstance(config2);
        Thread.sleep(300);

        JSONObject result = sdk2.getSuperProperties();
        assertTrue("clearSuperProperties() result must persist",
                result == null || result.length() == 0);
    }

    // ── events in DB survive until flushed ───────────────────────────────────

    @Test
    public void unFlushedEvents_remainInDb_untilFlush() throws Exception {
        sdk.track("unflushed_event_1");
        sdk.track("unflushed_event_2");
        Thread.sleep(500);

        int count = countDbEvents(context, APP_ID);
        assertTrue("Unflushed events must be present in the SQLite DB", count >= 2);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertTrue("Flushed payload must contain the 2 tracked events", events.length() >= 2);

        Thread.sleep(500);
        assertEquals("DB must be empty after a successful flush",
                0, countDbEvents(context, APP_ID));
    }

    @Test
    public void loginId_inEventPayload_afterLoginAndFlush() throws Exception {
        sdk.login("persist_test_account");
        Thread.sleep(200);
        trackAndWait(sdk, "account_check_event", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = events.getJSONObject(0);

        // Accept either field name that the SDK uses for the login identifier
        String payloadLoginId = event.optString("#account_id",
                event.optString("#login_id", ""));
        assertEquals("Event must carry the loginId set before track()",
                "persist_test_account", payloadLoginId);
    }
}
