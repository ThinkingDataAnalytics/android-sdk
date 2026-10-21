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
        prepareBetweenTests(context, APP_ID, server, sdk, 5);
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    /**
     * Critical customer path: call getters immediately after {@code sharedInstance},
     * without waiting for TrackTaskManager async init to finish.
     */
    @Test
    public void init_immediateGet_visitorAccountAndPresetProperties() throws Exception {
        String appId = APP_ID + "_immediate";
        clearSdkData(context, appId);
        removeSdkInstance(context, appId);
        TDConfig config = TDConfig.getInstance(context, appId, server.url("/").toString());
        config.disableRConfig = true;

        // No Thread.sleep — exercise the "init then immediately read" race window.
        ThinkingAnalyticsSDK freshSdk = ThinkingAnalyticsSDK.sharedInstance(config);
        assertNotNull(freshSdk);

        String visitorId = freshSdk.getDistinctId();
        String accountId = freshSdk.getLoginId();
        TDPresetProperties preset = freshSdk.getPresetProperties();

        assertNotNull("visitor (distinctId) must be readable right after init", visitorId);
        assertFalse("visitor (distinctId) must not be empty right after init", visitorId.isEmpty());
        assertEquals("visitor id must be stable on immediate re-read",
                visitorId, freshSdk.getDistinctId());
        assertTrue("account (loginId) must be empty before login, even if read immediately",
                accountId == null || accountId.isEmpty());
        assertNotNull("preset properties must be readable right after init", preset);
        assertFalse("preset #device_id must not be empty right after init",
                preset.deviceId == null || preset.deviceId.isEmpty());
        assertTrue("preset #screen_width must be positive right after init",
                preset.screenWidth > 0);
        assertTrue("preset #screen_height must be positive right after init",
                preset.screenHeight > 0);
        assertFalse("preset #os must not be empty right after init",
                preset.os == null || preset.os.isEmpty());
        org.json.JSONObject superProps = freshSdk.getSuperProperties();
        assertTrue("initial super properties must be empty",
                superProps == null || superProps.length() == 0);
    }

    /**
     * After login/identify, getters must return the exact values immediately
     * (no wait for async storage write).
     */
    @Test
    public void loginAndIdentify_immediateGet_returnsExactValues() throws Exception {
        final String expectedAccount = "exact_account_001";
        final String expectedVisitor = "exact_visitor_001";

        sdk.login(expectedAccount);
        sdk.identify(expectedVisitor);

        assertEquals("account must match login() immediately",
                expectedAccount, sdk.getLoginId());
        assertEquals("visitor must match identify() immediately",
                expectedVisitor, sdk.getDistinctId());
        // Re-read to ensure no race flip-flop
        assertEquals(expectedAccount, sdk.getLoginId());
        assertEquals(expectedVisitor, sdk.getDistinctId());
    }

    /**
     * Simulate cold start: persisted visitor/account must be restored accurately
     * when getters are called immediately after a new {@code sharedInstance}.
     */
    @Test
    public void init_immediateGet_restoredVisitorAndAccount_matchPersisted() throws Exception {
        String appId = APP_ID + "_restore";
        final String expectedVisitor = "persisted_visitor_xyz";
        final String expectedAccount = "persisted_account_xyz";

        clearSdkData(context, appId);
        removeSdkInstance(context, appId);
        writeLegacyPlaintextStorage(context, appId, expectedVisitor, expectedAccount,
                new org.json.JSONObject());

        TDConfig config = TDConfig.getInstance(context, appId, server.url("/").toString());
        config.disableRConfig = true;
        ThinkingAnalyticsSDK restored = ThinkingAnalyticsSDK.sharedInstance(config);

        // Immediate read — do not wait for async init
        assertEquals("restored visitor must match persisted identifyID",
                expectedVisitor, restored.getDistinctId());
        assertEquals("restored account must match persisted loginID",
                expectedAccount, restored.getLoginId());
    }

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
