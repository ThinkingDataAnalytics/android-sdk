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
        prepareBetweenTestsFacade(context, APP_ID, server, sdk, 10);
        clearDynamicSuperProperties();
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
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
        JSONObject current = TDAnalytics.getSuperProperties();
        assertEquals("present", current.getString("keep_upload"));
        assertFalse(current.has("drop_upload"));

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
        JSONObject current = TDAnalytics.getSuperProperties();
        assertTrue(current == null || current.length() == 0);

        trackAndWaitFacade("super_clear_upload", null);

        JSONObject uploaded = decodeRequestBody(flushAndAssertFacade(server))
                .getJSONObject(0)
                .getJSONObject("properties");
        assertFalse(uploaded.has("cleared_key"));
    }

    @Test
    public void eventProperties_overrideStaticAndDynamicInUpload() throws Exception {
        JSONObject staticProps = new JSONObject();
        staticProps.put("source", "static");
        staticProps.put("static_only", "S");
        staticProps.put("shared_key", "from_static");
        TDAnalytics.setSuperProperties(staticProps);
        TDAnalytics.setDynamicSuperProperties(new TDAnalytics.TDDynamicSuperPropertiesHandler() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                try {
                    JSONObject dyn = new JSONObject();
                    dyn.put("source", "dynamic");
                    dyn.put("dynamic_only", "D");
                    dyn.put("shared_key", "from_dynamic");
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
        assertEquals("S", uploaded.getString("static_only"));
        assertEquals("D", uploaded.getString("dynamic_only"));
        assertEquals("from_dynamic", uploaded.getString("shared_key"));
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
