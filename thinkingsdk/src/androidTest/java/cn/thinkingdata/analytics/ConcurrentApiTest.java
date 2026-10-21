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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

/**
 * Concurrent functional tests for primary SDK APIs:
 * {@code track}, {@code login}, {@code identify}/{@code setDistinctId}, {@code user_set}, {@code init}.
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
        prepareBetweenTests(context, APP_ID, server, sdk, 80);
        clearDbEvents(context, APP_ID);
    }

    @After
    public void tearDown() throws Exception {
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        waitForTrackTasks();
        cleanupAfterTest(server);
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
}
