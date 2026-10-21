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
 * Instrumented tests for time calibration APIs.
 */
@RunWith(AndroidJUnit4.class)
public class TimeCalibrateTest extends BaseSdkTest {

    private static final String APP_ID = "at_time_001";
    private static final String CALIBRATED_TIME = "2030-06-15 10:30:00.000";
    private static final long MAX_EVENT_TIME_DRIFT_MS = 5_000L;

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
        resetTimeCalibration();
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        prepareBetweenTestsFacade(context, APP_ID, server, sdk, 10);
        resetTimeCalibration();
    }

    @After
    public void tearDown() throws Exception {
        resetTimeCalibration();
        cleanupAfterTest(server);
    }

    @Test
    public void calibrateTime_eventTime_matchesCalibratedTimestamp() throws Exception {
        long calibratedMs = parseEventTimeMillis(CALIBRATED_TIME);
        ThinkingAnalyticsSDK.calibrateTime(calibratedMs);

        trackAndWaitFacade("calibrated_timestamp_event", null);
        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
        long eventMs = parseEventTimeMillis(events.getJSONObject(0).getString("#time"));

        assertTrue("Event #time must follow calibrateTime() timestamp",
                Math.abs(eventMs - calibratedMs) <= MAX_EVENT_TIME_DRIFT_MS);
    }
}
