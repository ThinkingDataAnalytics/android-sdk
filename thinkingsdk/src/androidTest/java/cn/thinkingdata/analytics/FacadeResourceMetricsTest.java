/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Process;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

/**
 * Soft resource regression gates for {@link TDAnalytics}:
 * Java heap / PSS memory delta, leak-suspicion across rounds, and relative CPU time.
 *
 * <p>These are instrumented-process metrics, not Studio Profiler / LeakCanary proofs.
 * Thresholds are intentionally loose across devices.
 *
 * <p>Logcat tag: {@code TDApiResource}
 */
@RunWith(AndroidJUnit4.class)
public class FacadeResourceMetricsTest extends BaseSdkTest {

    private static final String TAG = "TDApiResource";
    private static final String APP_ID = "at_resource_001";

    /** After 1000 tracks + flush + GC, Java used-heap growth should stay under this. */
    private static final long LIMIT_HEAP_DELTA_KB = 12 * 1024L; // 12 MB
    /** Same for PSS when ActivityManager is available. */
    private static final long LIMIT_PSS_DELTA_KB = 20 * 1024L; // 20 MB
    /** Multi-round leak suspicion: final heap should not exceed first post-GC by this much. */
    private static final long LIMIT_LEAK_GROWTH_KB = 16 * 1024L; // 16 MB
    /** Relative CPU for 1000 tracks on the calling thread budget. */
    private static final long LIMIT_CPU_MS = 5_000L;

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
        PerfReportStore.flush(context);
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        prepareBetweenTestsFacade(context, APP_ID, server, sdk, 200);
        forceGc();
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    @Test(timeout = 120_000)
    public void resource_trackStorm_memoryDeltaWithinBudget() throws Exception {
        forceGc();
        MemSample before = sampleMemory("before_track_storm");

        final AtomicInteger drainStop = new AtomicInteger(0);
        Thread drainer = startRequestDrainer(drainStop);
        try {
            runTrackStorm(1000);
            TDAnalytics.flush();
            Thread.sleep(800);
        } finally {
            drainStop.set(1);
            drainer.join(3_000);
        }

        forceGc();
        MemSample after = sampleMemory("after_track_storm");

        long heapDeltaKb = after.usedHeapKb - before.usedHeapKb;
        long pssDeltaKb = (before.pssKb > 0 && after.pssKb > 0)
                ? (after.pssKb - before.pssKb) : -1;
        Log.i(TAG, "[resource] heapDeltaKb=" + heapDeltaKb
                + " pssDeltaKb=" + pssDeltaKb
                + " limitHeapKb=" + LIMIT_HEAP_DELTA_KB);

        boolean heapPass = heapDeltaKb <= LIMIT_HEAP_DELTA_KB;
        JSONObject extras = new JSONObject();
        extras.put("beforeHeapKb", before.usedHeapKb);
        extras.put("afterHeapKb", after.usedHeapKb);
        extras.put("beforePssKb", before.pssKb);
        extras.put("afterPssKb", after.pssKb);
        PerfReportStore.recordResource("trackStorm_memory", "heapDelta",
                heapDeltaKb, LIMIT_HEAP_DELTA_KB, "KB", heapPass, extras);
        assertTrue("Java heap grew " + heapDeltaKb + "KB after track storm (limit "
                        + LIMIT_HEAP_DELTA_KB + "KB)",
                heapPass);

        if (pssDeltaKb >= 0) {
            boolean pssPass = pssDeltaKb <= LIMIT_PSS_DELTA_KB;
            PerfReportStore.recordResource("trackStorm_memory", "pssDelta",
                    pssDeltaKb, LIMIT_PSS_DELTA_KB, "KB", pssPass, extras);
            assertTrue("PSS grew " + pssDeltaKb + "KB after track storm (limit "
                            + LIMIT_PSS_DELTA_KB + "KB)",
                    pssPass);
        }
    }

