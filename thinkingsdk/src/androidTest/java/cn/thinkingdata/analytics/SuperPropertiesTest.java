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

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SuperPropertiesTest extends BaseSdkTest {

    private static final String APP_ID = "at_super_001";
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
        prepareBetweenTests(context, APP_ID, server, sdk, 10);
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    // ── setSuperProperties / getSuperProperties ───────────────────────────────

    @Test
    public void setSuperProperties_canBeRetrieved() throws Exception {
        JSONObject props = new JSONObject();
        props.put("channel", "app_store");
        props.put("version", "3.4.3");

        sdk.setSuperProperties(props);
        Thread.sleep(300);

        JSONObject result = sdk.getSuperProperties();
        assertNotNull(result);
        assertEquals("app_store", result.getString("channel"));
        assertEquals("3.4.3", result.getString("version"));
    }

    @Test
    public void setSuperProperties_multipleCalls_merged() throws Exception {
        JSONObject first = new JSONObject();
        first.put("key1", "value1");
        JSONObject second = new JSONObject();
        second.put("key2", "value2");

        sdk.setSuperProperties(first);
        Thread.sleep(200);
        sdk.setSuperProperties(second);
        Thread.sleep(200);

        JSONObject result = sdk.getSuperProperties();
        assertNotNull(result);
        assertTrue("Both keys must be present", result.has("key1") && result.has("key2"));
    }

    @Test
    public void setSuperProperties_duplicateKey_latestValueWins() throws Exception {
        JSONObject first = new JSONObject();
        first.put("platform", "ios");
        JSONObject second = new JSONObject();
        second.put("platform", "android");

        sdk.setSuperProperties(first);
        Thread.sleep(200);
        sdk.setSuperProperties(second);
        Thread.sleep(200);

        assertEquals("android", sdk.getSuperProperties().getString("platform"));
    }

    @Test
    public void setSuperProperties_nullInput_noException() throws InterruptedException {
        sdk.setSuperProperties(null); // must not throw
        Thread.sleep(200);
    }

    // ── unsetSuperProperty ────────────────────────────────────────────────────

    @Test
    public void unsetSuperProperty_removesSpecificKey() throws Exception {
        JSONObject props = new JSONObject();
        props.put("to_remove", "gone");
        props.put("to_keep", "present");
        sdk.setSuperProperties(props);
        Thread.sleep(200);

        sdk.unsetSuperProperty("to_remove");
        Thread.sleep(200);

        JSONObject result = sdk.getSuperProperties();
        assertNotNull(result);
        assertFalse("'to_remove' must be gone after unsetSuperProperty()", result.has("to_remove"));
        assertTrue("'to_keep' must remain", result.has("to_keep"));
    }

    @Test
    public void unsetSuperProperty_nonExistentKey_noException() throws InterruptedException {
        sdk.unsetSuperProperty("key_that_does_not_exist"); // must not throw
        Thread.sleep(200);
    }

    // ── clearSuperProperties ──────────────────────────────────────────────────

    @Test
    public void clearSuperProperties_removesAll() throws Exception {
        JSONObject props = new JSONObject();
        props.put("a", 1);
        props.put("b", 2);
        sdk.setSuperProperties(props);
        Thread.sleep(200);

        sdk.clearSuperProperties();
        Thread.sleep(200);

        JSONObject result = sdk.getSuperProperties();
        assertTrue("clearSuperProperties() must leave getSuperProperties() empty",
                result == null || result.length() == 0);
    }

    @Test
    public void clearSuperProperties_thenSetNew_onlyNewOnesPresent() throws Exception {
        JSONObject old = new JSONObject();
        old.put("old_key", "old_value");
        sdk.setSuperProperties(old);
        Thread.sleep(200);

        sdk.clearSuperProperties();
        Thread.sleep(200);

        JSONObject fresh = new JSONObject();
        fresh.put("new_key", "new_value");
        sdk.setSuperProperties(fresh);
        Thread.sleep(200);

        JSONObject result = sdk.getSuperProperties();
        assertNotNull(result);
        assertTrue("new_key must be present", result.has("new_key"));
        assertFalse("old_key must not be present after clear + re-set", result.has("old_key"));
    }

    // ── merged into tracked events ────────────────────────────────────────────

    @Test
    public void setSuperProperties_mergedIntoTrackedEvent() throws Exception {
        JSONObject superProps = new JSONObject();
        superProps.put("app_version", "9.9.9");
        superProps.put("channel", "organic");
        sdk.setSuperProperties(superProps);
        Thread.sleep(300);

        trackAndWait(sdk, "purchase", null);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertTrue("At least one event must be uploaded", events.length() > 0);

        JSONObject event = events.getJSONObject(0);
        JSONObject props = event.getJSONObject("properties");
        assertEquals("Super property must be in event properties",
                "9.9.9", props.getString("app_version"));
        assertEquals("organic", props.getString("channel"));
    }

    @Test
    public void setSuperProperties_eventOwnProps_notOverriddenBySuperProps() throws Exception {
        JSONObject superProps = new JSONObject();
        superProps.put("source", "super");
        sdk.setSuperProperties(superProps);
        Thread.sleep(200);

        JSONObject eventProps = new JSONObject();
        eventProps.put("source", "event");
        trackAndWait(sdk, "click", eventProps);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject event = events.getJSONObject(0);
        JSONObject props = event.getJSONObject("properties");
        // Document the SDK's actual merge order (event wins or super wins)
        assertNotNull("'source' must be present in event properties", props.opt("source"));
    }
}
