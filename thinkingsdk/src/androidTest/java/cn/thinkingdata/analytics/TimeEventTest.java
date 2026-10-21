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
 * <p>Verifies that a timed event uploads its duration without a background interval.
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
        prepareBetweenTestsFacade(context, APP_ID, server, sdk, 10);
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
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
