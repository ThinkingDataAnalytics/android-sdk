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
 * Instrumented tests for {@link TDAnalytics} facade static APIs.
 */
@RunWith(AndroidJUnit4.class)
public class TDAnalyticsFacadeTest extends BaseSdkTest {

    private static final String APP_ID = "at_facade_001";
    private static Context context;
    private static MockWebServer server;
    private static ThinkingAnalyticsSDK sdk;

    @BeforeClass
    public static void classSetUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        server = new MockWebServer();
        server.start();
        sdk = initFacadeSdk(context, APP_ID, server);
    }

    @AfterClass
    public static void classTearDown() throws Exception {
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        prepareBetweenTestsFacade(context, APP_ID, server, sdk, 10);
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    // ── login / setDistinctId getters ─────────────────────────────────────────

    @Test
    public void facade_login_setsAccountId() throws InterruptedException {
        TDAnalytics.login("facade_user_001");
        Thread.sleep(300);
        assertEquals("facade_user_001", TDAnalytics.getAccountId());
    }

    @Test
    public void facade_setDistinctId_changesDistinctId() throws InterruptedException {
        TDAnalytics.setDistinctId("facade_distinct_001");
        Thread.sleep(300);
        assertEquals("facade_distinct_001", TDAnalytics.getDistinctId());
    }

    @Test
    public void facade_login_doesNotChangeDistinctId() throws InterruptedException {
        String distinctIdBefore = TDAnalytics.getDistinctId();
        TDAnalytics.login("facade_user_002");
        Thread.sleep(300);
        assertEquals(distinctIdBefore, TDAnalytics.getDistinctId());
    }

    @Test
    public void facade_setDistinctId_doesNotChangeAccountId() throws InterruptedException {
        TDAnalytics.login("facade_user_003");
        Thread.sleep(200);
        TDAnalytics.setDistinctId("facade_distinct_003");
        Thread.sleep(300);
        assertEquals("facade_user_003", TDAnalytics.getAccountId());
    }

    // ── upload payload via facade track / flush ───────────────────────────────

    @Test
    public void facade_track_afterLogin_accountIdInUpload() throws Exception {
        TDAnalytics.login("facade_upload_account");
        Thread.sleep(300);
        trackAndWaitFacade("facade_login_event", null);

        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
        JSONObject event = events.getJSONObject(0);
        assertTrue(event.has("#account_id") || event.has("#login_id"));
        String accountId = event.optString("#account_id", event.optString("#login_id"));
        assertEquals("facade_upload_account", accountId);
    }

    @Test
    public void facade_track_afterSetDistinctId_distinctIdInUpload() throws Exception {
        TDAnalytics.setDistinctId("facade_upload_distinct");
        Thread.sleep(300);
        trackAndWaitFacade("facade_identify_event", null);

        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
        JSONObject event = events.getJSONObject(0);
        assertEquals("facade_upload_distinct", event.getString("#distinct_id"));
    }

    @Test
    public void facade_track_afterLoginAndSetDistinctId_bothIdsInUpload() throws Exception {
        TDAnalytics.setDistinctId("facade_both_distinct");
        Thread.sleep(200);
        TDAnalytics.login("facade_both_account");
        Thread.sleep(300);
        trackAndWaitFacade("facade_both_ids_event", null);

        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
        JSONObject event = events.getJSONObject(0);

        assertEquals("facade_both_distinct", event.getString("#distinct_id"));
        assertTrue(event.has("#account_id") || event.has("#login_id"));
        String accountId = event.optString("#account_id", event.optString("#login_id"));
        assertEquals("facade_both_account", accountId);
    }

    @Test
    public void facade_getDistinctId_matchesSdkInstance() throws InterruptedException {
        TDAnalytics.setDistinctId("facade_sync_check");
        Thread.sleep(300);
        assertEquals(sdk.getDistinctId(), TDAnalytics.getDistinctId());
    }
}
