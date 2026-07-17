/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;
import android.util.Log;

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

import cn.thinkingdata.analytics.model.TDFirstEventModel;

import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Call-site latency benchmarks for {@link TDAnalytics} public facade APIs.
 *
 * <p>Measures how long the API call takes to <em>return</em> on the calling thread
 * (most APIs enqueue async work). Thresholds are soft regression guards for
 * main-thread friendliness, not end-to-end upload latency.
 *
 * <p>Results are printed as {@code [latency] name avg=..ms max=..ms} in logcat.
 */
@RunWith(AndroidJUnit4.class)
public class FacadeApiLatencyTest extends BaseSdkTest {

    private static final String TAG = "TDApiLatency";
    private static final String APP_ID = "at_latency_001";

    /** Soft caps for fire-and-forget / getter call return time. */
    private static final long LIMIT_FAST_MS = 50L;
    private static final long LIMIT_NORMAL_MS = 100L;
    private static final long LIMIT_HEAVY_MS = 300L;
    private static final long LIMIT_INIT_MS = 2000L;

    private static final int WARMUP = 2;
    private static final int ITERATIONS = 8;

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
        enqueueResponses(server, 40);
        resetFacadeSdkState();
        clearDbEvents(context, APP_ID);
        Thread.sleep(300);
        drainPendingRequests(server);
    }

    @After
    public void tearDown() throws Exception {
        drainPendingRequests(server);
    }

    // ── calibrate / encrypt / init ────────────────────────────────────────────

    @Test
    public void latency_calibrateTime() {
        assertAvgUnder("calibrateTime", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.calibrateTime(System.currentTimeMillis());
            }
        });
    }

    @Test
    public void latency_calibrateTimeWithNtp() {
        // Only measures call return (NTP runs async). Avoid hammering real NTP.
        double ms = measureOnce("calibrateTimeWithNtp", new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.calibrateTimeWithNtp("time.apple.com");
            }
        });
        assertUnder("calibrateTimeWithNtp", ms, LIMIT_NORMAL_MS);
    }

    @Test
    public void latency_encryptLocalStorage() {
        assertAvgUnder("encryptLocalStorage", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.encryptLocalStorage();
            }
        });
    }

    @Test
    public void latency_init_withAppIdAndUrl() throws Exception {
        final String appId = APP_ID + "_init_latency";
        clearSdkData(context, appId);
        removeSdkInstance(context, appId);

        double ms = measureOnce("init(context,appId,url)", new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.init(context, appId, server.url("/").toString());
            }
        });
        assertUnder("init(context,appId,url)", ms, LIMIT_INIT_MS);
        // Re-bind facade instance used by other tests in this class.
        TDAnalytics.instance = sdk;
    }

    @Test
    public void latency_init_withConfig() throws Exception {
        final String appId = APP_ID + "_init_cfg_latency";
        clearSdkData(context, appId);
        removeSdkInstance(context, appId);
        final TDConfig config = TDConfig.getInstance(context, appId, server.url("/").toString());
        config.disableRConfig = true;

        double ms = measureOnce("init(TDConfig)", new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.init(config);
            }
        });
        assertUnder("init(TDConfig)", ms, LIMIT_INIT_MS);
        TDAnalytics.instance = sdk;
    }

    // ── track / autoTrack / timeEvent ─────────────────────────────────────────

    @Test
    public void latency_track_eventNameOnly() {
        assertAvgUnder("track(eventName)", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.track("latency_track");
            }
        });
    }

    @Test
    public void latency_track_withProperties() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("k", "v");
        props.put("n", 1);
        assertAvgUnder("track(eventName,properties)", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.track("latency_track_props", props);
            }
        });
    }

    @Test
    public void latency_track_withTimeAndZone() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("k", 1);
        final Date time = new Date();
        final TimeZone zone = TimeZone.getDefault();
        assertAvgUnder("track(eventName,properties,time,zone)", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.track("latency_track_time", props, time, zone);
            }
        });
    }

    @Test
    public void latency_track_eventModel() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("model", true);
        assertAvgUnder("track(TDEventModel)", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.track(new TDFirstEventModel("latency_model", props));
            }
        });
    }

    @Test
    public void latency_enableAutoTrack_typesOnly() {
        assertAvgUnder("enableAutoTrack(types)", LIMIT_HEAVY_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.enableAutoTrack(TDAnalytics.TDAutoTrackEventType.APP_START
                        | TDAnalytics.TDAutoTrackEventType.APP_END);
            }
        });
    }

    @Test
    public void latency_enableAutoTrack_withProperties() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("auto", 1);
        assertAvgUnder("enableAutoTrack(types,properties)", LIMIT_HEAVY_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.enableAutoTrack(TDAnalytics.TDAutoTrackEventType.APP_START, props);
            }
        });
    }

    @Test
    public void latency_enableAutoTrack_withHandler() {
        assertAvgUnder("enableAutoTrack(types,handler)", LIMIT_HEAVY_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.enableAutoTrack(
                        TDAnalytics.TDAutoTrackEventType.APP_START,
                        new TDAnalytics.TDAutoTrackEventHandler() {
                            @Override
                            public JSONObject getAutoTrackEventProperties(int type,
                                                                           JSONObject properties) {
                                return properties;
                            }
                        });
            }
        });
    }

    @Test
    public void latency_timeEvent() {
        assertAvgUnder("timeEvent", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.timeEvent("latency_timed_event");
            }
        });
    }

    // ── user properties ───────────────────────────────────────────────────────

    @Test
    public void latency_userSet() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("name", "latency");
        assertAvgUnder("userSet", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.userSet(props);
            }
        });
    }

    @Test
    public void latency_userSetOnce() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("first", true);
        assertAvgUnder("userSetOnce", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.userSetOnce(props);
            }
        });
    }

    @Test
    public void latency_userUnset() {
        assertAvgUnder("userUnset", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.userUnset("latency_key");
            }
        });
    }

    @Test
    public void latency_userAdd() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("score", 1);
        assertAvgUnder("userAdd", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.userAdd(props);
            }
        });
    }

    @Test
    public void latency_userAppend() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("tags", new JSONArray().put("a"));
        assertAvgUnder("userAppend", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.userAppend(props);
            }
        });
    }

    @Test
    public void latency_userUniqAppend() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("tags", new JSONArray().put("u"));
        assertAvgUnder("userUniqAppend", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.userUniqAppend(props);
            }
        });
    }

    @Test
    public void latency_userDelete() {
        // Destructive — measure once only.
        double ms = measureOnce("userDelete", new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.userDelete();
            }
        });
        assertUnder("userDelete", ms, LIMIT_FAST_MS);
    }

    // ── super / preset / identity / flush ─────────────────────────────────────

    @Test
    public void latency_setSuperProperties() throws Exception {
        final JSONObject props = new JSONObject();
        props.put("channel", "latency");
        assertAvgUnder("setSuperProperties", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.setSuperProperties(props);
            }
        });
    }

    @Test
    public void latency_setDynamicSuperProperties() {
        assertAvgUnder("setDynamicSuperProperties", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.setDynamicSuperProperties(new TDAnalytics.TDDynamicSuperPropertiesHandler() {
                    @Override
                    public JSONObject getDynamicSuperProperties() {
                        return new JSONObject();
                    }
                });
            }
        });
    }

    @Test
    public void latency_unsetSuperProperty() {
        assertAvgUnder("unsetSuperProperty", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.unsetSuperProperty("channel");
            }
        });
    }

    @Test
    public void latency_clearSuperProperties() {
        assertAvgUnder("clearSuperProperties", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.clearSuperProperties();
            }
        });
    }

    @Test
    public void latency_getSuperProperties() {
        assertAvgUnder("getSuperProperties", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.getSuperProperties();
            }
        });
    }

    @Test
    public void latency_getPresetProperties() {
        // May touch WindowManager / DeviceInfo — allow heavier budget.
        assertAvgUnder("getPresetProperties", LIMIT_HEAVY_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.getPresetProperties();
            }
        });
    }

    @Test
    public void latency_login() {
        final AtomicCounter counter = new AtomicCounter();
        assertAvgUnder("login", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.login("latency_user_" + counter.next());
            }
        });
    }

    @Test
    public void latency_logout() {
        assertAvgUnder("logout", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.logout();
            }
        });
    }

    @Test
    public void latency_setDistinctId() {
        final AtomicCounter counter = new AtomicCounter();
        assertAvgUnder("setDistinctId", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.setDistinctId("latency_distinct_" + counter.next());
            }
        });
    }

    @Test
    public void latency_getDistinctId() {
        assertAvgUnder("getDistinctId", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.getDistinctId();
            }
        });
    }

    @Test
    public void latency_getAccountId() {
        assertAvgUnder("getAccountId", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.getAccountId();
            }
        });
    }

    @Test
    public void latency_getDeviceId() {
        assertAvgUnder("getDeviceId", LIMIT_NORMAL_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.getDeviceId();
            }
        });
    }

    @Test
    public void latency_flush() {
        assertAvgUnder("flush", LIMIT_FAST_MS, new ApiCall() {
            @Override
            public void run() {
                TDAnalytics.flush();
            }
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private interface ApiCall {
        void run() throws Exception;
    }

    private static final class AtomicCounter {
        private int value;

        int next() {
            return value++;
        }
    }

    /** Elapsed wall time in milliseconds (sub-ms precision). */
    private static double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    private static double measureOnce(String name, ApiCall call) {
        try {
            long t0 = System.nanoTime();
            call.run();
            double ms = elapsedMs(t0);
            Log.i(TAG, "[latency] " + name + " = " + String.format(java.util.Locale.US, "%.3f", ms) + "ms");
            return ms;
        } catch (Exception e) {
            throw new RuntimeException(name + " threw", e);
        }
    }

    private static void assertAvgUnder(String name, long limitMs, ApiCall call) {
        for (int i = 0; i < WARMUP; i++) {
            try {
                call.run();
            } catch (Exception e) {
                throw new RuntimeException(name + " warmup failed", e);
            }
        }

        double[] samples = new double[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            try {
                long t0 = System.nanoTime();
                call.run();
                samples[i] = elapsedMs(t0);
            } catch (Exception e) {
                throw new RuntimeException(name + " measure failed", e);
            }
        }

        Arrays.sort(samples);
        double sum = 0;
        for (double s : samples) {
            sum += s;
        }
        double avg = sum / samples.length;
        double max = samples[samples.length - 1];
        double p50 = samples[samples.length / 2];

        Log.i(TAG, "[latency] " + name
                + " avg=" + String.format(java.util.Locale.US, "%.3f", avg) + "ms"
                + " p50=" + String.format(java.util.Locale.US, "%.3f", p50) + "ms"
                + " max=" + String.format(java.util.Locale.US, "%.3f", max) + "ms"
                + " limit=" + limitMs + "ms"
                + " samples=" + formatSamples(samples));

        boolean passed = avg <= limitMs;
        PerfReportStore.recordLatency(name, avg, p50, max, limitMs, samples, passed);
        assertTrue(name + " avg " + String.format(java.util.Locale.US, "%.3f", avg)
                        + "ms exceeds limit " + limitMs + "ms (max="
                        + String.format(java.util.Locale.US, "%.3f", max) + ")",
                passed);
    }

    private static String formatSamples(double[] samples) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < samples.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format(java.util.Locale.US, "%.3f", samples[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private static void assertUnder(String name, double actualMs, long limitMs) {
        boolean passed = actualMs <= limitMs;
        PerfReportStore.recordLatencyOnce(name, actualMs, limitMs, passed);
        assertTrue(name + " took " + String.format(java.util.Locale.US, "%.3f", actualMs)
                        + "ms, limit " + limitMs + "ms",
                passed);
    }
}
