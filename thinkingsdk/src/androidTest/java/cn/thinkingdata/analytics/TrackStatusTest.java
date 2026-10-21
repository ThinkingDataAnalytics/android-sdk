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

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TrackStatusTest extends BaseSdkTest {

    private static final String APP_ID = "at_status_001";
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
    }

    @After
    public void tearDown() throws Exception {
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        waitForTrackTasks();
        cleanupAfterTest(server);
    }

    @Test
    public void status_PAUSE_blocksEvents_thenNORMAL_recovers() throws Exception {
        int countBefore = countDbEvents(context, APP_ID);
        int requestsBefore = server.getRequestCount();

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.PAUSE);
        waitForTrackTasks();
        trackAndWait(sdk, "paused_event", null);
        sdk.flush();
        waitForTrackTasks();

        assertEquals("PAUSE must block track() — no new DB rows",
                countBefore, countDbEvents(context, APP_ID));
        assertEquals("PAUSE must block flush()", requestsBefore, server.getRequestCount());

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        waitForTrackTasks();
        trackAndWait(sdk, "after_pause_event", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull(findEventByName(events, "after_pause_event"));
        assertNull(findEventByName(events, "paused_event"));
    }

    @Test
    public void status_SAVE_ONLY_buffersEvents_thenNORMAL_uploads() throws Exception {
        int countBefore = countDbEvents(context, APP_ID);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.SAVE_ONLY);
        waitForTrackTasks();
        trackAndWait(sdk, "save_only_event", null);
        assertTrue("SAVE_ONLY must persist the event",
                countDbEvents(context, APP_ID) > countBefore);
        sdk.flush();
        waitForTrackTasks();

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNull("SAVE_ONLY: events must be stored but NOT uploaded even on flush()", req);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        waitForTrackTasks();
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("Buffered event must upload after restoring NORMAL",
                findEventByName(events, "save_only_event"));
    }

    @Test
    public void status_STOP_discardsEvents_thenNORMAL_recovers() throws Exception {
        int countBefore = countDbEvents(context, APP_ID);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.STOP);
        waitForTrackTasks();
        trackAndWait(sdk, "ignored_event", null);

        assertEquals("STOP status must not add rows to the database",
                countBefore, countDbEvents(context, APP_ID));

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        waitForTrackTasks();
        trackAndWait(sdk, "after_stop_event", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull(findEventByName(events, "after_stop_event"));
        assertNull("STOP events must not reappear after restoring NORMAL",
                findEventByName(events, "ignored_event"));
    }
}
