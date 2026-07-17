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

import java.util.TimeZone;

import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.core.preset.TDPresetUtils;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Instrumented tests for {@link TDAnalytics#getPresetProperties()}.
 *
 * <p>Validates returned field values against device/system sources and that the same
 * values appear in flushed track payloads via the facade APIs.
 */
@RunWith(AndroidJUnit4.class)
public class FacadePresetPropertiesTest extends BaseSdkTest {

    private static final String APP_ID = "at_facade_preset_001";

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
        clearPresetDisableList();
        clearDbEvents(context, APP_ID);
        Thread.sleep(400);
        drainPendingRequests(server);
    }

    @After
    public void tearDown() throws Exception {
        drainPendingRequests(server);
    }

    @Test
    public void getPresetProperties_staticFields_matchDevice() throws Exception {
        TDPresetProperties preset = TDAnalytics.getPresetProperties();
        assertNotNull(preset);

        assertFalse("os must not be empty", TextUtils.isEmpty(preset.os));
        assertTrue("os should identify Android",
                preset.os.toLowerCase().contains("android"));
        assertFalse("os_version must not be empty", TextUtils.isEmpty(preset.osVersion));
        assertEquals(Build.VERSION.RELEASE, preset.osVersion);

        assertEquals(context.getPackageName(), preset.bundleId);
        assertEquals(Build.MANUFACTURER, preset.manufacture);
        assertEquals(Build.MODEL, preset.deviceModel);

        int[] expectedScreenSize = getExpectedScreenSize(context);
        assertEquals(expectedScreenSize[0], preset.screenWidth);
        assertEquals(expectedScreenSize[1], preset.screenHeight);

        assertFalse("device_id must not be empty", TextUtils.isEmpty(preset.deviceId));
        assertFalse("system_language must not be empty",
                TextUtils.isEmpty(preset.systemLanguage));

        if (hasAppVersion(context)) {
            assertEquals(getExpectedAppVersion(context), preset.appVersion);
        }

        assertTrue("device_type must be Phone or Tablet",
                "Phone".equals(preset.deviceType) || "Tablet".equals(preset.deviceType));
    }

    @Test
    public void getPresetProperties_dynamicFields_arePopulated() {
        TDPresetProperties preset = TDAnalytics.getPresetProperties();
        assertNotNull(preset);

        assertFalse("network_type must not be empty", TextUtils.isEmpty(preset.networkType));
        assertFalse("ram must not be empty", TextUtils.isEmpty(preset.ram));
        assertFalse("disk must not be empty", TextUtils.isEmpty(preset.disk));
        assertTrue("fps must be non-negative", preset.fps >= 0);

        double expectedOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis())
                / (1000.0 * 60 * 60);
        assertEquals("zone_offset should match default timezone",
                expectedOffset, preset.zoneOffset, 1e-6);
    }

    @Test
    public void getPresetProperties_matchesSdkInstance() {
        TDPresetProperties facade = TDAnalytics.getPresetProperties();
        TDPresetProperties instance = sdk.getPresetProperties();

        assertEquals(instance.os, facade.os);
        assertEquals(instance.osVersion, facade.osVersion);
        assertEquals(instance.bundleId, facade.bundleId);
        assertEquals(instance.manufacture, facade.manufacture);
        assertEquals(instance.deviceModel, facade.deviceModel);
        assertEquals(instance.screenWidth, facade.screenWidth);
        assertEquals(instance.screenHeight, facade.screenHeight);
        assertEquals(instance.deviceId, facade.deviceId);
        assertEquals(instance.systemLanguage, facade.systemLanguage);
        assertEquals(instance.appVersion, facade.appVersion);
        assertEquals(instance.deviceType, facade.deviceType);
        assertEquals(instance.isSimulator, facade.isSimulator);
        assertEquals(instance.networkType, facade.networkType);
        assertEquals(instance.ram, facade.ram);
        assertEquals(instance.disk, facade.disk);
        assertEquals(instance.zoneOffset, facade.zoneOffset, 1e-9);
    }

    @Test
    public void getPresetProperties_toEventPresetProperties_hasRequiredKeys() throws Exception {
        JSONObject json = TDAnalytics.getPresetProperties().toEventPresetProperties();
        assertNotNull(json);

        assertTrue(json.has(TDPresetUtils.KEY_OS));
        assertTrue(json.has(TDPresetUtils.KEY_OS_VERSION));
        assertTrue(json.has(TDPresetUtils.KEY_BUNDLE_ID));
        assertTrue(json.has(TDPresetUtils.KEY_MANUFACTURER));
        assertTrue(json.has(TDPresetUtils.KEY_DEVICE_MODEL));
        assertTrue(json.has(TDPresetUtils.KEY_SCREEN_WIDTH));
        assertTrue(json.has(TDPresetUtils.KEY_SCREEN_HEIGHT));
        assertTrue(json.has(TDPresetUtils.KEY_DEVICE_ID));
        assertTrue(json.has(TDPresetUtils.KEY_SYSTEM_LANGUAGE));
        assertTrue(json.has(TDPresetUtils.KEY_DEVICE_TYPE));
        assertTrue(json.has(TDPresetUtils.KEY_NETWORK_TYPE));
        assertTrue(json.has(TDPresetUtils.KEY_SIMULATOR));
        assertTrue(json.has(TDConstants.KEY_ZONE_OFFSET));
        assertTrue(json.has(TDConstants.KEY_RAM));
        assertTrue(json.has(TDConstants.KEY_DISK));
    }

    @Test
    public void getPresetProperties_valuesMatchUploadedTrack() throws Exception {
        TDPresetProperties preset = TDAnalytics.getPresetProperties();
        trackAndWaitFacade("facade_preset_match", null);

        JSONArray events = decodeRequestBody(flushAndAssertFacade(server));
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
        assertEquals(preset.deviceType, uploaded.getString(TDPresetUtils.KEY_DEVICE_TYPE));
        assertEquals(preset.isSimulator, uploaded.getBoolean(TDPresetUtils.KEY_SIMULATOR));
        assertEquals(preset.networkType, uploaded.getString(TDPresetUtils.KEY_NETWORK_TYPE));
        assertEquals(preset.ram, uploaded.getString(TDConstants.KEY_RAM));
        assertEquals(preset.disk, uploaded.getString(TDConstants.KEY_DISK));
        assertEquals(preset.zoneOffset, uploaded.getDouble(TDConstants.KEY_ZONE_OFFSET), 1e-9);

        if (TextUtils.isEmpty(preset.appVersion)) {
            assertFalse(uploaded.has(TDPresetUtils.KEY_APP_VERSION));
        } else {
            assertEquals(preset.appVersion, uploaded.getString(TDPresetUtils.KEY_APP_VERSION));
        }
    }

    /**
     * Copy-isolation regression (v3.4.5): {@code currentPresetProperties()} now copies
     * {@code mDeviceInfo} into a fresh map before trimming, avoiding the new R8 (AGP 9.2.1)
     * immutable-parameter crash. The returned preset must exclude the lib keys and stay
     * stable across repeated calls (source device-info must never be mutated).
     */
    @Test
    public void getPresetProperties_excludesLibKeys_andStableAcrossCalls() throws Exception {
        JSONObject first = TDAnalytics.getPresetProperties().toEventPresetProperties();
        assertNotNull(first);
        assertFalse("#lib must be trimmed from preset properties",
                first.has(TDConstants.KEY_LIB));
        assertFalse("#lib_version must be trimmed from preset properties",
                first.has(TDConstants.KEY_LIB_VERSION));

        // Repeated calls must not mutate the source map — required keys stay present
        // and lib keys stay absent every time.
        for (int i = 0; i < 5; i++) {
            JSONObject repeat = TDAnalytics.getPresetProperties().toEventPresetProperties();
            assertFalse(repeat.has(TDConstants.KEY_LIB));
            assertFalse(repeat.has(TDConstants.KEY_LIB_VERSION));
            assertTrue(repeat.has(TDPresetUtils.KEY_OS));
            assertTrue(repeat.has(TDPresetUtils.KEY_DEVICE_ID));
            assertEquals(first.getString(TDPresetUtils.KEY_OS),
                    repeat.getString(TDPresetUtils.KEY_OS));
            assertEquals(first.getString(TDPresetUtils.KEY_DEVICE_ID),
                    repeat.getString(TDPresetUtils.KEY_DEVICE_ID));
        }
    }

    private static boolean hasAppVersion(Context ctx) throws PackageManager.NameNotFoundException {
        return !TextUtils.isEmpty(getExpectedAppVersion(ctx));
    }

    private static String getExpectedAppVersion(Context ctx)
            throws PackageManager.NameNotFoundException {
        PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        return info.versionName;
    }
}
