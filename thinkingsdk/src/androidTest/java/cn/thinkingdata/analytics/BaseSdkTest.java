/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import cn.thinkingdata.analytics.encrypt.ThinkingDataEncrypt;
import cn.thinkingdata.analytics.persistence.CommonStoragePlugin;
import cn.thinkingdata.analytics.persistence.ConfigStoragePlugin;
import cn.thinkingdata.analytics.persistence.LocalStorageType;
import cn.thinkingdata.analytics.tasks.TrackTaskManager;
import cn.thinkingdata.analytics.utils.TDConstants;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Utility helpers for SDK instrumented tests.
 *
 * ROOT-CAUSE NOTE — singleton URL trap
 * ─────────────────────────────────────
 * TDConfig.getInstance(context, appId, url) and ThinkingAnalyticsSDK.sharedInstance(config)
 * both return cached singletons keyed on (context, appId). After the first @Before call the
 * SDK is pinned to that first MockWebServer port; subsequent @Before calls spin up a new
 * MockWebServer on a different port, but the SDK keeps sending to the old one.
 *
     * FIX → each subclass uses @BeforeClass / @AfterClass (static) to start the server ONCE
     * and init the SDK ONCE.  Between tests a normal @Before just re-enqueues fresh 200-OK
     * responses and resets SDK state.  This class provides the shared helper methods that
     * subclasses call from their own static setup.
     *
     * TDAnalytics.instance trap
     * ─────────────────────────
     * {@link TDAnalytics#init(TDConfig)} only assigns {@code instance} once (first init wins).
     * Later test classes that call facade static methods would otherwise talk to the wrong SDK
     * and flush to another class's MockWebServer. Use {@link #initFacadeSdk} which re-binds
     * {@code TDAnalytics.instance} to the current appId's SDK.
     */
public abstract class BaseSdkTest {

    /** Quiet period with no MockWebServer traffic before treating the queue as drained. */
    private static final long DRAIN_QUIET_MS = 250;
    /** Upper bound for {@link #drainPendingRequests}. */
    private static final long DRAIN_MAX_MS = 3_000;
    /** Timeout for {@link #waitForTrackTasks()}. */
    private static final long TRACK_TASK_TIMEOUT_MS = 10_000;

    /** Baseline distinct_id captured at {@link #initSdk} / {@link #initFacadeSdk} per appId. */
    private static final Map<String, String> sBaselineDistinctIds = new HashMap<>();

    // ── class-level setup helpers ─────────────────────────────────────────────

    /**
     * Start a MockWebServer, clear old SDK data, build and return the SDK instance.
     * Call from a {@code @BeforeClass} method in each subclass.
     */
    protected static ThinkingAnalyticsSDK initSdk(Context ctx, String appId,
                                                   MockWebServer srv) throws Exception {
        clearSdkData(ctx, appId);
        TDConfig config = TDConfig.getInstance(ctx, appId, srv.url("/").toString());
        config.disableRConfig = true;
        ThinkingAnalyticsSDK sdk = ThinkingAnalyticsSDK.sharedInstance(config);
        Thread.sleep(1000); // wait for async init (distinctId, DataHandle, etc.)
        sBaselineDistinctIds.put(appId, sdk.getDistinctId());
        return sdk;
    }

    /**
     * Initialize SDK through {@link TDAnalytics#init(TDConfig)} so facade static methods work.
     */
    protected static ThinkingAnalyticsSDK initFacadeSdk(Context ctx, String appId,
                                                        MockWebServer srv) throws Exception {
        clearSdkData(ctx, appId);
        TDConfig config = TDConfig.getInstance(ctx, appId, srv.url("/").toString());
        config.disableRConfig = true;
        TDAnalytics.init(config);
        ThinkingAnalyticsSDK sdk = ThinkingAnalyticsSDK.sharedInstance(config);
        TDAnalytics.instance = sdk;
        Thread.sleep(1000);
        sBaselineDistinctIds.put(appId, sdk.getDistinctId());
        return sdk;
    }

    /**
     * Enqueue {@code count} default 200-OK responses into the server.
     * Call from a {@code @Before} method before each test that expects network traffic.
     */
    protected static void enqueueResponses(MockWebServer srv, int count) {
        for (int i = 0; i < count; i++) {
            srv.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody("{\"code\":0}"));
        }
    }

    // ── per-test isolation ────────────────────────────────────────────────────

    /**
     * Block until all tasks already queued on {@link TrackTaskManager} have finished.
     * The manager uses a single worker thread, so a marker task runs only after prior work.
     */
    protected static void waitForTrackTasks() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        if (!latch.await(TRACK_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            throw new InterruptedException(
                    "TrackTaskManager queue did not drain within " + TRACK_TASK_TIMEOUT_MS + "ms");
        }
    }

    /**
     * Restore the distinct_id captured when the SDK was first initialized for {@code appId}.
     */
    protected static void restoreBaselineDistinctId(ThinkingAnalyticsSDK sdk, String appId) {
        String baseline = sBaselineDistinctIds.get(appId);
        if (baseline != null) {
            sdk.identify(baseline);
        }
    }

    /**
     * Standard {@code @Before} for tests that assert HTTP uploads via MockWebServer.
     * Clears DB, re-enqueues mock responses, resets SDK state, restores distinct_id,
     * waits for async work, then drains stale requests.
     */
    protected static void prepareBetweenTests(Context ctx, String appId, MockWebServer srv,
                                              ThinkingAnalyticsSDK sdk, int responseCount)
            throws Exception {
        clearDbEvents(ctx, appId);
        enqueueResponses(srv, responseCount);
        resetSdkState(sdk);
        restoreBaselineDistinctId(sdk, appId);
        waitForTrackTasks();
        // Allow SaveMessageWorker / SendMessageWorker to finish any auto-flush from NORMAL.
        Thread.sleep(200);
        drainPendingRequests(srv);
    }

    /**
     * Same as {@link #prepareBetweenTests} but resets state through {@link TDAnalytics} facade APIs.
     */
    protected static void prepareBetweenTestsFacade(Context ctx, String appId, MockWebServer srv,
                                                    ThinkingAnalyticsSDK sdk, int responseCount)
            throws Exception {
        clearDbEvents(ctx, appId);
        enqueueResponses(srv, responseCount);
        resetFacadeSdkState();
        restoreBaselineDistinctId(sdk, appId);
        waitForTrackTasks();
        Thread.sleep(200);
        drainPendingRequests(srv);
    }

    /**
     * Standard {@code @After} — wait for in-flight SDK work and discard upload requests
     * so the next test does not consume stale traffic.
     */
    protected static void cleanupAfterTest(MockWebServer srv) throws Exception {
        waitForTrackTasks();
        drainPendingRequests(srv);
    }

    protected static void loginAndWait(ThinkingAnalyticsSDK sdk, String loginId)
            throws InterruptedException {
        sdk.login(loginId);
        waitForTrackTasks();
    }

    protected static void identifyAndWait(ThinkingAnalyticsSDK sdk, String identity)
            throws InterruptedException {
        sdk.identify(identity);
        waitForTrackTasks();
    }

    // ── per-test action helpers ───────────────────────────────────────────────

    protected static void trackAndWait(ThinkingAnalyticsSDK sdk,
                                       String event, JSONObject props)
            throws InterruptedException {
        if (props != null) {
            sdk.track(event, props);
        } else {
            sdk.track(event);
        }
        waitForTrackTasks();
        // SaveMessageWorker persists asynchronously after TrackTaskManager work completes.
        Thread.sleep(150);
    }

    protected static void trackAndWaitFacade(String event, JSONObject props)
            throws InterruptedException {
        if (props != null) {
            TDAnalytics.track(event, props);
        } else {
            TDAnalytics.track(event);
        }
        waitForTrackTasks();
        Thread.sleep(150);
    }

    protected static RecordedRequest flushAndAwaitRequest(ThinkingAnalyticsSDK sdk,
                                                          MockWebServer srv)
            throws Exception {
        // Do NOT drain here: callers may already have a legitimate upload in-flight
        // (e.g. auto-flush on SAVE_ONLY → NORMAL). Draining would swallow that request.
        sdk.flush();
        waitForTrackTasks();
        // SaveMessageWorker → SendMessageWorker is async after TrackTaskManager.
        Thread.sleep(150);
        return srv.takeRequest(5, TimeUnit.SECONDS);
    }

    protected static RecordedRequest flushAndAssertRequest(ThinkingAnalyticsSDK sdk,
                                                           MockWebServer srv)
            throws Exception {
        RecordedRequest req = flushAndAwaitRequest(sdk, srv);
        assertNotNull("SDK must send an HTTP request after flush()", req);
        return req;
    }

    protected static RecordedRequest flushAndAssertFacade(MockWebServer srv) throws Exception {
        TDAnalytics.flush();
        waitForTrackTasks();
        Thread.sleep(150);
        RecordedRequest req = srv.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull("TDAnalytics.flush() must send an HTTP request", req);
        return req;
    }

    /**
     * Assert {@code flush()} does not produce an HTTP upload (empty queue).
     */
    protected static void assertNoUploadAfterFlush(ThinkingAnalyticsSDK sdk, MockWebServer srv)
            throws Exception {
        waitForTrackTasks();
        drainPendingRequests(srv);
        sdk.flush();
        waitForTrackTasks();
        Thread.sleep(150);
        RecordedRequest req = srv.takeRequest(2, TimeUnit.SECONDS);
        assertNull("flush() with no queued events must not send a request", req);
    }

    /**
     * Discard any HTTP requests already sitting in the MockWebServer queue.
     * Waits until no request arrives for {@link #DRAIN_QUIET_MS} before returning.
     */
    protected static void drainPendingRequests(MockWebServer srv) throws Exception {
        drainPendingRequests(srv, DRAIN_QUIET_MS, DRAIN_MAX_MS);
    }

    protected static void drainPendingRequests(MockWebServer srv, long quietPeriodMs,
                                               long maxWaitMs) throws Exception {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        long lastRequestAt = System.currentTimeMillis();
        while (System.currentTimeMillis() < deadline) {
            RecordedRequest req = srv.takeRequest(200, TimeUnit.MILLISECONDS);
            if (req != null) {
                lastRequestAt = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastRequestAt >= quietPeriodMs) {
                return;
            }
        }
    }

    // ── request body decoding ─────────────────────────────────────────────────

    /**
     * Decode the SDK upload body (Base64 → gzip → JSON) and return the event array.
     * Handles both bare JSONArray and {"data":[...]} wrapper formats.
     */
    protected static JSONArray decodeRequestBody(RecordedRequest request) throws Exception {
        String rawBody = request.getBody().readUtf8();

        String base64Part = rawBody;
        if (rawBody.startsWith("data=")) {
            base64Part = URLDecoder.decode(rawBody.substring(5), "UTF-8");
        }

        byte[] gzipped = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT);

        GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(gzipped));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = gzis.read(buf)) > 0) baos.write(buf, 0, len);
        gzis.close();
        String json = baos.toString("UTF-8");

        if (json.startsWith("[")) {
            return new JSONArray(json);
        } else {
            JSONObject wrapper = new JSONObject(json);
            if (wrapper.has("data")) {
                return wrapper.getJSONArray("data");
            }
            JSONArray arr = new JSONArray();
            arr.put(wrapper);
            return arr;
        }
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    protected static int countDbEvents(Context ctx, String appId) {
        try {
            File dbFile = ctx.getDatabasePath("thinkingdata");
            if (!dbFile.exists()) return 0;
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            android.database.Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM events WHERE token=?", new String[]{appId});
            int count = 0;
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
            db.close();
            return count;
        } catch (Exception e) {
            return -1;
        }
    }

    protected static void clearDbEvents(Context ctx, String appId) {
        try {
            File dbFile = ctx.getDatabasePath("thinkingdata");
            if (!dbFile.exists()) return;
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            db.delete("events", "token=?", new String[]{appId});
            db.close();
        } catch (Exception ignored) {}
    }

    protected static void clearSdkData(Context ctx, String appId) {
        // Config SharedPreferences
        ctx.getSharedPreferences(
                "cn.thinkingdata.android.config_" + appId, Context.MODE_PRIVATE)
                .edit().clear().commit();

        // Common storage (try multiple naming conventions)
        ctx.getSharedPreferences(
                "cn.thinkingdata.android_" + appId, Context.MODE_PRIVATE)
                .edit().clear().commit();
        ctx.getSharedPreferences(
                "cn.thinkingdata.android." + appId, Context.MODE_PRIVATE)
                .edit().clear().commit();

        // Scan shared_prefs dir for anything containing appId
        File spDir = new File(ctx.getApplicationInfo().dataDir, "shared_prefs");
        if (spDir.exists()) {
            File[] files = spDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().contains(appId)) {
                        String name = f.getName().replace(".xml", "");
                        ctx.getSharedPreferences(name, Context.MODE_PRIVATE)
                                .edit().clear().commit();
                    }
                }
            }
        }

        clearDbEvents(ctx, appId);
        ctx.getSharedPreferences(storagePreferenceName(appId), Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    /**
     * Drop a cached SDK singleton so the next {@code sharedInstance} rebuilds from storage.
     * Used to simulate process restart within one instrumented test process.
     */
    @SuppressWarnings("unchecked")
    protected static void removeSdkInstance(Context ctx, String appId) throws Exception {
        java.lang.reflect.Field field =
                ThinkingAnalyticsSDK.class.getDeclaredField("sInstanceMap");
        field.setAccessible(true);
        Map<Context, Map<String, ThinkingAnalyticsSDK>> map =
                (Map<Context, Map<String, ThinkingAnalyticsSDK>>) field.get(null);
        Context appContext = ctx.getApplicationContext();
        synchronized (map) {
            Map<String, ThinkingAnalyticsSDK> instances = map.get(appContext);
            if (instances != null) {
                instances.remove(appId);
            }
        }
    }

    protected static String storagePreferenceName(String appId) {
        return "com.thinkingdata.analyse_" + appId;
    }

    protected static String configPreferenceName(String appId) {
        return "cn.thinkingdata.android.config_" + appId;
    }

    /**
     * Drop the cached {@link ThinkingDataEncrypt} so the next SDK init rebuilds it.
     */
    @SuppressWarnings("unchecked")
    protected static void removeEncryptInstance(String token) throws Exception {
        java.lang.reflect.Field field =
                ThinkingDataEncrypt.class.getDeclaredField("sInstances");
        field.setAccessible(true);
        Map<String, ThinkingDataEncrypt> map =
                (Map<String, ThinkingDataEncrypt>) field.get(null);
        synchronized (map) {
            map.remove(token);
        }
    }

    /**
     * Drop the cached {@link TDConfig} so the next {@code getInstance} rebuilds it, which is the
     * only way to re-run the remote config request within one instrumented test process.
     */
    @SuppressWarnings("unchecked")
    protected static void removeConfigInstance(Context ctx, String token) throws Exception {
        java.lang.reflect.Field field = TDConfig.class.getDeclaredField("sInstances");
        field.setAccessible(true);
        Map<Context, Map<String, TDConfig>> map =
                (Map<Context, Map<String, TDConfig>>) field.get(null);
        Context appContext = ctx.getApplicationContext();
        synchronized (map) {
            Map<String, TDConfig> instances = map.get(appContext);
            if (instances != null) {
                instances.remove(token);
            }
        }
    }

    /**
     * Pre-populate the persisted AES-GCM support flag, simulating a launch after the remote
     * config has already been fetched once.
     */
    protected static void seedGcmSupport(Context ctx, String appId, int state) {
        ctx.getSharedPreferences(configPreferenceName(appId), Context.MODE_PRIVATE)
                .edit()
                .putInt("encryptSupportGcm", state)
                .commit();
    }

    /**
     * Read the persisted AES-GCM support flag through a fresh storage plugin, so the assertion
     * also covers the case where local storage encryption is enabled.
     */
    protected static int readGcmSupport(Context ctx, String appId) {
        return new ConfigStoragePlugin(ctx, appId)
                .get(LocalStorageType.ENCRYPT_SUPPORT_GCM, ConfigStoragePlugin.GCM_UNKNOWN);
    }

    protected static void writeLegacyPlaintextStorage(Context ctx, String appId,
                                                      String identifyId, String loginId,
                                                      JSONObject superProperties) {
        ctx.getSharedPreferences(storagePreferenceName(appId), Context.MODE_PRIVATE)
                .edit()
                .putString("identifyID", identifyId)
                .putString("loginID", loginId)
                .putString("superProperties", superProperties.toString())
                .commit();
    }

    protected static String readRawStorageValue(Context ctx, String appId, String key) {
        return ctx.getSharedPreferences(storagePreferenceName(appId), Context.MODE_PRIVATE)
                .getString(key, null);
    }

    protected static String reloadStorageValue(Context ctx, String appId, int storageType,
                                                 String defaultValue) {
        CommonStoragePlugin plugin = new CommonStoragePlugin(ctx, appId);
        return plugin.get(storageType, defaultValue);
    }

    protected static long parseEventTimeMillis(String timeStr) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
        return sdf.parse(timeStr).getTime();
    }

    protected static void resetTimeCalibration() {
        ThinkingAnalyticsSDK.calibrateTime(System.currentTimeMillis());
    }

    protected static ThinkingAnalyticsSDK initSdkPreserveStorage(Context ctx, String appId,
                                                                 MockWebServer srv) throws Exception {
        TDConfig config = TDConfig.getInstance(ctx, appId, srv.url("/").toString());
        config.disableRConfig = true;
        ThinkingAnalyticsSDK sdk = ThinkingAnalyticsSDK.sharedInstance(config);
        Thread.sleep(1000);
        return sdk;
    }

    /**
     * Reset mutable SDK state between tests. Does NOT touch server or config.
     */
    protected static void resetSdkState(ThinkingAnalyticsSDK sdk) throws InterruptedException {
        sdk.logout();
        sdk.clearSuperProperties();
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
    }

    protected static void resetFacadeSdkState() throws InterruptedException {
        TDAnalytics.logout();
        TDAnalytics.clearSuperProperties();
        TDAnalytics.setTrackStatus(TDAnalytics.TDTrackStatus.NORMAL);
    }

    protected static void clearPresetDisableList() {
        TDPresetProperties.initDisableList(new String[0]);
    }

    protected static void clearGlobalInstallState(Context ctx) {
        ctx.getSharedPreferences("com.thinkingdata.analyse", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    protected static JSONObject findEventByName(JSONArray events, String name) throws Exception {
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            if (name.equals(event.optString("#event_name"))) {
                return event;
            }
        }
        return null;
    }
}
