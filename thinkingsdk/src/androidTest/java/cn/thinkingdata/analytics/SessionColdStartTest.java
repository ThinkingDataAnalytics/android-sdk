/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.Map;

import cn.thinkingdata.analytics.persistence.CommonStorageManager;
import cn.thinkingdata.analytics.session.SessionManager;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Cold start across a real process death. Each method is a separate
 * {@code am instrument} invocation. The host force-stops the test package
 * between them. {@code resumeAfterFiveMinutes} must run only after five
 * minutes have passed since {@code recordSession}.
 */
@RunWith(AndroidJUnit4.class)
public class SessionColdStartTest extends BaseSdkTest {

    static final String APP_ID = "at_session_cold_001";
    private static final String MARKER = "session-cold-start.txt";
    private static final String TAG = "SessionColdStart";

    @Test
    public void recordSession() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        clearSdkData(context, APP_ID);
        clearSessionManagers();
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            ThinkingAnalyticsSDK sdk = openSdk(context, server);
            assertEquals(TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS, sdk.mConfig.getSessionTimeout());
            trackAndWait(sdk, "cold_start_seed", null);

            String sessionId = sdk.mSessionManager.getSessionId();
            long lastEventTime = new CommonStorageManager(context, sdk.mConfig.getName())
                    .getAnalyticsSessionLastEventTime(-1L);
            assertTrue(sessionId != null && sessionId.length() > 0);
            assertTrue(lastEventTime > 0);
            writeMarker(context, sessionId, lastEventTime);
            Log.i(TAG, "RECORDED id=" + sessionId + " last=" + lastEventTime);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void resumeWithinFiveMinutes() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Marker marker = readMarker(context);
        long gap = System.currentTimeMillis() - marker.lastEventTime;
        assertTrue("cold start happened after the 5 minute window, gapMs=" + gap,
                gap >= 0 && gap < TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS);

        clearSessionManagers();
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            String storedId = new CommonStorageManager(context, APP_ID).getAnalyticsSessionId("");
            assertEquals(marker.sessionId, storedId);

            ThinkingAnalyticsSDK sdk = openSdk(context, server);
            assertEquals(marker.sessionId, sdk.mSessionManager.getSessionId());
            trackAndWait(sdk, "cold_start_resume", null);
            assertEquals(marker.sessionId, sdk.mSessionManager.getSessionId());
            Log.i(TAG, "KEPT id=" + marker.sessionId + " gapMs=" + gap);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void resumeAfterFiveMinutes() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Marker marker = readMarker(context);
        long gap = System.currentTimeMillis() - marker.lastEventTime;
        assertTrue("silence has not reached 5 minutes, gapMs=" + gap,
                gap >= TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS);

        clearSessionManagers();
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            String storedId = new CommonStorageManager(context, APP_ID).getAnalyticsSessionId("");
            assertEquals(marker.sessionId, storedId);

            ThinkingAnalyticsSDK sdk = openSdk(context, server);
            assertEquals(TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS, sdk.mConfig.getSessionTimeout());
            assertEquals(marker.sessionId, sdk.mSessionManager.getSessionId());

            trackAndWait(sdk, "cold_start_expired", null);
            String next = sdk.mSessionManager.getSessionId();
            assertNotEquals(marker.sessionId, next);
            long sessionStart = Long.parseLong(next.replace("-", "").substring(0, 12), 16);
            assertTrue(Math.abs(System.currentTimeMillis() - sessionStart) < 30_000L);
            Log.i(TAG, "ROTATED from=" + marker.sessionId + " to=" + next + " gapMs=" + gap);
        } finally {
            server.shutdown();
        }
    }

    private static ThinkingAnalyticsSDK openSdk(Context context, MockWebServer server) throws Exception {
        TDConfig config = TDConfig.getInstance(context, APP_ID, server.url("/").toString());
        config.disableRConfig = true;
        config.setSessionTimeout(TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS);
        ThinkingAnalyticsSDK sdk = ThinkingAnalyticsSDK.sharedInstance(config);
        Thread.sleep(1000);
        return sdk;
    }

    @SuppressWarnings("unchecked")
    private static void clearSessionManagers() throws Exception {
        Field field = SessionManager.class.getDeclaredField("INSTANCES");
        field.setAccessible(true);
        Map<String, SessionManager> instances = (Map<String, SessionManager>) field.get(null);
        instances.clear();
    }

    private static void writeMarker(Context context, String sessionId, long lastEventTime) throws Exception {
        FileWriter writer = new FileWriter(markerFile(context), false);
        writer.write(sessionId + "\n" + lastEventTime + "\n");
        writer.close();
    }

    private static Marker readMarker(Context context) throws Exception {
        File file = markerFile(context);
        assertTrue("missing marker from recordSession: " + file, file.isFile());
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String sessionId = reader.readLine();
        String last = reader.readLine();
        reader.close();
        return new Marker(sessionId, Long.parseLong(last));
    }

    private static File markerFile(Context context) {
        return new File(context.getFilesDir(), MARKER);
    }

    private static final class Marker {
        final String sessionId;
        final long lastEventTime;

        Marker(String sessionId, long lastEventTime) {
            this.sessionId = sessionId;
            this.lastEventTime = lastEventTime;
        }
    }
}
