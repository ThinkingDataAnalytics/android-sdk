/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

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
 * Tests for preset properties: getter correctness and presence in uploaded event payloads.
 */
@RunWith(AndroidJUnit4.class)
public class PresetPropertiesTest extends BaseSdkTest {

    private static final String APP_ID = "at_preset_001";
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
        clearDbEvents(context, APP_ID);
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    // ── getPresetProperties() correctness ─────────────────────────────────────

    @Test
    public void presetProperties_staticFields_areValid() throws Exception {
        TDPresetProperties preset = sdk.getPresetProperties();
        assertNotNull(preset);

        assertFalse("os must not be empty", preset.os == null || preset.os.isEmpty());
        assertFalse("os_version must not be empty",
                preset.osVersion == null || preset.osVersion.isEmpty());
        assertEquals(context.getPackageName(), preset.bundleId);
        assertEquals(Build.MANUFACTURER, preset.manufacture);
        assertEquals(Build.MODEL, preset.deviceModel);
        assertTrue("screen_width must be positive", preset.screenWidth > 0);
        assertTrue("screen_height must be positive", preset.screenHeight > 0);
        assertFalse("device_id must not be empty",
                preset.deviceId == null || preset.deviceId.isEmpty());
        assertFalse("system_language must not be empty",
                preset.systemLanguage == null || preset.systemLanguage.isEmpty());
        if (hasAppVersion(context)) {
            assertFalse("app_version must not be empty when PackageManager provides one",
                    preset.appVersion == null || preset.appVersion.isEmpty());
            assertEquals(getExpectedAppVersion(context), preset.appVersion);
        }
        assertTrue("device_type must be Phone or Tablet",
                "Phone".equals(preset.deviceType) || "Tablet".equals(preset.deviceType));
    }

    @Test
    public void presetProperties_dynamicFields_areValid() {
        TDPresetProperties preset = sdk.getPresetProperties();
        assertNotNull(preset);

        assertFalse("network_type must not be empty",
                preset.networkType == null || preset.networkType.isEmpty());
        assertFalse("ram must not be empty", preset.ram == null || preset.ram.isEmpty());
        assertFalse("disk must not be empty", preset.disk == null || preset.disk.isEmpty());
    }

    @Test
    public void presetProperties_toEventPresetProperties_containsRequiredKeys() throws Exception {
        JSONObject presetJson = sdk.getPresetProperties().toEventPresetProperties();
        assertNotNull(presetJson);
        assertCommonPresetKeysPresent(context, presetJson);
        assertTrue(presetJson.has(TDConstants.KEY_ZONE_OFFSET));
    }

    // ── preset properties in uploaded track payload ─────────────────────────────

