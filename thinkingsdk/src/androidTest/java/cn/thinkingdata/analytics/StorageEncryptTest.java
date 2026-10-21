/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import cn.thinkingdata.analytics.persistence.LocalStorageType;
import cn.thinkingdata.core.sp.TDStorageEncryptPlugin;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Instrumented tests for local storage encryption and upgrade from legacy plaintext data.
 */
@RunWith(AndroidJUnit4.class)
public class StorageEncryptTest extends BaseSdkTest {

    private static final String APP_ID = "at_encrypt_001";
    private static final String LEGACY_DISTINCT_ID = "legacy_distinct_id";
    private static final String LEGACY_LOGIN_ID = "legacy_login_id";
    private static final String LEGACY_SUPER_KEY = "legacy_super_key";
    private static final String LEGACY_SUPER_VALUE = "legacy_super_value";

    private static Context context;
    private static MockWebServer server;

    @BeforeClass
    public static void classSetUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        server = new MockWebServer();
        server.start();
    }

    @AfterClass
    public static void classTearDown() throws Exception {
        clearSdkData(context, APP_ID);
        server.shutdown();
    }

    @Test
    public void encrypt_upgrade_sdkInit_readsLegacyPlaintextAfterEnableEncrypt() throws Exception {
        clearSdkData(context, APP_ID);

        JSONObject legacySuperProps = new JSONObject();
        legacySuperProps.put(LEGACY_SUPER_KEY, LEGACY_SUPER_VALUE);
        writeLegacyPlaintextStorage(context, APP_ID, LEGACY_DISTINCT_ID,
                LEGACY_LOGIN_ID, legacySuperProps);

        TDAnalytics.encryptLocalStorage();
        ThinkingAnalyticsSDK sdk = initSdkPreserveStorage(context, APP_ID, server);
        Thread.sleep(500);

        assertEquals(LEGACY_DISTINCT_ID, sdk.getDistinctId());
        assertEquals(LEGACY_LOGIN_ID, sdk.getLoginId());
        assertEquals(LEGACY_SUPER_VALUE,
                sdk.getSuperProperties().getString(LEGACY_SUPER_KEY));
    }

    @Test
    public void encrypt_upgrade_thenNewWrite_migratesAndEncrypts() throws Exception {
        clearSdkData(context, APP_ID);

        JSONObject legacySuperProps = new JSONObject();
        legacySuperProps.put(LEGACY_SUPER_KEY, LEGACY_SUPER_VALUE);
        writeLegacyPlaintextStorage(context, APP_ID, LEGACY_DISTINCT_ID,
                LEGACY_LOGIN_ID, legacySuperProps);

        TDAnalytics.encryptLocalStorage();
        assertEquals(LEGACY_DISTINCT_ID,
                reloadStorageValue(context, APP_ID, LocalStorageType.IDENTIFY, ""));

        CommonStoragePluginHelper.saveIdentify(context, APP_ID, "post_upgrade_distinct");
        String rawValue = readRawStorageValue(context, APP_ID, "identifyID");
        assertNotEquals("post_upgrade_distinct", rawValue);
        assertEquals("post_upgrade_distinct",
                TDStorageEncryptPlugin.getInstance().decryptData(rawValue));
    }

    /**
     * Small helper to write through a fresh storage plugin instance.
     */
    private static final class CommonStoragePluginHelper {
        private CommonStoragePluginHelper() {}

        static void saveIdentify(Context ctx, String appId, String identifyId) {
            cn.thinkingdata.analytics.persistence.CommonStoragePlugin plugin =
                    new cn.thinkingdata.analytics.persistence.CommonStoragePlugin(ctx, appId);
            plugin.save(LocalStorageType.IDENTIFY, identifyId);
        }
    }
}
