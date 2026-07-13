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
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertNotNull;

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
 */
public abstract class BaseSdkTest {

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

    // ── per-test action helpers ───────────────────────────────────────────────

    protected static void trackAndWait(ThinkingAnalyticsSDK sdk,
                                       String event, JSONObject props)
            throws InterruptedException {
        if (props != null) {
            sdk.track(event, props);
        } else {
            sdk.track(event);
        }
        Thread.sleep(300);
    }

    protected static RecordedRequest flushAndAwaitRequest(ThinkingAnalyticsSDK sdk,
                                                          MockWebServer srv)
            throws Exception {
        sdk.flush();
        return srv.takeRequest(5, TimeUnit.SECONDS);
    }

    protected static RecordedRequest flushAndAssertRequest(ThinkingAnalyticsSDK sdk,
                                                           MockWebServer srv)
            throws Exception {
        RecordedRequest req = flushAndAwaitRequest(sdk, srv);
        assertNotNull("SDK must send an HTTP request after flush()", req);
        return req;
    }

    /**
     * Discard any HTTP requests already sitting in the MockWebServer queue.
     * Call after {@link #resetSdkState} (which sets NORMAL and triggers an async flush)
     * and before tests that assert no request was sent.
     */
    protected static void drainPendingRequests(MockWebServer srv) throws Exception {
        RecordedRequest req;
        while ((req = srv.takeRequest(200, TimeUnit.MILLISECONDS)) != null) {
            // discard stale requests from prior tests or async flushes
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
    }

    /**
     * Reset mutable SDK state between tests. Does NOT touch server or config.
     */
    protected static void resetSdkState(ThinkingAnalyticsSDK sdk) throws InterruptedException {
        sdk.logout();
        sdk.clearSuperProperties();
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        Thread.sleep(200);
    }
}