    @Test
    public void track_uploadedPresetProperties_containRequiredKeys() throws Exception {
        trackAndWait(sdk, "preset_keys_check", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject properties = events.getJSONObject(0).getJSONObject("properties");
        assertCommonPresetKeysPresent(context, properties);
        assertTrue("Uploaded event must contain #zone_offset",
                properties.has(TDConstants.KEY_ZONE_OFFSET));
        assertTrue("Uploaded event must contain #ram", properties.has(TDConstants.KEY_RAM));
        assertTrue("Uploaded event must contain #disk", properties.has(TDConstants.KEY_DISK));
    }

    @Test
    public void track_uploadedPresetProperties_matchGetPresetProperties() throws Exception {
        TDPresetProperties preset = sdk.getPresetProperties();
        trackAndWait(sdk, "preset_match_check", null);
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject uploaded = events.getJSONObject(0).getJSONObject("properties");

        assertEquals(preset.os, uploaded.getString(TDPresetUtils.KEY_OS));
        assertEquals(preset.osVersion, uploaded.getString(TDPresetUtils.KEY_OS_VERSION));
        assertEquals(preset.bundleId, uploaded.getString(TDPresetUtils.KEY_BUNDLE_ID));
        assertEquals(preset.manufacture, uploaded.getString(TDPresetUtils.KEY_MANUFACTURER));
        assertEquals(preset.deviceModel, uploaded.getString(TDPresetUtils.KEY_DEVICE_MODEL));
        assertEquals(preset.screenWidth, uploaded.getInt(TDPresetUtils.KEY_SCREEN_WIDTH));
        assertEquals(preset.screenHeight, uploaded.getInt(TDPresetUtils.KEY_SCREEN_HEIGHT));
        assertEquals(preset.deviceId, uploaded.getString(TDPresetUtils.KEY_DEVICE_ID));
        assertEquals(preset.systemLanguage, uploaded.getString(TDPresetUtils.KEY_SYSTEM_LANGUAGE));
        if (TextUtils.isEmpty(preset.appVersion)) {
            assertFalse("Uploaded event must omit #app_version when preset value is empty",
                    uploaded.has(TDPresetUtils.KEY_APP_VERSION));
        } else {
            assertEquals(preset.appVersion, uploaded.getString(TDPresetUtils.KEY_APP_VERSION));
        }
        assertEquals(preset.isSimulator, uploaded.getBoolean(TDPresetUtils.KEY_SIMULATOR));
        assertEquals(preset.deviceType, uploaded.getString(TDPresetUtils.KEY_DEVICE_TYPE));
        assertEquals(preset.networkType, uploaded.getString(TDPresetUtils.KEY_NETWORK_TYPE));
        assertEquals(preset.ram, uploaded.getString(TDConstants.KEY_RAM));
        assertEquals(preset.disk, uploaded.getString(TDConstants.KEY_DISK));
        assertEquals(preset.zoneOffset, uploaded.getDouble(TDConstants.KEY_ZONE_OFFSET), 1e-9);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void assertCommonPresetKeysPresent(Context ctx, JSONObject properties)
            throws Exception {
        assertTrue("#os must be present", properties.has(TDPresetUtils.KEY_OS));
        assertTrue("#os_version must be present", properties.has(TDPresetUtils.KEY_OS_VERSION));
        assertTrue("#bundle_id must be present", properties.has(TDPresetUtils.KEY_BUNDLE_ID));
        assertTrue("#manufacturer must be present", properties.has(TDPresetUtils.KEY_MANUFACTURER));
        assertTrue("#device_model must be present", properties.has(TDPresetUtils.KEY_DEVICE_MODEL));
        assertTrue("#screen_width must be present", properties.has(TDPresetUtils.KEY_SCREEN_WIDTH));
        assertTrue("#screen_height must be present", properties.has(TDPresetUtils.KEY_SCREEN_HEIGHT));
        assertTrue("#device_id must be present", properties.has(TDPresetUtils.KEY_DEVICE_ID));
        assertTrue("#system_language must be present", properties.has(TDPresetUtils.KEY_SYSTEM_LANGUAGE));
        if (hasAppVersion(ctx)) {
            assertTrue("#app_version must be present when PackageManager provides one",
                    properties.has(TDPresetUtils.KEY_APP_VERSION));
            assertEquals(getExpectedAppVersion(ctx),
                    properties.getString(TDPresetUtils.KEY_APP_VERSION));
        }
        assertTrue("#simulator must be present", properties.has(TDPresetUtils.KEY_SIMULATOR));
        assertTrue("#device_type must be present", properties.has(TDPresetUtils.KEY_DEVICE_TYPE));
        assertTrue("#network_type must be present", properties.has(TDPresetUtils.KEY_NETWORK_TYPE));
    }

    private static boolean hasAppVersion(Context ctx) throws PackageManager.NameNotFoundException {
        return !TextUtils.isEmpty(getExpectedAppVersion(ctx));
    }

    private static String getExpectedAppVersion(Context ctx) throws PackageManager.NameNotFoundException {
        PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        return info.versionName;
    }
}
