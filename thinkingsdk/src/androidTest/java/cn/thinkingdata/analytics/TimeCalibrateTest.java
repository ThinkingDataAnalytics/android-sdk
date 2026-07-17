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

import cn.thinkingdata.analytics.utils.TDConstants;

import java.text.SimpleDateFormat;
import java.util.Locale;

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
        enqueueResponses(server, 10);
        resetFacadeSdkState();
        resetTimeCalibration();
        clearDbEvents(context, APP_ID);
        Thread.sleep(400);
        drainPendingRequests(server);
    }

    @After
    public void tearDown() {
        resetTimeCalibration();
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

    @Test
    public void facade_calibrateTime_eventTime_matchesCalibratedTimestamp() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
        long calibratedMs = sdf.parse("2028-03-20 08:15:30.123").getTime();
        TDAnalytics.calibrateTime(calibratedMs);

        trackAndWaitFacade("facade_calibrated_event", null);
        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
        long eventMs = parseEventTimeMillis(events.getJSONObject(0).getString("#time"));

        assertTrue("TDAnalytics.calibrateTime() must affect uploaded #time",
                Math.abs(eventMs - calibratedMs) <= MAX_EVENT_TIME_DRIFT_MS);
    }

    @Test
    public void calibrateTime_invalidTimestamp_isIgnored() throws Exception {
        long beforeTrack = System.currentTimeMillis();
        ThinkingAnalyticsSDK.calibrateTime(0);
        ThinkingAnalyticsSDK.calibrateTime(-1);

        trackAndWaitFacade("invalid_calibrate_event", null);
        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
        long eventMs = parseEventTimeMillis(events.getJSONObject(0).getString("#time"));

        assertTrue("Invalid calibrateTime() must not shift event time far from wall clock",
                Math.abs(eventMs - beforeTrack) <= 15_000L);
    }

    @Test(timeout = 15_000)
    public void calibrateTimeWithNtp_producesValidEventTime() throws Exception {
        TDAnalytics.calibrateTimeWithNtp("time.apple.com");

        Thread.sleep(4_000);
        trackAndWaitFacade("ntp_calibrated_event", null);
        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));

        String eventTime = events.getJSONObject(0).getString("#time");
        assertTrue("NTP-calibrated event #time must match expected format",
                eventTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));

        long eventMs = parseEventTimeMillis(eventTime);
        assertTrue("NTP-calibrated event time must stay near current wall clock",
                Math.abs(eventMs - System.currentTimeMillis()) <= 60_000L);
    }
}