    @Test(timeout = 180_000)
    public void resource_multiRound_noMonotonicLeakSuspicion() throws Exception {
        final int rounds = 5;
        final int tracksPerRound = 400;
        long[] postGcHeapKb = new long[rounds];

        for (int r = 0; r < rounds; r++) {
            enqueueResponses(server, 80);
            final AtomicInteger drainStop = new AtomicInteger(0);
            Thread drainer = startRequestDrainer(drainStop);
            try {
                runTrackStorm(tracksPerRound);
                TDAnalytics.login("resource_user_" + r);
                TDAnalytics.setDistinctId("resource_distinct_" + r);
                TDAnalytics.flush();
                Thread.sleep(500);
            } finally {
                drainStop.set(1);
                drainer.join(3_000);
            }
            clearDbEvents(context, APP_ID);
            drainPendingRequests(server);
            forceGc();
            MemSample sample = sampleMemory("round_" + r);
            postGcHeapKb[r] = sample.usedHeapKb;
        }

        long first = postGcHeapKb[0];
        long last = postGcHeapKb[rounds - 1];
        long growth = last - first;
        Log.i(TAG, "[resource] leakSuspect rounds heapKb="
                + java.util.Arrays.toString(postGcHeapKb)
                + " growthKb=" + growth
                + " limitKb=" + LIMIT_LEAK_GROWTH_KB);

        boolean passed = growth <= LIMIT_LEAK_GROWTH_KB;
        JSONObject extras = new JSONObject();
        org.json.JSONArray roundsArr = new org.json.JSONArray();
        for (long v : postGcHeapKb) {
            roundsArr.put(v);
        }
        extras.put("roundsHeapKb", roundsArr);
        PerfReportStore.recordResource("multiRound_leakSuspect", "heapGrowth",
                growth, LIMIT_LEAK_GROWTH_KB, "KB", passed, extras);
        assertTrue("Post-GC heap grew " + growth + "KB across " + rounds
                        + " rounds (limit " + LIMIT_LEAK_GROWTH_KB + "KB) — leak suspicion",
                passed);
    }

    @Test(timeout = 120_000)
    public void resource_trackStorm_cpuTimeWithinBudget() throws Exception {
        // Warm paths so first-call class loading does not dominate CPU sample.
        runTrackStorm(50);
        TDAnalytics.flush();
        Thread.sleep(300);
        drainPendingRequests(server);
        clearDbEvents(context, APP_ID);
        enqueueResponses(server, 100);

        final AtomicInteger drainStop = new AtomicInteger(0);
        Thread drainer = startRequestDrainer(drainStop);

        long cpuBefore = Debug.threadCpuTimeNanos();
        long wallBefore = System.nanoTime();
        try {
            runTrackStorm(1000);
            TDAnalytics.flush();
            Thread.sleep(500);
        } finally {
            drainStop.set(1);
            drainer.join(3_000);
        }
        long cpuMs = TimeUnit.NANOSECONDS.toMillis(Debug.threadCpuTimeNanos() - cpuBefore);
        long wallMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - wallBefore);

        Log.i(TAG, "[resource] trackStorm cpuMs=" + cpuMs
                + " wallMs=" + wallMs
                + " limitCpuMs=" + LIMIT_CPU_MS);

        boolean passed = cpuMs <= LIMIT_CPU_MS;
        JSONObject extras = new JSONObject();
        extras.put("wallMs", wallMs);
        PerfReportStore.recordResource("trackStorm_cpu", "threadCpu",
                cpuMs, LIMIT_CPU_MS, "ms", passed, extras);
        assertTrue("Calling-thread CPU " + cpuMs + "ms exceeds limit " + LIMIT_CPU_MS + "ms",
                passed);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void runTrackStorm(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            JSONObject props = new JSONObject();
            props.put("i", i);
            TDAnalytics.track("resource_metrics_track", props);
            if ((i & 63) == 0) {
                // Yield occasionally so save/send workers can breathe.
                Thread.sleep(1);
            }
        }
    }

    private static Thread startRequestDrainer(final AtomicInteger stopFlag) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (stopFlag.get() == 0) {
                    try {
                        RecordedRequest req = server.takeRequest(100, TimeUnit.MILLISECONDS);
                        if (req == null) {
                            Thread.sleep(20);
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    } catch (Throwable ignored) {
                        // keep draining
                    }
                }
            }
        }, "td-resource-drainer");
        t.start();
        return t;
    }

    private static void forceGc() throws InterruptedException {
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Thread.sleep(200);
    }

    private static MemSample sampleMemory(String label) {
        Runtime rt = Runtime.getRuntime();
        long usedHeapKb = (rt.totalMemory() - rt.freeMemory()) / 1024L;
        long pssKb = readPssKb();
        Log.i(TAG, "[resource] " + label
                + " usedHeapKb=" + usedHeapKb
                + " pssKb=" + pssKb
                + " nativeHeapKb=" + (Debug.getNativeHeapAllocatedSize() / 1024L));
        return new MemSample(usedHeapKb, pssKb);
    }

    private static long readPssKb() {
        try {
            ActivityManager am =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) {
                return -1;
            }
            Debug.MemoryInfo[] infos = am.getProcessMemoryInfo(new int[]{Process.myPid()});
            if (infos == null || infos.length == 0) {
                return -1;
            }
            return infos[0].getTotalPss();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static final class MemSample {
        final long usedHeapKb;
        final long pssKb;

        MemSample(long usedHeapKb, long pssKb) {
            this.usedHeapKb = usedHeapKb;
            this.pssKb = pssKb;
        }
    }
}
