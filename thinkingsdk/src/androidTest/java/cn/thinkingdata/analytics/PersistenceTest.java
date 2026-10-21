/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Verifies that events remain in SQLite until flush and are removed after a successful upload.
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
        prepareBetweenTests(context, APP_ID, server, sdk, 15);
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

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
}
