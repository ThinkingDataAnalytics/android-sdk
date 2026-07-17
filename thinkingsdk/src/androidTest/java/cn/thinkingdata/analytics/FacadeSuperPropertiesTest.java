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

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Instrumented tests for {@link TDAnalytics} static / dynamic super-property APIs,
 * including verification that uploaded track payloads contain the expected properties.
 */
@RunWith(AndroidJUnit4.class)
public class FacadeSuperPropertiesTest extends BaseSdkTest {

    private static final String APP_ID = "at_facade_super_001";

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
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        enqueueResponses(server, 10);
        resetFacadeSdkState();
        clearDynamicSuperProperties();
        clearDbEvents(context, APP_ID);
        Thread.sleep(400);
        drainPendingRequests(server);
    }

    @After
    public void tearDown() throws Exception {
        drainPendingRequests(server);
    }

    // ── get / set / unset / clear (facade getters) ────────────────────────────

    @Test
    public void setSuperProperties_getSuperProperties_returnsValues() throws Exception {
        JSONObject props = new JSONObject();
        props.put("channel", "facade_store");
        props.put("campaign", "spring");
        TDAnalytics.setSuperProperties(props);
        Thread.sleep(300);

        JSONObject got = TDAnalytics.getSuperProperties();
        assertEquals("facade_store", got.getString("channel"));
        assertEquals("spring", got.getString("campaign"));
    }

    @Test
    public void unsetSuperProperty_removesKeyFromGetter() throws Exception {
        JSONObject props = new JSONObject();
        props.put("keep_me", "yes");
        props.put("drop_me", "no");
        TDAnalytics.setSuperProperties(props);
        Thread.sleep(200);

        TDAnalytics.unsetSuperProperty("drop_me");
        Thread.sleep(200);

        JSONObject got = TDAnalytics.getSuperProperties();
        assertTrue(got.has("keep_me"));
        assertFalse(got.has("drop_me"));
    }

    @Test
    public void clearSuperProperties_emptiesGetter() throws Exception {
        JSONObject props = new JSONObject();
        props.put("a", 1);
        props.put("b", 2);
        TDAnalytics.setSuperProperties(props);
        Thread.sleep(200);

        TDAnalytics.clearSuperProperties();
        Thread.sleep(200);

        JSONObject got = TDAnalytics.getSuperProperties();
        assertTrue(got == null || got.length() == 0);
    }

    // ── static super properties in uploaded payload ───────────────────────────

    @Test
    public void setSuperProperties_appearInUploadedTrack() throws Exception {
        JSONObject props = new JSONObject();
        props.put("static_channel", "organic");
        props.put("static_level", 7);
        TDAnalytics.setSuperProperties(props);
        Thread.sleep(300);

        trackAndWaitFacade("super_static_upload", null);

        JSONObject uploaded = decodeRequestBody(flushAndAssertFacade(server))
                .getJSONObject(0)
                .getJSONObject("properties");
        assertEquals("organic", uploaded.getString("static_channel"));
        assertEquals(7, uploaded.getInt("static_level"));
    }

    @Test
    public void unsetSuperProperty_absentFromSubsequentUpload() throws Exception {
        JSONObject props = new JSONObject();
        props.put("keep_upload", "present");
        props.put("drop_upload", "gone");
        TDAnalytics.setSuperProperties(props);
        Thread.sleep(200);
        TDAnalytics.unsetSuperProperty("drop_upload");
        Thread.sleep(200);

        trackAndWaitFacade("super_unset_upload", null);

        JSONObject uploaded = decodeRequestBody(flushAndAssertFacade(server))
                .getJSONObject(0)
                .getJSONObject("properties");
        assertEquals("present", uploaded.getString("keep_upload"));
        assertFalse(uploaded.has("drop_upload"));
    }

    @Test
    public void clearSuperProperties_absentFromSubsequentUpload() throws Exception {
        JSONObject props = new JSONObject();
        props.put("cleared_key", "should_not_upload");
        TDAnalytics.setSuperProperties(props);
        Thread.sleep(200);
        TDAnalytics.clearSuperProperties();
        Thread.sleep(200);

        trackAndWaitFacade("super_clear_upload", null);

        JSONObject uploaded = decodeRequestBody(flushAndAssertFacade(server))
                .getJSONObject(0)
                .getJSONObject("properties");
        assertFalse(uploaded.has("cleared_key"));
    }

    // ── dynamic super properties in uploaded payload ──────────────────────────

    @Test
    public void setDynamicSuperProperties_appearInUploadedTrack() throws Exception {
        final AtomicInteger callCount = new AtomicInteger(0);
        TDAnalytics.setDynamicSuperProperties(new TDAnalytics.TDDynamicSuperPropertiesHandler() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                callCount.incrementAndGet();
                try {
                    JSONObject dyn = new JSONObject();
                    dyn.put("dyn_ts", System.currentTimeMillis());
                    dyn.put("dyn_flag", "live");
                    return dyn;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread.sleep(200);

        trackAndWaitFacade("super_dynamic_upload", null);

        JSONObject uploaded = decodeRequestBody(flushAndAssertFacade(server))
                .getJSONObject(0)
                .getJSONObject("properties");
        assertTrue("dynamic handler must be invoked for track", callCount.get() > 0);
        assertEquals("live", uploaded.getString("dyn_flag"));
        assertTrue(uploaded.has("dyn_ts"));
        assertTrue(uploaded.getLong("dyn_ts") > 0);
    }

    @Test
    public void staticAndDynamicSuperProperties_bothInUploadedTrack() throws Exception {
        JSONObject staticProps = new JSONObject();
        staticProps.put("static_only", "S");
        staticProps.put("shared_key", "from_static");
        TDAnalytics.setSuperProperties(staticProps);

        TDAnalytics.setDynamicSuperProperties(new TDAnalytics.TDDynamicSuperPropertiesHandler() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                try {
                    JSONObject dyn = new JSONObject();
                    dyn.put("dynamic_only", "D");
                    dyn.put("shared_key", "from_dynamic");
                    return dyn;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread.sleep(300);

        trackAndWaitFacade("super_both_upload", null);

        JSONObject uploaded = decodeRequestBody(flushAndAssertFacade(server))
                .getJSONObject(0)
                .getJSONObject("properties");
        assertEquals("S", uploaded.getString("static_only"));
        assertEquals("D", uploaded.getString("dynamic_only"));
        // Dynamic is merged after static in obtainDefaultEventProperties.
        assertEquals("from_dynamic", uploaded.getString("shared_key"));
    }

    @Test
    public void eventProperties_overrideStaticAndDynamicInUpload() throws Exception {
        JSONObject staticProps = new JSONObject();
        staticProps.put("source", "static");
        TDAnalytics.setSuperProperties(staticProps);
        TDAnalytics.setDynamicSuperProperties(new TDAnalytics.TDDynamicSuperPropertiesHandler() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                try {
                    JSONObject dyn = new JSONObject();
                    dyn.put("source", "dynamic");
                    return dyn;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread.sleep(200);

        JSONObject eventProps = new JSONObject();
        eventProps.put("source", "event");
        trackAndWaitFacade("super_override_upload", eventProps);

        JSONObject uploaded = decodeRequestBody(flushAndAssertFacade(server))
                .getJSONObject(0)
                .getJSONObject("properties");
        assertEquals("event properties must win over static/dynamic super properties",
                "event", uploaded.getString("source"));
    }

    @Test
    public void dynamicSuperProperties_evaluatedPerTrack() throws Exception {
        final AtomicInteger seq = new AtomicInteger(0);
        TDAnalytics.setDynamicSuperProperties(new TDAnalytics.TDDynamicSuperPropertiesHandler() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                try {
                    JSONObject dyn = new JSONObject();
                    dyn.put("dyn_seq", seq.incrementAndGet());
                    return dyn;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread.sleep(200);

        trackAndWaitFacade("dyn_seq_1", null);
        JSONArray firstBatch = decodeRequestBody(flushAndAssertFacade(server));
        int firstSeq = firstBatch.getJSONObject(0).getJSONObject("properties").getInt("dyn_seq");

        trackAndWaitFacade("dyn_seq_2", null);
        JSONArray secondBatch = decodeRequestBody(flushAndAssertFacade(server));
        int secondSeq = secondBatch.getJSONObject(0).getJSONObject("properties").getInt("dyn_seq");

        assertEquals(1, firstSeq);
        assertEquals(2, secondSeq);
    }

    private static void clearDynamicSuperProperties() throws Exception {
        Field field = ThinkingAnalyticsSDK.class.getDeclaredField("mDynamicSuperPropertiesTracker");
        field.setAccessible(true);
        field.set(sdk, null);
    }
}
