/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

import cn.thinkingdata.analytics.utils.TDConstants;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Verifies {@code ta_app_install} auto-track enablement and upload payload.
 *
 * <p>The SDK gates real install emission on {@code firstInstallTime == lastUpdateTime}, which is
 * often false for reinstalled androidTest APKs. Enablement is still asserted, and the install
 * event shape is verified via {@link ThinkingAnalyticsSDK#autoTrack}.
 */
@RunWith(AndroidJUnit4.class)
public class AutoTrackInstallTest extends BaseSdkTest {

    private static final String APP_ID = "at_autotrack_install_001";
    private static Context context;
    private static MockWebServer server;
    private static ThinkingAnalyticsSDK sdk;

    @BeforeClass
    public static void classSetUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        server = new MockWebServer();
        server.start();
        clearSdkData(context, APP_ID);
        clearGlobalInstallState(context);
        resetFirstInstallFlags();
        sdk = initFacadeSdk(context, APP_ID, server);
        enqueueResponses(server, 10);
        TDAnalytics.enableAutoTrack(TDAnalytics.TDAutoTrackEventType.APP_INSTALL);
        Thread.sleep(800);
        drainPendingRequests(server);
    }

    @AfterClass
    public static void classTearDown() throws Exception {
        server.shutdown();
    }

    @Test(timeout = 15_000)
    public void autoTrack_appInstall_typeIsEnabled() {
        assertTrue(sdk.isAutoTrackEnabled());
        assertFalse(sdk.isAutoTrackEventTypeIgnored(
                ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL));
    }

    @Test(timeout = 20_000)
    public void autoTrack_appInstall_eventUploaded() throws Exception {
        clearDbEvents(context, APP_ID);
        enqueueResponses(server, 5);
        sdk.autoTrack(TDConstants.APP_INSTALL_EVENT_NAME, new JSONObject());
        Thread.sleep(500);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject installEvent = findEventByName(events, TDConstants.APP_INSTALL_EVENT_NAME);
        assertNotNull("ta_app_install must appear in upload payload", installEvent);
        assertEquals("track", installEvent.getString("#type"));
    }

    private static void resetFirstInstallFlags() throws Exception {
        Field firstInstall = ThinkingAnalyticsSDK.class.getDeclaredField("isFirstInstall");
        firstInstall.setAccessible(true);
        firstInstall.setBoolean(null, false);
    }
}
