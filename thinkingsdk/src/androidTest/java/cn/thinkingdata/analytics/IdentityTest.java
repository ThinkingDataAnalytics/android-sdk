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
        enqueueResponses(server, 5);
        resetSdkState(sdk);
        clearDbEvents(context, APP_ID);
    }

    @After
    public void tearDown() { /* nothing extra */ }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    public void login_setsLoginId() throws InterruptedException {
        sdk.login("user_001");
        Thread.sleep(300);
        assertEquals("user_001", sdk.getLoginId());
    }

    @Test
    public void login_loginIdMatchesGetLoginId() throws InterruptedException {
        sdk.login("user_abc");
        Thread.sleep(300);
        assertEquals("user_abc", sdk.getLoginId());
    }

    @Test
    public void login_doesNotChangeDistinctId() throws InterruptedException {
        String distinctIdBefore = sdk.getDistinctId();
        sdk.login("user_002");
        Thread.sleep(300);
        assertEquals("login() must not change the distinctId",
                distinctIdBefore, sdk.getDistinctId());
    }

    @Test
    public void login_multiple_lastLoginIdWins() throws InterruptedException {
        sdk.login("first_user");
        Thread.sleep(200);
        sdk.login("second_user");
        Thread.sleep(200);
        assertEquals("second_user", sdk.getLoginId());
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    public void logout_clearsLoginId() throws InterruptedException {
        sdk.login("user_003");
        Thread.sleep(300);
        sdk.logout();
        Thread.sleep(300);
        String loginId = sdk.getLoginId();
        assertTrue("logout() must clear the loginId",
                loginId == null || loginId.isEmpty());
    }

    @Test
    public void logout_withoutLogin_noException() throws InterruptedException {
        sdk.logout(); // already reset by @Before, but explicit extra call
        Thread.sleep(200);
        String loginId = sdk.getLoginId();
        assertTrue("loginId must be empty after logout() with no prior login",
                loginId == null || loginId.isEmpty());
    }

    @Test
    public void logout_doesNotChangeDistinctId() throws InterruptedException {
        sdk.login("user_004");
        Thread.sleep(200);
        String distinctIdBefore = sdk.getDistinctId();
        sdk.logout();
        Thread.sleep(300);
        assertEquals("logout() must not change the distinctId",
                distinctIdBefore, sdk.getDistinctId());
    }

    // ── identify ──────────────────────────────────────────────────────────────

    @Test
    public void identify_changesDistinctId() throws InterruptedException {
        sdk.identify("custom_distinct_id_xyz");
        Thread.sleep(300);
        assertEquals("custom_distinct_id_xyz", sdk.getDistinctId());
    }

    @Test
    public void identify_doesNotAffectLoginId() throws InterruptedException {
        sdk.login("user_005");
        Thread.sleep(200);
        sdk.identify("new_distinct_id");
        Thread.sleep(300);
        assertEquals("identify() must not affect the loginId",
                "user_005", sdk.getLoginId());
    }

    // ── combined ──────────────────────────────────────────────────────────────

    @Test
    public void loginAndIdentify_bothReflected() throws InterruptedException {
        sdk.identify("distinct_from_test");
        Thread.sleep(200);
        sdk.login("account_from_test");
        Thread.sleep(300);
        assertEquals("distinct_from_test", sdk.getDistinctId());
        assertEquals("account_from_test", sdk.getLoginId());
    }
}
