/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

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

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import cn.thinkingdata.analytics.encrypt.TDEncryptUtils;
import cn.thinkingdata.analytics.persistence.ConfigStoragePlugin;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

/**
 * Instrumented tests for report-data AES-GCM encryption and mixed-batch compatibility
 * with legacy ciphertext rows already stored in the local DB.
 */
@RunWith(AndroidJUnit4.class)
public class DataEncryptUploadTest extends BaseSdkTest {

    private static final String APP_ID = "at_data_encrypt_001";
    private static final String LEGACY_ECB_ENVELOPE =
            "{\"ekey\":\"LEGACY_RSA_WRAPPED_AES_KEY\",\"pkv\":1,"
                    + "\"payload\":\"LEGACY_AES_ECB_CIPHERTEXT_PLACEHOLDER\"}";

    private static Context context;
    private static MockWebServer server;
    private static ThinkingAnalyticsSDK sdk;
    private static String testPublicKey;

    @BeforeClass
    public static void classSetUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        server = new MockWebServer();
        server.start();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        KeyPair keyPair = kpg.generateKeyPair();
        testPublicKey = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.NO_WRAP);

        clearSdkData(context, APP_ID);
        removeSdkInstance(context, APP_ID);
        removeEncryptInstance(APP_ID);
        removeConfigInstance(context, APP_ID);

        // Encryption is gated on the receiver's AES-GCM support. Simulate a launch where the flag
        // has already been persisted by a previous remote config fetch, and answer the config
        // request with a body that carries no data object so the flag is left untouched.
        seedGcmSupport(context, APP_ID, ConfigStoragePlugin.GCM_SUPPORTED);
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"code\":\"0\"}"));

        TDConfig config = TDConfig.getInstance(context, APP_ID, server.url("/").toString());
        config.enableEncrypt(1, testPublicKey);
        sdk = ThinkingAnalyticsSDK.sharedInstance(config);
        Thread.sleep(1000);
    }

    @AfterClass
    public static void classTearDown() throws Exception {
        clearSdkData(context, APP_ID);
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        prepareBetweenTests(context, APP_ID, server, sdk, 15);
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    @Test
    public void encryptTrack_newEvent_uploadEnvelopeContainsModeGcm() throws Exception {
        trackAndWait(sdk, "gcm_encrypt_event", null);
        JSONArray data = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertTrue(data.length() > 0);

        JSONObject envelope = data.getJSONObject(0);
        assertTrue(TDEncryptUtils.isEncryptedData(envelope.toString()));
        assertEquals("gcm", envelope.getString("mode"));
        assertTrue(envelope.has("ekey"));
        assertTrue(envelope.has("pkv"));
        assertTrue(envelope.has("payload"));
        assertFalse("payload must not be empty", envelope.getString("payload").isEmpty());
    }

    /**
     * DB already has a legacy encrypted row (no {@code mode} field, historically AES-ECB).
     * A newly tracked event is encrypted with AES-GCM ({@code mode=gcm}).
     * One flush batch must include both envelopes without conversion.
     */
    @Test
    public void flush_dbHasLegacyEcbRow_batchContainsLegacyAndGcm() throws Exception {
        insertLegacyEncryptedEvent(APP_ID, LEGACY_ECB_ENVELOPE);
        assertTrue("legacy row must be present before track", countDbEvents(context, APP_ID) >= 1);

        trackAndWait(sdk, "new_gcm_after_legacy", null);

        JSONArray data = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertTrue("batch must contain at least legacy + new event", data.length() >= 2);

        boolean foundLegacy = false;
        boolean foundGcm = false;
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            assertTrue("each item must look encrypted", TDEncryptUtils.isEncryptedData(item.toString()));
            if (!item.has("mode")) {
                foundLegacy = true;
                assertEquals("LEGACY_RSA_WRAPPED_AES_KEY", item.getString("ekey"));
                assertEquals("LEGACY_AES_ECB_CIPHERTEXT_PLACEHOLDER", item.getString("payload"));
            } else if ("gcm".equals(item.optString("mode"))) {
                foundGcm = true;
                assertFalse(item.getString("ekey").isEmpty());
                assertFalse(item.getString("payload").isEmpty());
            }
        }
        assertTrue("batch must keep legacy ECB envelope as-is", foundLegacy);
        assertTrue("batch must include new GCM envelope with mode=gcm", foundGcm);
    }

    private static void insertLegacyEncryptedEvent(String token, String envelopeJson) {
        File dbFile = context.getDatabasePath("thinkingdata");
        SQLiteDatabase db = SQLiteDatabase.openDatabase(
                dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
        try {
            ContentValues cv = new ContentValues();
            cv.put("clickdata", envelopeJson + "#td#" + envelopeJson.hashCode());
            cv.put("creattime", System.currentTimeMillis());
            cv.put("token", token);
            long rowId = db.insert("events", null, cv);
            assertTrue("legacy encrypted row insert must succeed", rowId > 0);
        } finally {
            db.close();
        }
    }

}
