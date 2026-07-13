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

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SdkInitTest extends BaseSdkTest {

    private static final String APP_ID = "at_init_001";
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
        enqueueResponses(server, 5);
        resetSdkState(sdk);
        clearDbEvents(context, APP_ID);
    }

    @After
    public void tearDown() { /* nothing extra */ }

    // ── distinctId ─────────────────────────────────────────────────────────────

    @Test
    public void init_distinctId_isNonEmpty() {
        String distinctId = sdk.getDistinctId();
        assertNotNull("distinctId must not be null after init", distinctId);
        assertFalse("distinctId must not be empty after init", distinctId.isEmpty());
    }

    @Test
    public void init_distinctId_isStable() throws InterruptedException {
        String id1 = sdk.getDistinctId();
        Thread.sleep(200);
        String id2 = sdk.getDistinctId();
        assertEquals("distinctId must be stable between calls", id1, id2);
    }

    // ── loginId ───────────────────────────────────────────────────────────────

    @Test
    public void init_loginId_isEmptyBeforeLogin() {
        String loginId = sdk.getLoginId();
        assertTrue("loginId must be empty before login()",
                loginId == null || loginId.isEmpty());
    }

    // ── preset properties ─────────────────────────────────────────────────────

    @Test
    public void init_presetProperties_isNotNull() {
        TDPresetProperties presetProps = sdk.getPresetProperties();
        assertNotNull("getPresetProperties() must return non-null object", presetProps);
    }

    @Test
    public void init_presetProperties_deviceId_isNonEmpty() {
        TDPresetProperties presetProps = sdk.getPresetProperties();
        assertNotNull(presetProps);
        String deviceId = presetProps.deviceId;
        assertNotNull("device_id preset property must not be null", deviceId);
        assertFalse("device_id preset property must not be empty", deviceId.isEmpty());
    }

    // ── super properties initial state ────────────────────────────────────────

    @Test
    public void init_superProperties_emptyBeforeSet() {
        org.json.JSONObject superProps = sdk.getSuperProperties();
        assertTrue("superProperties must be empty before any setSuperProperties() call",
                superProps == null || superProps.length() == 0);
    }

    // ── multiple instances ────────────────────────────────────────────────────

    @Test
    public void init_twoAppIds_independentInstances() throws Exception {
        String appId2 = APP_ID + "_b";
        clearSdkData(context, appId2);
        TDConfig config2 = TDConfig.getInstance(context, appId2, server.url("/").toString());
        config2.disableRConfig = true;
        ThinkingAnalyticsSDK sdk2 = ThinkingAnalyticsSDK.sharedInstance(config2);
        assertNotNull("Second SDK instance must be non-null", sdk2);
        assertNotSame("Two instances with different appIds must be different objects", sdk, sdk2);
    }

    @Test
    public void init_sameAppId_returnsSameInstance() {
        TDConfig config2 = TDConfig.getInstance(context, APP_ID, server.url("/").toString());
        ThinkingAnalyticsSDK sdk2 = ThinkingAnalyticsSDK.sharedInstance(config2);
        assertSame("sharedInstance() for the same appId must return the same object", sdk, sdk2);
    }
}
