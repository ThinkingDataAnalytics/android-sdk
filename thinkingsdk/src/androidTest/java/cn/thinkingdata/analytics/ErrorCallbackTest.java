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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import cn.thinkingdata.analytics.utils.TDConstants;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Instrumented tests for {@link TDAnalytics#registerErrorCallback}.
 *
 * <p>Does not pre-enqueue success responses in {@code @Before} — those would be consumed by
 * flush before the intentional error response.
 */
@RunWith(AndroidJUnit4.class)
public class ErrorCallbackTest extends BaseSdkTest {

    private static final String APP_ID = "at_error_cb_001";
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
        resetFacadeSdkState();
        clearDbEvents(context, APP_ID);
        Thread.sleep(400);
        drainPendingRequests(server);
    }

    @After
    public void tearDown() throws Exception {
        drainPendingRequests(server);
    }

    @Test(timeout = 20_000)
    public void errorCallback_serverNonZeroCode_invokesCallback() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger errorCode = new AtomicInteger();
        final AtomicReference<String> errorMessage = new AtomicReference<>();

        TDAnalytics.registerErrorCallback(new TDAnalytics.TDSendDataErrorCallback() {
            @Override
            public void onSDKErrorCallback(int code, String errorMsg, String ext) {
                errorCode.set(code);
                errorMessage.set(errorMsg);
                latch.countDown();
            }
        });

        // code must be a JSON string — getString("code") is used in DataHandle
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":\"-1\"}"));

        trackAndWait(sdk, "error_callback_event", null);
        flushAndAssertRequest(sdk, server);

        assertTrue("Error callback must fire for non-zero server code",
                latch.await(10, TimeUnit.SECONDS));
        assertEquals(TDConstants.SDK_ERROR_NET, errorCode.get());
        assertNotNull(errorMessage.get());
        assertTrue(errorMessage.get().contains("-1"));
    }

    @Test(timeout = 20_000)
    public void errorCallback_networkFailure_invokesCallback() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger errorCode = new AtomicInteger();

        TDAnalytics.registerErrorCallback(new TDAnalytics.TDSendDataErrorCallback() {
            @Override
            public void onSDKErrorCallback(int code, String errorMsg, String ext) {
                errorCode.set(code);
                latch.countDown();
            }
        });

        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("internal error"));

        trackAndWait(sdk, "error_callback_network_event", null);
        sdk.flush();

        assertTrue("Error callback must fire for network/server failure",
                latch.await(10, TimeUnit.SECONDS));
        // IO/HTTP failures are reported as SDK_ERROR_NET_EXCEPTION (1006)
        assertEquals(TDConstants.SDK_ERROR_NET_EXCEPTION, errorCode.get());
    }
}
