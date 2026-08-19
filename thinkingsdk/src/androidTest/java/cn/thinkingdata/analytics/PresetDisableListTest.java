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

import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.core.preset.TDPresetUtils;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Tests for {@code TDDisPresetProperties} disable-list behavior.
 *
 * <p>Uses {@link TDPresetProperties#initDisableList(String[])} to simulate the
 * {@code TDDisPresetProperties} string-array resource. Dynamic preset keys added at
 * track time ({@code #network_type}, {@code #ram}, {@code #disk}) honor this list.
 */
@RunWith(AndroidJUnit4.class)
public class PresetDisableListTest extends BaseSdkTest {

    private static final String APP_ID = "at_disable_001";
    private static final String[] DISABLED_DYNAMIC_KEYS = new String[] {
            TDPresetUtils.KEY_NETWORK_TYPE,
            TDConstants.KEY_RAM,
            TDConstants.KEY_DISK,
    };

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
        clearPresetDisableList();
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        TDPresetProperties.initDisableList(DISABLED_DYNAMIC_KEYS);
        prepareBetweenTests(context, APP_ID, server, sdk, 10);
        clearDbEvents(context, APP_ID);
    }

    @After
    public void tearDown() throws Exception {
        clearPresetDisableList();
        cleanupAfterTest(server);
    }

    @Test
    public void disableList_getPresetProperties_excludesDisabledKeys() {
        TDPresetProperties preset = sdk.getPresetProperties();
        JSONObject presetJson = preset.toEventPresetProperties();

        assertFalse("Disabled #network_type must be absent from preset properties",
                presetJson.has(TDPresetUtils.KEY_NETWORK_TYPE));
        assertFalse("Disabled #ram must be absent from preset properties",
                presetJson.has(TDConstants.KEY_RAM));
        assertFalse("Disabled #disk must be absent from preset properties",
                presetJson.has(TDConstants.KEY_DISK));
    }

    @Test
    public void disableList_getPresetProperties_keepsEnabledKeys() {
        TDPresetProperties preset = sdk.getPresetProperties();
        JSONObject presetJson = preset.toEventPresetProperties();

        assertTrue(presetJson.has(TDPresetUtils.KEY_OS));
        assertTrue(presetJson.has(TDPresetUtils.KEY_DEVICE_ID));
        assertTrue(presetJson.has(TDConstants.KEY_ZONE_OFFSET));
    }

    @Test
    public void disableList_uploadedEvent_excludesDisabledKeys() throws Exception {
        trackAndWait(sdk, "disable_list_event", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject properties = events.getJSONObject(0).getJSONObject("properties");

        assertFalse("Disabled #network_type must be absent from uploaded event",
                properties.has(TDPresetUtils.KEY_NETWORK_TYPE));
        assertFalse("Disabled #ram must be absent from uploaded event",
                properties.has(TDConstants.KEY_RAM));
        assertFalse("Disabled #disk must be absent from uploaded event",
                properties.has(TDConstants.KEY_DISK));
    }

    @Test
    public void disableList_uploadedEvent_keepsEnabledKeys() throws Exception {
        trackAndWait(sdk, "disable_list_keep_event", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject properties = events.getJSONObject(0).getJSONObject("properties");

        assertTrue(properties.has(TDPresetUtils.KEY_OS));
        assertTrue(properties.has(TDPresetUtils.KEY_DEVICE_ID));
        assertTrue(properties.has(TDConstants.KEY_ZONE_OFFSET));
    }
}
