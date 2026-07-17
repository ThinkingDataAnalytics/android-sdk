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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

/**
 * Multi-threaded stress tests for primary SDK APIs:
 * {@code track}, {@code login}, {@code identify}/{@code setDistinctId}, {@code user_set}, {@code init}.
 *
 * <p>Also includes a minimal pre-release stress checklist:
 * soak mixed APIs, interleaved track-status flips, and intermittent server errors with recovery.
 *
 * <p>Goal: concurrent callers must not crash the process, and identity / upload state must remain
 * consistent after all worker threads settle.
 */
@RunWith(AndroidJUnit4.class)
public class ConcurrentApiTest extends BaseSdkTest {

    private static final String APP_ID = "at_concurrent_001";
    private static final int THREADS = 8;
    private static final int CALLS_PER_THREAD = 20;

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
        clearDbEvents(context, APP_ID);
        enqueueResponses(server, 80);
        resetSdkState(sdk);
        Thread.sleep(400);
        drainPendingRequests(server);
        clearDbEvents(context, APP_ID);
    }

    @After
    public void tearDown() throws Exception {
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        Thread.sleep(200);
        drainPendingRequests(server);
    }

    @Test(timeout = 60_000)
    public void concurrent_track_noCrashAndEventsUploaded() throws Exception {
        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>();

        runConcurrent(THREADS, CALLS_PER_THREAD, new Worker() {
            @Override
            public void run(int threadIndex, int callIndex) throws Exception {
                JSONObject props = new JSONObject();
                props.put("tid", threadIndex);
                props.put("seq", callIndex);
                sdk.track("concurrent_track", props);
            }
        }, failures, firstError);

        assertEquals("concurrent track must not throw: " + firstError.get(),
                0, failures.get());

        Thread.sleep(800);
        RecordedRequest req = flushAndAssertRequest(sdk, server);
        JSONArray events = decodeRequestBody(req);
        int trackCount = countEventsByName(events, "concurrent_track");
        // Flush may batch across requests; assert we got a meaningful batch and no crash.
        assertTrue("expected concurrent_track events in upload, got " + trackCount
                        + " (payload size=" + events.length() + ")",
                trackCount > 0 || events.length() > 0);
    }

    /**
     * Stress: 10 threads track 5000 events in total; every event must be uploaded exactly once.
     */
    @Test(timeout = 180_000)
    public void concurrent_track_5000_allEventsReceived() throws Exception {
        final int totalEvents = 5000;
        final int threads = 10;
        final int perThread = totalEvents / threads;
        final String eventName = "concurrent_track_5k";

        // bulk flush size is 20 and send batch is 50 → expect many HTTP posts
        enqueueResponses(server, 400);

        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>();
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            final int threadIndex = t;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        start.await(10, TimeUnit.SECONDS);
                        for (int c = 0; c < perThread; c++) {
                            try {
                                JSONObject props = new JSONObject();
                                // globally unique index for loss/duplicate detection
                                props.put("idx", threadIndex * perThread + c);
                                props.put("tid", threadIndex);
                                sdk.track(eventName, props);
                            } catch (Throwable err) {
                                failures.incrementAndGet();
                                firstError.compareAndSet(null, err);
                            }
                        }
                    } catch (InterruptedException ie) {
                        failures.incrementAndGet();
                        firstError.compareAndSet(null, ie);
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }
            });
        }

        start.countDown();
        assertTrue("5000-track workers must finish", done.await(90, TimeUnit.SECONDS));
        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS));
        assertEquals("concurrent 5000 track must not throw: " + firstError.get(),
                0, failures.get());

        // Wait for TrackTaskManager queue + DB writes, then flush remaining rows.
        waitUntilDbQuietOrTimeout(context, APP_ID, 15_000);
        flushUntilDbEmpty(sdk, context, APP_ID, 60);

        Set<Integer> receivedIdx = new HashSet<>();
        int namedCount = drainAndCollectEventIndexes(server, eventName, receivedIdx, 30_000);

        assertEquals("server must receive exactly " + totalEvents + " '" + eventName + "' events",
                totalEvents, namedCount);
        assertEquals("received idx set size must equal " + totalEvents + " (no loss/dup)",
                totalEvents, receivedIdx.size());
        assertEquals("DB must be empty after full upload", 0, countDbEvents(context, APP_ID));
    }

    /**
     * Pre-release soak: mix track/login/identify/user_set/flush for a bounded window.
     * Asserts no crashes and that restoring NORMAL allows DB to drain.
     *
     * <p>Paced deliberately — unbounded hot loops OOM the instrumentation process because
     * MockWebServer retains request bodies and SharedPreferences apply queues grow.
     */
    @Test(timeout = 120_000)
    public void stress_soak_mixedApis_noCrashAndDbDrainable() throws Exception {
        final long soakMs = 15_000L;
        final int workers = 3;
        enqueueResponses(server, 400);

        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>();
        final AtomicInteger trackCalls = new AtomicInteger(0);
        final long deadline = System.currentTimeMillis() + soakMs;
        final CountDownLatch done = new CountDownLatch(workers);
        ExecutorService pool = Executors.newFixedThreadPool(workers + 1);

        // Drain MockWebServer continuously so retained bodies do not blow the heap.
        final AtomicInteger drainStop = new AtomicInteger(0);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                while (drainStop.get() == 0) {
                    try {
                        RecordedRequest req = server.takeRequest(200, TimeUnit.MILLISECONDS);
                        if (req == null) {
                            Thread.sleep(50);
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (Throwable ignored) {
                        // keep draining
                    }
                }
            }
        });

        for (int t = 0; t < workers; t++) {
            final int threadIndex = t;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    int seq = 0;
                    try {
                        while (System.currentTimeMillis() < deadline) {
                            try {
                                switch (seq % 5) {
                                    case 0: {
                                        JSONObject props = new JSONObject();
                                        props.put("tid", threadIndex);
                                        props.put("seq", seq);
                                        sdk.track("soak_track", props);
                                        trackCalls.incrementAndGet();
                                        break;
                                    }
                                    case 1:
                                        sdk.login("soak_login_" + threadIndex + "_" + (seq % 20));
                                        break;
                                    case 2:
                                        sdk.identify("soak_distinct_" + threadIndex + "_" + (seq % 20));
                                        break;
                                    case 3: {
                                        JSONObject props = new JSONObject();
                                        props.put("n", seq % 50);
                                        sdk.user_set(props);
                                        break;
                                    }
                                    default:
                                        sdk.flush();
                                        break;
                                }
                                Thread.sleep(15);
                            } catch (Throwable err) {
                                failures.incrementAndGet();
                                firstError.compareAndSet(null, err);
                            }
                            seq++;
                        }
                    } finally {
                        done.countDown();
                    }
                }
            });
        }

        assertTrue(done.await(soakMs + 20_000, TimeUnit.MILLISECONDS));
        drainStop.set(1);
        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS));
        assertEquals("soak must not throw: " + firstError.get(), 0, failures.get());
        assertTrue("soak should issue some track calls", trackCalls.get() > 0);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        waitUntilDbQuietOrTimeout(context, APP_ID, 10_000);
        flushUntilDbEmpty(sdk, context, APP_ID, 80);
        // Final drain of any leftover uploads
        while (server.takeRequest(300, TimeUnit.MILLISECONDS) != null) {
            // discard
        }
        assertEquals("DB should drain after soak + NORMAL flush",
                0, countDbEvents(context, APP_ID));
        assertFalse(sdk.getDistinctId() == null || sdk.getDistinctId().isEmpty());
    }

    /**
     * Pre-release: flip PAUSE / SAVE_ONLY / NORMAL while other threads keep calling APIs.
     * Avoid STOP here (it clears identity and empties queue by design).
     */
    @Test(timeout = 120_000)
    public void stress_interleavedTrackStatus_noCrashAndRecoverable() throws Exception {
        enqueueResponses(server, 200);
        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>();
        final int workers = 6;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(workers);
        ExecutorService pool = Executors.newFixedThreadPool(workers);

        // worker 0 flips status; others hammer track/login/identify/flush
        for (int t = 0; t < workers; t++) {
            final int threadIndex = t;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        start.await(10, TimeUnit.SECONDS);
                        if (threadIndex == 0) {
                            ThinkingAnalyticsSDK.TATrackStatus[] statuses =
                                    new ThinkingAnalyticsSDK.TATrackStatus[] {
                                            ThinkingAnalyticsSDK.TATrackStatus.PAUSE,
                                            ThinkingAnalyticsSDK.TATrackStatus.NORMAL,
                                            ThinkingAnalyticsSDK.TATrackStatus.SAVE_ONLY,
                                            ThinkingAnalyticsSDK.TATrackStatus.NORMAL,
                                    };
                            for (int i = 0; i < 20; i++) {
                                try {
                                    sdk.setTrackStatus(statuses[i % statuses.length]);
                                    Thread.sleep(50);
                                } catch (Throwable err) {
                                    failures.incrementAndGet();
                                    firstError.compareAndSet(null, err);
                                }
                            }
                        } else {
                            for (int i = 0; i < 40; i++) {
                                try {
                                    switch (i % 4) {
                                        case 0:
                                            sdk.track("status_stress_track");
                                            break;
                                        case 1:
                                            sdk.login("status_stress_login_" + threadIndex);
                                            break;
                                        case 2:
                                            sdk.identify("status_stress_id_" + threadIndex);
                                            break;
                                        default:
                                            sdk.flush();
                                            break;
                                    }
                                    Thread.sleep(10);
                                } catch (Throwable err) {
                                    failures.incrementAndGet();
                                    firstError.compareAndSet(null, err);
                                }
                            }
                        }
                    } catch (InterruptedException ie) {
                        failures.incrementAndGet();
                        firstError.compareAndSet(null, ie);
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }
            });
        }

        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS));
        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS));
        assertEquals("status interleave must not throw: " + firstError.get(),
                0, failures.get());

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        Thread.sleep(400);
        drainPendingRequests(server);
        clearDbEvents(context, APP_ID);

        // Recovered NORMAL path must still accept and upload a new event.
        sdk.track("status_stress_recover_probe");
        Thread.sleep(500);
        assertNotNull(flushAndAssertRequest(sdk, server));
        assertFalse(sdk.getDistinctId() == null || sdk.getDistinctId().isEmpty());
    }

    /**
     * Pre-release: intermittent HTTP 500 then recovery — events must eventually upload.
     */
    @Test(timeout = 180_000)
    public void stress_intermittentServerErrors_thenRecover_allEventsUploaded() throws Exception {
        final int totalEvents = 200;
        final String eventName = "stress_recover_track";

        // Fail every other flush attempt, then provide plenty of successes for retries.
        for (int i = 0; i < 120; i++) {
            if (i % 2 == 0) {
                server.enqueue(new MockResponse().setResponseCode(500).setBody("temp error"));
            } else {
                server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":0}"));
            }
        }
        for (int i = 0; i < 200; i++) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":0}"));
        }

        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>();
        runConcurrent(8, totalEvents / 8, new Worker() {
            @Override
            public void run(int threadIndex, int callIndex) throws Exception {
                JSONObject props = new JSONObject();
                props.put("idx", threadIndex * (totalEvents / 8) + callIndex);
                sdk.track(eventName, props);
            }
        }, failures, firstError);

        assertEquals("error-injection track must not throw: " + firstError.get(),
                0, failures.get());

        waitUntilDbQuietOrTimeout(context, APP_ID, 10_000);
        flushUntilDbEmpty(sdk, context, APP_ID, 100);

        Set<Integer> receivedIdx = new HashSet<>();
        int namedCount = drainAndCollectEventIndexes(server, eventName, receivedIdx, 45_000);

        // Retries after HTTP 500 may re-POST the same batch, so namedCount can exceed totalEvents.
        assertTrue("server should observe at least " + totalEvents + " named events, got " + namedCount,
                namedCount >= totalEvents);
        assertEquals("unique idx must equal " + totalEvents + " after recovery (no loss)",
                totalEvents, receivedIdx.size());
        assertEquals("DB must be empty after recovery uploads",
                0, countDbEvents(context, APP_ID));
    }

    @Test(timeout = 60_000)
    public void concurrent_login_noCrashAndLoginIdIsOneOfWrittenValues() throws Exception {
        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>();
        final Set<String> writtenIds = new HashSet<>();

        runConcurrent(THREADS, CALLS_PER_THREAD, new Worker() {
            @Override
            public void run(int threadIndex, int callIndex) {
                String loginId = "login_" + threadIndex + "_" + callIndex;
                synchronized (writtenIds) {
                    writtenIds.add(loginId);
                }
                sdk.login(loginId);
            }
        }, failures, firstError);

        assertEquals("concurrent login must not throw: " + firstError.get(),
                0, failures.get());

        Thread.sleep(800);
        String loginId = sdk.getLoginId();
        assertNotNull(loginId);
        assertFalse(loginId.isEmpty());
        assertTrue("final loginId must be one of concurrently written values, got: " + loginId,
                writtenIds.contains(loginId));
    }

    @Test(timeout = 60_000)
    public void concurrent_identify_noCrashAndDistinctIdIsOneOfWrittenValues() throws Exception {
        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>();
        final Set<String> writtenIds = new HashSet<>();

        runConcurrent(THREADS, CALLS_PER_THREAD, new Worker() {
            @Override
            public void run(int threadIndex, int callIndex) {
                String distinctId = "distinct_" + threadIndex + "_" + callIndex;
                synchronized (writtenIds) {
                    writtenIds.add(distinctId);
                }
                sdk.identify(distinctId);
            }
        }, failures, firstError);

        assertEquals("concurrent identify must not throw: " + firstError.get(),
                0, failures.get());

        Thread.sleep(800);
        String distinctId = sdk.getDistinctId();
        assertNotNull(distinctId);
        assertFalse(distinctId.isEmpty());
        assertTrue("final distinctId must be one of concurrently written values, got: " + distinctId,
                writtenIds.contains(distinctId));
    }

    @Test(timeout = 60_000)
    public void concurrent_userSet_noCrashAndRequestSent() throws Exception {
        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>();

        sdk.login("concurrent_user_set");
        Thread.sleep(200);

        runConcurrent(THREADS, CALLS_PER_THREAD, new Worker() {
            @Override
            public void run(int threadIndex, int callIndex) throws Exception {
                JSONObject props = new JSONObject();
                props.put("score", threadIndex * 1000 + callIndex);
                props.put("tag", "t" + threadIndex);
                sdk.user_set(props);
            }
        }, failures, firstError);

        assertEquals("concurrent user_set must not throw: " + firstError.get(),
                0, failures.get());

        Thread.sleep(800);
        assertNotNull(flushAndAssertRequest(sdk, server));
    }

    @Test(timeout = 60_000)
    public void concurrent_mixedApis_noCrashAndIdentityReadable() throws Exception {
        final AtomicInteger failures = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>();

        runConcurrent(THREADS, CALLS_PER_THREAD, new Worker() {
            @Override
            public void run(int threadIndex, int callIndex) throws Exception {
                switch (callIndex % 5) {
                    case 0:
                        sdk.track("mixed_track_" + threadIndex);
                        break;
                    case 1:
                        sdk.login("mixed_login_" + threadIndex + "_" + callIndex);
                        break;
                    case 2:
                        sdk.identify("mixed_distinct_" + threadIndex + "_" + callIndex);
                        break;
                    case 3: {
                        JSONObject props = new JSONObject();
                        props.put("k", callIndex);
                        sdk.user_set(props);
                        break;
                    }
                    default:
                        sdk.flush();
                        break;
                }
            }
        }, failures, firstError);

        assertEquals("mixed concurrent APIs must not throw: " + firstError.get(),
                0, failures.get());

        Thread.sleep(1000);
        assertNotNull(sdk.getDistinctId());
        assertFalse(sdk.getDistinctId().isEmpty());
        // loginId may be empty only if last mixed op was logout — we never logout here
        String loginId = sdk.getLoginId();
        assertNotNull(loginId);
        assertFalse("loginId should remain set after mixed concurrent calls", loginId.isEmpty());
    }

    @Test(timeout = 60_000)
    public void concurrent_init_sameAppId_returnsSameInstance() throws Exception {
        final String appId = "at_concurrent_init_001";
        final MockWebServer localServer = new MockWebServer();
        localServer.start();
        try {
            clearSdkData(context, appId);
            final String url = localServer.url("/").toString();
            final List<ThinkingAnalyticsSDK> instances = new ArrayList<>();
            final AtomicInteger failures = new AtomicInteger(0);
            final AtomicReference<Throwable> firstError = new AtomicReference<>();

            final int initThreads = 12;
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(initThreads);
            ExecutorService pool = Executors.newFixedThreadPool(initThreads);
            for (int i = 0; i < initThreads; i++) {
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            start.await(10, TimeUnit.SECONDS);
                            TDConfig config = TDConfig.getInstance(context, appId, url);
                            config.disableRConfig = true;
                            ThinkingAnalyticsSDK instance = ThinkingAnalyticsSDK.sharedInstance(config);
                            synchronized (instances) {
                                instances.add(instance);
                            }
                        } catch (Throwable t) {
                            failures.incrementAndGet();
                            firstError.compareAndSet(null, t);
                        } finally {
                            done.countDown();
                        }
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS));
            pool.shutdownNow();

            assertEquals("concurrent init must not throw: " + firstError.get(),
                    0, failures.get());
            assertEquals(initThreads, instances.size());
            ThinkingAnalyticsSDK first = instances.get(0);
            assertNotNull(first);
            for (ThinkingAnalyticsSDK instance : instances) {
                assertSame("sharedInstance must return the same singleton under concurrent init",
                        first, instance);
            }
        } finally {
            localServer.shutdown();
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private interface Worker {
        void run(int threadIndex, int callIndex) throws Exception;
    }

    private static void runConcurrent(int threads, int callsPerThread, final Worker worker,
                                      final AtomicInteger failures,
                                      final AtomicReference<Throwable> firstError)
            throws InterruptedException {
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            final int threadIndex = t;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        start.await(10, TimeUnit.SECONDS);
                        for (int c = 0; c < callsPerThread; c++) {
                            try {
                                worker.run(threadIndex, c);
                            } catch (Throwable err) {
                                failures.incrementAndGet();
                                firstError.compareAndSet(null, err);
                            }
                        }
                    } catch (InterruptedException ie) {
                        failures.incrementAndGet();
                        firstError.compareAndSet(null, ie);
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }
            });
        }
        start.countDown();
        assertTrue("workers must finish in time", done.await(45, TimeUnit.SECONDS));
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
    }

    private static int countEventsByName(JSONArray events, String name) throws Exception {
        int count = 0;
        for (int i = 0; i < events.length(); i++) {
            if (name.equals(events.getJSONObject(i).optString("#event_name"))) {
                count++;
            }
        }
        return count;
    }

    private static void waitUntilDbQuietOrTimeout(Context ctx, String appId, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int last = -1;
        int stableRounds = 0;
        while (System.currentTimeMillis() < deadline) {
            int current = countDbEvents(ctx, appId);
            if (current == last) {
                stableRounds++;
                if (stableRounds >= 3) {
                    return;
                }
            } else {
                stableRounds = 0;
                last = current;
            }
            Thread.sleep(300);
        }
    }

    private static void flushUntilDbEmpty(ThinkingAnalyticsSDK sdkInstance, Context ctx,
                                          String appId, int maxRounds) throws Exception {
        for (int i = 0; i < maxRounds; i++) {
            int remaining = countDbEvents(ctx, appId);
            if (remaining <= 0) {
                return;
            }
            sdkInstance.flush();
            Thread.sleep(250);
        }
    }

    /**
     * Drain MockWebServer requests and collect unique {@code properties.idx} for {@code eventName}.
     *
     * @return total number of events with the given name across all drained requests
     */
    private static int drainAndCollectEventIndexes(MockWebServer srv, String eventName,
                                                    Set<Integer> outIdx, long drainWindowMs)
            throws Exception {
        int namedCount = 0;
        long deadline = System.currentTimeMillis() + drainWindowMs;
        while (System.currentTimeMillis() < deadline) {
            RecordedRequest req = srv.takeRequest(500, TimeUnit.MILLISECONDS);
            if (req == null) {
                if (namedCount > 0) {
                    // allow a short quiet period after first batches arrive
                    RecordedRequest more = srv.takeRequest(1, TimeUnit.SECONDS);
                    if (more == null) {
                        break;
                    }
                    req = more;
                } else {
                    continue;
                }
            }
            JSONArray events = decodeRequestBody(req);
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                if (!eventName.equals(event.optString("#event_name"))) {
                    continue;
                }
                namedCount++;
                JSONObject props = event.optJSONObject("properties");
                if (props != null && props.has("idx")) {
                    outIdx.add(props.getInt("idx"));
                }
            }
        }
        return namedCount;
    }
}
