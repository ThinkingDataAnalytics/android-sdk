/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Tests for user identity: login, logout, identify (setDistinctId), getLoginId, getDistinctId.
 */
@RunWith(AndroidJUnit4.class)
public class IdentityTest extends BaseSdkTest {

    private static final String APP_ID = "at_identity_001";
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
        prepareBetweenTests(context, APP_ID, server, sdk, 5);
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    @Test
    public void login_doesNotChangeDistinctId() throws InterruptedException {
        String distinctIdBefore = sdk.getDistinctId();
        sdk.login("user_002");
        Thread.sleep(300);
        assertEquals("user_002", sdk.getLoginId());
        assertEquals("login() must not change the distinctId",
                distinctIdBefore, sdk.getDistinctId());
    }

    @Test
    public void logout_clearsLoginId() throws InterruptedException {
        String distinctIdBefore = sdk.getDistinctId();
        sdk.login("user_003");
        Thread.sleep(300);
        sdk.logout();
        Thread.sleep(300);
        String loginId = sdk.getLoginId();
        assertTrue("logout() must clear the loginId",
                loginId == null || loginId.isEmpty());
        assertEquals(distinctIdBefore, sdk.getDistinctId());
    }

    @Test
    public void identify_doesNotAffectLoginId() throws InterruptedException {
        sdk.login("user_005");
        Thread.sleep(200);
        sdk.identify("new_distinct_id");
        Thread.sleep(300);
        assertEquals("new_distinct_id", sdk.getDistinctId());
        assertEquals("identify() must not affect the loginId",
                "user_005", sdk.getLoginId());
    }
}
