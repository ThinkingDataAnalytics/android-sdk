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

    @Test
    public void facade_track_afterLoginAndSetDistinctId_bothIdsInUpload() throws Exception {
        String originalAccount = TDAnalytics.getAccountId();
        TDAnalytics.setDistinctId("facade_both_distinct");
        Thread.sleep(200);
        assertEquals(originalAccount, TDAnalytics.getAccountId());
        TDAnalytics.login("facade_both_account");
        Thread.sleep(300);
        assertEquals("facade_both_account", TDAnalytics.getAccountId());
        assertEquals("facade_both_distinct", TDAnalytics.getDistinctId());
        assertEquals(sdk.getDistinctId(), TDAnalytics.getDistinctId());
        trackAndWaitFacade("facade_both_ids_event", null);

        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
        JSONObject event = events.getJSONObject(0);

        assertEquals("facade_both_distinct", event.getString("#distinct_id"));
        assertTrue(event.has("#account_id") || event.has("#login_id"));
        String accountId = event.optString("#account_id", event.optString("#login_id"));
        assertEquals("facade_both_account", accountId);

        TDAnalytics.logout();
        waitForTrackTasks();
        String loggedOutAccount = TDAnalytics.getAccountId();
        assertTrue(loggedOutAccount == null || loggedOutAccount.isEmpty());
        assertEquals("facade_both_distinct", TDAnalytics.getDistinctId());
    }
}
