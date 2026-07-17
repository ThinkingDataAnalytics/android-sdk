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

import static org.junit.Assert.*;

/**
 * Instrumented tests for {@link TDAnalytics#timeEvent(String)}.
 *
 * <p>Verifies that {@code #duration} only counts foreground time: background intervals are
 * accumulated via {@link ThinkingAnalyticsSDK#appEnterBackground()} /
 * {@link ThinkingAnalyticsSDK#appBecomeActive()} and reported separately as
 * {@code #background_duration}.
 */
@RunWith(AndroidJUnit4.class)
public class TimeEventTest extends BaseSdkTest {

    private static final String APP_ID = "at_time_event_001";
    private static final String EVENT = "timed_bg_check";

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
        enqueueResponses(server, 10);
        resetFacadeSdkState();
        clearDbEvents(context, APP_ID);
        Thread.sleep(400);
        drainPendingRequests(server);
    }

    @After
    public void tearDown() throws Exception {
        drainPendingRequests(server);
    }

    @Test
    public void timeEvent_durationExcludesBackgroundTime() throws Exception {
        long wallStart = System.currentTimeMillis();

        TDAnalytics.timeEvent(EVENT);
        Thread.sleep(400); // let TrackTaskManager register the timer

        // Foreground ~1.0s
        Thread.sleep(1000);

        // Enter background: freeze foreground accumulation, start counting background
        sdk.appEnterBackground();
        Thread.sleep(2000);

        // Return to foreground: background interval recorded separately
        sdk.appBecomeActive();
        Thread.sleep(1000);

        trackAndWaitFacade(EVENT, null);

        long wallElapsedSec = (System.currentTimeMillis() - wallStart) / 1000L;

        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
        JSONObject event = events.getJSONObject(0);
        assertEquals(EVENT, event.getString("#event_name"));

        JSONObject properties = event.getJSONObject("properties");
        assertTrue("timed event must contain #duration",
                properties.has(TDConstants.KEY_DURATION));
        assertTrue("timed event must contain #background_duration after backgrounding",
                properties.has(TDConstants.KEY_BACKGROUND_DURATION));

        double duration = properties.getDouble(TDConstants.KEY_DURATION);
        double backgroundDuration = properties.getDouble(TDConstants.KEY_BACKGROUND_DURATION);

        // Foreground was ~2.0s (1s + 1s); allow timing jitter on device.
        assertTrue("#duration should be ~2s foreground, got " + duration,
                duration >= 1.5 && duration <= 3.0);
        // Background was ~2.0s.
        assertTrue("#background_duration should be ~2s, got " + backgroundDuration,
                backgroundDuration >= 1.5 && backgroundDuration <= 3.0);
        // Duration must exclude background: wall clock is ~4s+, duration must stay below that.
        assertTrue("#duration must be less than wall-clock elapsed (background excluded); "
                        + "duration=" + duration + " wall=" + wallElapsedSec,
                duration < wallElapsedSec - 0.5);
        assertTrue("#duration + #background_duration should approximate wall time",
                Math.abs((duration + backgroundDuration) - wallElapsedSec) < 2.0);
    }

    @Test
    public void timeEvent_withoutBackground_hasDurationOnly() throws Exception {
        TDAnalytics.timeEvent(EVENT);
        Thread.sleep(400);
        Thread.sleep(1200);
        trackAndWaitFacade(EVENT, null);

        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
        JSONObject properties = events.getJSONObject(0).getJSONObject("properties");

        assertTrue(properties.has(TDConstants.KEY_DURATION));
        double duration = properties.getDouble(TDConstants.KEY_DURATION);
        assertTrue("#duration should be ~1.2s, got " + duration,
                duration >= 0.8 && duration <= 2.5);
        assertFalse("no backgrounding ⇒ no #background_duration",
                properties.has(TDConstants.KEY_BACKGROUND_DURATION));
    }
}
