/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;
import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.thinkingdata.analytics.encrypt.TDEncryptUtils;
import cn.thinkingdata.analytics.persistence.ConfigStoragePlugin;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented tests for gating report-data encryption on the receiver's AES-GCM support,
 * declared through the remote config field {@code secret_key.mode}.
 *
 * <p>Each test uses its own appId and its own MockWebServer so the remote config request can be
 * answered differently per scenario. A custom {@link Dispatcher} serves both {@code /config} and
 * {@code /sync}, and records the upload requests itself instead of relying on
 * {@code MockWebServer#takeRequest}.
 */
@RunWith(AndroidJUnit4.class)
public class EncryptGcmModeTest extends BaseSdkTest {

    private static Context context;
    private static String testPublicKey;

    private MockWebServer server;
    private final BlockingQueue<RecordedRequest> syncRequests = new LinkedBlockingQueue<>();
    private volatile String configResponseBody;
    private volatile int configResponseCode = 200;
    private String appId;

    @BeforeClass
    public static void classSetUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        KeyPair keyPair = kpg.generateKeyPair();
        testPublicKey = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.NO_WRAP);
    }

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path != null && path.startsWith("/config")) {
                    MockResponse response = new MockResponse().setResponseCode(configResponseCode);
                    if (configResponseBody != null) {
                        response.setBody(configResponseBody);
                    }
                    return response;
                }
                syncRequests.add(request);
                return new MockResponse().setResponseCode(200).setBody("{\"code\":0}");
            }
        });
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        if (appId != null) {
            clearSdkData(context, appId);
        }
        server.shutdown();
        syncRequests.clear();
    }

    // ── first launch ──────────────────────────────────────────────────────────

    @Test
    public void firstLaunch_configReturnsModeGcm_dataEncryptedAndFlagPersisted() throws Exception {
        appId = "at_gcm_supported_001";
        configResponseBody = configBody(secretKeyJson("gcm"));

        ThinkingAnalyticsSDK sdk = initSdkForScenario(false);

        assertEquals("mode=gcm must persist as supported",
                ConfigStoragePlugin.GCM_SUPPORTED, readGcmSupport(context, appId));

        trackAndWait(sdk, "gcm_supported_event", null);
        RecordedRequest request = flushAndTakeSyncRequest(sdk);
        JSONObject envelope = firstItem(request);

        assertTrue("payload must be encrypted", TDEncryptUtils.isEncryptedData(envelope.toString()));
        assertEquals("gcm", envelope.getString("mode"));
        assertFalse(envelope.getString("payload").isEmpty());
        assertEquals("1", request.getHeader("TA-Datas-Type"));
    }

    @Test
    public void firstLaunch_configWithoutMode_dataStaysPlaintextAndFlagPersisted() throws Exception {
        appId = "at_gcm_unsupported_001";
        // Old receiver: secret_key is delivered but carries no mode field.
        configResponseBody = configBody(secretKeyJson(null));

        ThinkingAnalyticsSDK sdk = initSdkForScenario(false);

        assertEquals("missing mode must persist as unsupported",
                ConfigStoragePlugin.GCM_UNSUPPORTED, readGcmSupport(context, appId));

        trackAndWait(sdk, "gcm_unsupported_event", null);
        RecordedRequest request = flushAndTakeSyncRequest(sdk);
        JSONObject event = firstItem(request);

        assertFalse("payload must not be encrypted",
                TDEncryptUtils.isEncryptedData(event.toString()));
        assertEquals("gcm_unsupported_event", event.getString("#event_name"));
        assertEquals("0", request.getHeader("TA-Datas-Type"));
    }

    @Test
    public void firstLaunch_configWithoutSecretKey_dataStaysPlaintext() throws Exception {
        appId = "at_gcm_no_secret_001";
        configResponseBody = configBody(null);

        ThinkingAnalyticsSDK sdk = initSdkForScenario(false);

        assertEquals(ConfigStoragePlugin.GCM_UNSUPPORTED, readGcmSupport(context, appId));

        trackAndWait(sdk, "gcm_no_secret_event", null);
        JSONObject event = firstItem(flushAndTakeSyncRequest(sdk));
        assertEquals("gcm_no_secret_event", event.getString("#event_name"));
    }

    // ── later launches ────────────────────────────────────────────────────────

    @Test
    public void secondLaunch_persistedFlag_encryptsWithoutWaitingForConfig() throws Exception {
        appId = "at_gcm_persisted_001";
        // Config request fails, so the decision can only come from the persisted flag.
        configResponseCode = 500;
        configResponseBody = null;

        clearSdkData(context, appId);
        seedGcmSupport(context, appId, ConfigStoragePlugin.GCM_SUPPORTED);
        ThinkingAnalyticsSDK sdk = initSdkForScenario(false, false);

        assertEquals("a failed config request must not overwrite the persisted flag",
                ConfigStoragePlugin.GCM_SUPPORTED, readGcmSupport(context, appId));

        trackAndWait(sdk, "gcm_persisted_event", null);
        RecordedRequest request = flushAndTakeSyncRequest(sdk);
        JSONObject envelope = firstItem(request);

        assertTrue(TDEncryptUtils.isEncryptedData(envelope.toString()));
        assertEquals("gcm", envelope.getString("mode"));
        assertEquals("1", request.getHeader("TA-Datas-Type"));
    }

    @Test
    public void secondLaunch_persistedUnsupportedFlag_staysPlaintext() throws Exception {
        appId = "at_gcm_persisted_off_001";
        configResponseCode = 500;
        configResponseBody = null;

        clearSdkData(context, appId);
        seedGcmSupport(context, appId, ConfigStoragePlugin.GCM_UNSUPPORTED);
        ThinkingAnalyticsSDK sdk = initSdkForScenario(false, false);

        trackAndWait(sdk, "gcm_persisted_off_event", null);
        JSONObject event = firstItem(flushAndTakeSyncRequest(sdk));

        assertFalse(TDEncryptUtils.isEncryptedData(event.toString()));
        assertEquals("gcm_persisted_off_event", event.getString("#event_name"));
    }

    // ── remote config disabled ────────────────────────────────────────────────

    @Test
    public void remoteConfigDisabled_manualKey_staysPlaintext() throws Exception {
        appId = "at_gcm_rconfig_off_001";
        configResponseBody = configBody(secretKeyJson("gcm"));

        ThinkingAnalyticsSDK sdk = initSdkForScenario(true);

        trackAndWait(sdk, "gcm_rconfig_off_event", null);
        JSONObject event = firstItem(flushAndTakeSyncRequest(sdk));

        assertFalse("without remote config GCM support is unknown, so data must stay plaintext",
                TDEncryptUtils.isEncryptedData(event.toString()));
        assertEquals("gcm_rconfig_off_event", event.getString("#event_name"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ThinkingAnalyticsSDK initSdkForScenario(boolean disableRConfig) throws Exception {
        return initSdkForScenario(disableRConfig, true);
    }

    private ThinkingAnalyticsSDK initSdkForScenario(boolean disableRConfig, boolean clearStorage)
            throws Exception {
        if (clearStorage) {
            clearSdkData(context, appId);
        }
        removeSdkInstance(context, appId);
        removeEncryptInstance(appId);
        removeConfigInstance(context, appId);

        TDConfig config = TDConfig.getInstance(context, appId, server.url("/").toString());
        config.disableRConfig = disableRConfig;
        config.enableEncrypt(1, testPublicKey);
        ThinkingAnalyticsSDK sdk = ThinkingAnalyticsSDK.sharedInstance(config);
        // Wait for async init plus the remote config round trip.
        Thread.sleep(2000);
        waitForTrackTasks();
        clearDbEvents(context, appId);
        syncRequests.clear();
        return sdk;
    }

    private RecordedRequest flushAndTakeSyncRequest(ThinkingAnalyticsSDK sdk) throws Exception {
        sdk.flush();
        waitForTrackTasks();
        RecordedRequest request = syncRequests.poll(5, TimeUnit.SECONDS);
        assertNotNull("SDK must upload the tracked event after flush()", request);
        return request;
    }

    private JSONObject firstItem(RecordedRequest request) throws Exception {
        JSONArray data = decodeRequestBody(request);
        assertTrue("upload batch must not be empty", data.length() > 0);
        return data.getJSONObject(0);
    }

    private static String secretKeyJson(String mode) {
        return "{" + (mode == null ? "" : "\"mode\":\"" + mode + "\",")
                + "\"key\":\"" + testPublicKey + "\","
                + "\"version\":1,\"symmetric\":\"AES\",\"asymmetric\":\"RSA\"}";
    }

    private static String configBody(String secretKeyJson) {
        return "{\"code\":\"0\",\"data\":{\"sync_interval\":15,\"sync_batch_size\":20"
                + (secretKeyJson == null ? "" : ",\"secret_key\":" + secretKeyJson)
                + "}}";
    }
}
