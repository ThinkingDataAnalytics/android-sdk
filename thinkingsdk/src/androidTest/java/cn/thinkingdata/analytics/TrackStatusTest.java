/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
        clearDbEvents(context, APP_ID);
        enqueueResponses(server, 10);
        resetSdkState(sdk);
        // resetSdkState sets NORMAL which triggers an async flush — drain it so
        // "no request" assertions are not fooled by stale traffic.
        Thread.sleep(400);
        drainPendingRequests(server);
    }

    @After
    public void tearDown() throws Exception {
        // Always restore NORMAL so the @Before flush-out in resetSdkState works next time
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        Thread.sleep(100);
    }

    // ── NORMAL ────────────────────────────────────────────────────────────────

    @Test
    public void status_NORMAL_trackAndFlush_requestSent() throws Exception {
        trackAndWait(sdk, "normal_event", null);
        assertNotNull(flushAndAssertRequest(sdk, server));
    }

    // ── PAUSE ─────────────────────────────────────────────────────────────────
    //
    // SDK behaviour: isTrackDisabled() is true for PAUSE, so track() and flush()
    // both return early. Events are neither collected nor uploaded while paused.

    @Test
    public void status_PAUSE_trackBlocked_noDbRows() throws Exception {
        int countBefore = countDbEvents(context, APP_ID);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.PAUSE);
        Thread.sleep(300);
        sdk.track("paused_event");
        Thread.sleep(300);

        assertEquals("PAUSE must block track() — no new DB rows",
                countBefore, countDbEvents(context, APP_ID));
    }

    @Test
    public void status_PAUSE_manualFlush_noRequestSent() throws Exception {
        drainPendingRequests(server);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.PAUSE);
        Thread.sleep(300);
        sdk.flush();
        Thread.sleep(300);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNull("flush() while PAUSE is a no-op — no request must be sent", req);
    }

    @Test
    public void status_PAUSE_thenNORMAL_tracksNormally() throws Exception {
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.PAUSE);
        Thread.sleep(200);
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        Thread.sleep(200);

        trackAndWait(sdk, "after_resume_event", null);
        assertNotNull("After restoring NORMAL from PAUSE, events must upload",
                flushAndAssertRequest(sdk, server));
    }

    // ── STOP ──────────────────────────────────────────────────────────────────

    @Test
    public void status_STOP_eventsDiscarded_noRequestSent() throws Exception {
        drainPendingRequests(server);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.STOP);
        Thread.sleep(300);

        sdk.track("stopped_event");
        Thread.sleep(300);
        // flush() is a no-op while STOP (isTrackDisabled), but call it anyway
        sdk.flush();
        Thread.sleep(300);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNull("Events tracked while STOP must be discarded (no request)", req);
    }

    @Test
    public void status_STOP_thenNORMAL_previousEventsNotResurrected() throws Exception {
        drainPendingRequests(server);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.STOP);
        Thread.sleep(300);
        sdk.track("discarded_event");
        Thread.sleep(300);

        // Restoring NORMAL triggers an automatic flush() inside the SDK.
        // With an empty queue that flush must not upload the discarded event.
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        Thread.sleep(500);
        drainPendingRequests(server);

        sdk.flush();
        Thread.sleep(300);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNull("Events discarded while STOP must not reappear after NORMAL restored", req);
    }

    @Test
    public void status_STOP_thenNORMAL_newEventsTrackedNormally() throws Exception {
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.STOP);
        Thread.sleep(200);
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        Thread.sleep(200);

        trackAndWait(sdk, "after_stop_normal_event", null);
        assertNotNull("New events tracked after STOP→NORMAL must be uploaded",
                flushAndAssertRequest(sdk, server));
    }

    // ── SAVE_ONLY ─────────────────────────────────────────────────────────────

    @Test
    public void status_SAVE_ONLY_eventsStoredNotUploaded() throws Exception {
        drainPendingRequests(server);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.SAVE_ONLY);
        Thread.sleep(300);

        trackAndWait(sdk, "save_only_event", null);
        sdk.flush();
        Thread.sleep(300);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNull("SAVE_ONLY: events must be stored but NOT uploaded even on flush()", req);
    }

    @Test
    public void status_SAVE_ONLY_thenNORMAL_storedEventsUploaded() throws Exception {
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.SAVE_ONLY);
        Thread.sleep(200);
        trackAndWait(sdk, "buffered_event", null);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        Thread.sleep(200);

        RecordedRequest req = flushAndAwaitRequest(sdk, server);
        assertNotNull("Stored events must be uploaded after SAVE_ONLY → NORMAL transition", req);
    }

    // ── DB row count ──────────────────────────────────────────────────────────

    @Test
    public void status_STOP_noDbRowsCreated() throws Exception {
        int countBefore = countDbEvents(context, APP_ID);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.STOP);
        Thread.sleep(200);
        sdk.track("ignored_event");
        Thread.sleep(300);

        assertEquals("STOP status must not add rows to the database",
                countBefore, countDbEvents(context, APP_ID));
    }

    @Test
    public void status_SAVE_ONLY_dbRowsCreated() throws Exception {
        int countBefore = countDbEvents(context, APP_ID);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.SAVE_ONLY);
        Thread.sleep(200);
        trackAndWait(sdk, "saved_to_db", null);

        assertTrue("SAVE_ONLY must add at least one row to the database",
                countDbEvents(context, APP_ID) > countBefore);
    }
}
