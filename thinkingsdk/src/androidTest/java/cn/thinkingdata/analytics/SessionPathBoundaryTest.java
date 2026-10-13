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

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Set;

import cn.thinkingdata.analytics.persistence.AnalyticsSessionStore;
import cn.thinkingdata.analytics.persistence.CommonStorageManager;
import cn.thinkingdata.analytics.session.SessionManager;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.core.preset.TDPresetUtils;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Session rules on the real track path. The 5-minute gap is shortened so the
 * device can hit the boundary without waiting out wall-clock silence.
 */
@RunWith(AndroidJUnit4.class)
public class SessionPathBoundaryTest extends BaseSdkTest {

    private static final String APP_ID = "at_session_path_001";
    private static final long TIMEOUT = 2_500L;
    private static final long GAP = 1_600L;

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
        clearDisabledEvents();
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        sdk.mConfig.setSessionTimeout(TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS);
        server.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        clearDisabledEvents();
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        sdk.mConfig.setSessionTimeout(TIMEOUT);
        prepareBetweenTests(context, APP_ID, server, sdk, 12);
    }

    @After
    public void tearDown() throws Exception {
        clearDisabledEvents();
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        sdk.mConfig.setSessionTimeout(TDConfig.DEFAULT_SESSION_TIMEOUT_MILLIS);
        cleanupAfterTest(server);
    }

    @Test
    public void historicalEventTime_keepsSessionAndUsesWallClock() throws Exception {
        sdk.mConfig.setSessionTimeout(60_000L);
        trackAndWait(sdk, "session_now", null);
        String sessionId = sdk.mSessionManager.getSessionId();
        Date historical = new Date(System.currentTimeMillis() - 60 * 60 * 1000L);

        sdk.track("session_historical", null, historical);
        waitForTrackTasks();
        Thread.sleep(150);

        assertEquals(sessionId, sdk.mSessionManager.getSessionId());
        long sessionStart = timestampMillis(sessionId);
        assertTrue(Math.abs(System.currentTimeMillis() - sessionStart) < 30_000L);
        assertTrue(System.currentTimeMillis() - historical.getTime() > 30 * 60 * 1000L);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertEquals(sessionId, sessionIdOf(events, "session_now"));
        assertEquals(sessionId, sessionIdOf(events, "session_historical"));
    }

    @Test
    public void userSet_doesNotCarrySessionId_orExtendSession() throws Exception {
        trackAndWait(sdk, "session_before_user", null);
        String sessionId = sdk.mSessionManager.getSessionId();
        Thread.sleep(GAP);

        JSONObject props = new JSONObject();
        props.put("nickname", "session-probe");
        sdk.user_set(props);
        waitForTrackTasks();
        Thread.sleep(GAP);
        trackAndWait(sdk, "session_after_user", null);

        assertNotEquals(sessionId, sdk.mSessionManager.getSessionId());
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject userSet = findEventByType(events, "user_set");
        assertNotNull(userSet);
        assertFalse(userSet.toString().contains(TDConstants.KEY_SESSION_ID));
        assertEquals(sessionId, sessionIdOf(events, "session_before_user"));
        assertNotEquals(sessionId, sessionIdOf(events, "session_after_user"));
    }

    @Test
    public void disabledEvent_doesNotExtendSession() throws Exception {
        trackAndWait(sdk, "session_before_disabled", null);
        String sessionId = sdk.mSessionManager.getSessionId();
        Thread.sleep(GAP);

        setDisabledEvents("session_ignored");
        sdk.track("session_ignored");
        waitForTrackTasks();
        Thread.sleep(GAP);
        trackAndWait(sdk, "session_after_disabled", null);

        assertNotEquals(sessionId, sdk.mSessionManager.getSessionId());
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertEquals(sessionId, sessionIdOf(events, "session_before_disabled"));
        assertNotEquals(sessionId, sessionIdOf(events, "session_after_disabled"));
        assertNull(findEventByName(events, "session_ignored"));
    }

    @Test
    public void pause_doesNotExtendSession() throws Exception {
        assertStatusDoesNotExtend(ThinkingAnalyticsSDK.TATrackStatus.PAUSE, "session_while_paused");
    }

    @Test
    public void stop_doesNotExtendSession() throws Exception {
        assertStatusDoesNotExtend(ThinkingAnalyticsSDK.TATrackStatus.STOP, "session_while_stopped");
    }

    @Test
    public void saveOnly_extendsSession() throws Exception {
        trackAndWait(sdk, "session_before_save_only", null);
        String sessionId = sdk.mSessionManager.getSessionId();
        Thread.sleep(GAP);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.SAVE_ONLY);
        waitForTrackTasks();
        trackAndWait(sdk, "session_saved_only", null);
        assertEquals(sessionId, sdk.mSessionManager.getSessionId());

        Thread.sleep(GAP);
        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        waitForTrackTasks();
        Thread.sleep(200);
        trackAndWait(sdk, "session_after_save_only", null);

        assertEquals(sessionId, sdk.mSessionManager.getSessionId());
        JSONArray events = collectEvents();
        assertEquals(sessionId, sessionIdOf(events, "session_before_save_only"));
        assertEquals(sessionId, sessionIdOf(events, "session_saved_only"));
        assertEquals(sessionId, sessionIdOf(events, "session_after_save_only"));
    }

    @Test
    public void shrinkTimeout_rotatesOnNextEvent() throws Exception {
        sdk.mConfig.setSessionTimeout(60_000L);
        trackAndWait(sdk, "session_before_shrink", null);
        String sessionId = sdk.mSessionManager.getSessionId();

        sdk.mConfig.setSessionTimeout(400L);
        Thread.sleep(500);
        trackAndWait(sdk, "session_after_shrink", null);

        assertNotEquals(sessionId, sdk.mSessionManager.getSessionId());
    }

    @Test
    public void lengthenTimeout_keepsSession() throws Exception {
        sdk.mConfig.setSessionTimeout(400L);
        trackAndWait(sdk, "session_before_lengthen", null);
        String sessionId = sdk.mSessionManager.getSessionId();

        sdk.mConfig.setSessionTimeout(60_000L);
        Thread.sleep(700);
        trackAndWait(sdk, "session_after_lengthen", null);

        assertEquals(sessionId, sdk.mSessionManager.getSessionId());
    }

    @Test
    public void presetProperties_readSessionWithoutExtending() throws Exception {
        trackAndWait(sdk, "session_before_preset", null);
        String sessionId = sdk.mSessionManager.getSessionId();
        Thread.sleep(GAP);

        JSONObject preset = sdk.getPresetProperties().toEventPresetProperties();
        assertEquals(sessionId, preset.getString(TDConstants.KEY_SESSION_ID));

        Thread.sleep(GAP);
        trackAndWait(sdk, "session_after_preset", null);

        assertNotEquals(sessionId, sdk.mSessionManager.getSessionId());
    }

    @Test
    public void subprocessEvent_doesNotRotateOrExtend() throws Exception {
        trackAndWait(sdk, "session_before_subprocess", null);
        String sessionId = sdk.mSessionManager.getSessionId();
        Thread.sleep(TIMEOUT + 400L);

        JSONObject subprocess = new JSONObject();
        subprocess.put(TDConstants.KEY_SUBPROCESS_TAG, true);
        subprocess.put(TDPresetUtils.KEY_BUNDLE_ID, "cn.thinkingdata.analytics.test");
        trackAndWait(sdk, "session_subprocess", subprocess);
        assertEquals(sessionId, sdk.mSessionManager.getSessionId());

        trackAndWait(sdk, "session_after_subprocess", null);
        assertNotEquals(sessionId, sdk.mSessionManager.getSessionId());

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject subprocessEvent = findEvent(events, "session_subprocess");
        JSONObject properties = subprocessEvent.getJSONObject("properties");
        assertEquals(sessionId, properties.getString(TDConstants.KEY_SESSION_ID));
        assertFalse(properties.has(TDConstants.KEY_SUBPROCESS_TAG));
        assertNotEquals(sessionId, sessionIdOf(events, "session_after_subprocess"));
    }

    @Test
    public void lifecycleTransitions_doNotExtendSession() throws Exception {
        trackAndWait(sdk, "session_before_background", null);
        String sessionId = sdk.mSessionManager.getSessionId();
        Thread.sleep(GAP);

        sdk.appEnterBackground();
        Thread.sleep(GAP);
        sdk.appBecomeActive();
        trackAndWait(sdk, "session_after_background", null);

        assertNotEquals(sessionId, sdk.mSessionManager.getSessionId());
    }

    @Test
    public void appEnd_withinTimeout_extendsSession() throws Exception {
        trackAndWait(sdk, "session_before_app_end", null);
        String sessionId = sdk.mSessionManager.getSessionId();
        sdk.appEnterBackground();
        Thread.sleep(GAP);

        sdk.autoTrack(TDConstants.APP_END_EVENT_NAME, new JSONObject());
        Thread.sleep(400);
        waitForTrackTasks();
        assertEquals(sessionId, sdk.mSessionManager.getSessionId());

        // Farther from background than the timeout, but still inside the window opened by ta_app_end.
        Thread.sleep(GAP);
        trackAndWait(sdk, "session_after_app_end", null);
        assertEquals(sessionId, sdk.mSessionManager.getSessionId());

        Thread.sleep(TIMEOUT + 400L);
        trackAndWait(sdk, "session_after_app_end_quiet", null);
        assertNotEquals(sessionId, sdk.mSessionManager.getSessionId());

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertEquals(sessionId, sessionIdOf(events, TDConstants.APP_END_EVENT_NAME));
        assertEquals(sessionId, sessionIdOf(events, "session_after_app_end"));
        assertNotEquals(sessionId, sessionIdOf(events, "session_after_app_end_quiet"));
    }

    @Test
    public void appEnd_afterTimeout_startsNewSession() throws Exception {
        long timeout = 500L;
        sdk.mConfig.setSessionTimeout(timeout);
        trackAndWait(sdk, "session_before_expired_app_end", null);
        String previousSessionId = sdk.mSessionManager.getSessionId();

        sdk.appEnterBackground();
        Thread.sleep(timeout + 300L);
        sdk.autoTrack(TDConstants.APP_END_EVENT_NAME, new JSONObject());
        waitForTrackTasks();
        Thread.sleep(150L);

        String endSessionId = sdk.mSessionManager.getSessionId();
        assertNotEquals(previousSessionId, endSessionId);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertEquals(previousSessionId,
                sessionIdOf(events, "session_before_expired_app_end"));
        assertEquals(endSessionId, sessionIdOf(events, TDConstants.APP_END_EVENT_NAME));
    }

    @Test
    public void accountTransitions_startNewSessions_sameAccountKeepsSession() throws Exception {
        sdk.mConfig.setSessionTimeout(60_000L);
        sdk.track("session_anonymous");

        sdk.login("account_a");
        sdk.track("session_account_a");

        sdk.login("account_a");
        sdk.track("session_account_a_repeated");

        sdk.login("account_b");
        sdk.track("session_account_b");

        sdk.login("account_a");
        sdk.track("session_account_a_again");

        sdk.logout();
        sdk.track("session_after_logout");
        waitForTrackTasks();
        Thread.sleep(300L);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        String anonymous = sessionIdOf(events, "session_anonymous");
        String accountA = sessionIdOf(events, "session_account_a");
        String accountARepeated = sessionIdOf(events, "session_account_a_repeated");
        String accountB = sessionIdOf(events, "session_account_b");
        String accountAAgain = sessionIdOf(events, "session_account_a_again");
        String afterLogout = sessionIdOf(events, "session_after_logout");

        assertNotEquals(anonymous, accountA);
        assertEquals(accountA, accountARepeated);
        assertNotEquals(accountARepeated, accountB);
        assertNotEquals(accountB, accountAAgain);
        assertNotEquals(accountAAgain, afterLogout);

        assertEquals("account_a", findEvent(events, "session_account_a")
                .getString(TDConstants.KEY_ACCOUNT_ID));
        assertEquals("account_b", findEvent(events, "session_account_b")
                .getString(TDConstants.KEY_ACCOUNT_ID));
        assertFalse(findEvent(events, "session_after_logout")
                .has(TDConstants.KEY_ACCOUNT_ID));
    }

    @Test
    public void lightInstance_sharesSessionId() throws Exception {
        sdk.mConfig.setSessionTimeout(60_000L);
        ThinkingAnalyticsSDK light = sdk.createLightInstance();
        Thread.sleep(1000);

        trackAndWait(sdk, "session_main_instance", null);
        String sessionId = sdk.mSessionManager.getSessionId();
        trackAndWait(light, "session_light_instance", null);

        assertEquals(sessionId, light.mSessionManager.getSessionId());
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertEquals(sessionId, sessionIdOf(events, "session_main_instance"));
        assertEquals(sessionId, sessionIdOf(events, "session_light_instance"));
    }

    @Test
    public void distinctAppId_usesIndependentSession() throws Exception {
        MockWebServer otherServer = new MockWebServer();
        otherServer.start();
        try {
            ThinkingAnalyticsSDK other = initSdk(context, "at_session_path_002", otherServer);
            other.mConfig.setSessionTimeout(60_000L);
            sdk.mConfig.setSessionTimeout(60_000L);

            trackAndWait(sdk, "session_app_a", null);
            trackAndWait(other, "session_app_b", null);
            trackAndWait(sdk, "session_app_a_again", null);

            String sessionA = sdk.mSessionManager.getSessionId();
            assertNotEquals(sessionA, other.mSessionManager.getSessionId());
            assertEquals(sessionA, sdk.mSessionManager.getSessionId());
        } finally {
            otherServer.shutdown();
        }
    }

    @Test
    public void localStorage_reloadsSessionWithinTimeout() throws Exception {
        sdk.mConfig.setSessionTimeout(60_000L);
        trackAndWait(sdk, "session_persisted", null);
        String sessionId = sdk.mSessionManager.getSessionId();

        CommonStorageManager storage = new CommonStorageManager(context, sdk.mConfig.getName());
        assertEquals(sessionId, storage.getAnalyticsSessionId(""));

        SessionManager restored = new SessionManager(new AnalyticsSessionStore(storage));
        assertEquals(sessionId, restored.getSessionId());
        assertEquals(sessionId, restored.onEvent(System.currentTimeMillis(), 60_000L));
    }

    private static void assertStatusDoesNotExtend(ThinkingAnalyticsSDK.TATrackStatus status,
                                                   String droppedEvent) throws Exception {
        trackAndWait(sdk, "session_before_status", null);
        flushAndAssertRequest(sdk, server);
        String sessionId = sdk.mSessionManager.getSessionId();
        Thread.sleep(GAP);

        sdk.setTrackStatus(status);
        waitForTrackTasks();
        sdk.track(droppedEvent);
        waitForTrackTasks();
        Thread.sleep(GAP);

        sdk.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
        waitForTrackTasks();
        Thread.sleep(200);
        drainPendingRequests(server);
        trackAndWait(sdk, "session_after_status", null);

        assertNotEquals(sessionId, sdk.mSessionManager.getSessionId());
        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotEquals(sessionId, sessionIdOf(events, "session_after_status"));
        assertNull(findEventByName(events, droppedEvent));
    }

    private static JSONArray collectEvents() throws Exception {
        sdk.flush();
        waitForTrackTasks();
        Thread.sleep(200);
        JSONArray all = new JSONArray();
        long deadline = System.currentTimeMillis() + 4_000L;
        while (System.currentTimeMillis() < deadline) {
            okhttp3.mockwebserver.RecordedRequest request =
                    server.takeRequest(400, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (request == null) {
                break;
            }
            JSONArray batch = decodeRequestBody(request);
            for (int i = 0; i < batch.length(); i++) {
                all.put(batch.getJSONObject(i));
            }
            deadline = System.currentTimeMillis() + 400L;
        }
        assertTrue(all.length() > 0);
        return all;
    }

    private static void setDisabledEvents(String... names) throws Exception {
        disabledEvents().clear();
        for (String name : names) {
            disabledEvents().add(name);
        }
    }

    private static void clearDisabledEvents() throws Exception {
        disabledEvents().clear();
    }

    @SuppressWarnings("unchecked")
    private static Set<String> disabledEvents() throws Exception {
        Field field = TDConfig.class.getDeclaredField("mDisabledEvents");
        field.setAccessible(true);
        return (Set<String>) field.get(sdk.mConfig);
    }

    private static String sessionIdOf(JSONArray events, String eventName) throws Exception {
        return findEvent(events, eventName).getJSONObject("properties")
                .getString(TDConstants.KEY_SESSION_ID);
    }

    private static JSONObject findEvent(JSONArray events, String eventName) throws Exception {
        JSONObject event = findEventByName(events, eventName);
        assertNotNull("missing event " + eventName, event);
        return event;
    }

    private static JSONObject findEventByType(JSONArray events, String type) throws Exception {
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            if (type.equals(event.optString("#type"))) {
                return event;
            }
        }
        return null;
    }

    private static long timestampMillis(String uuid) {
        return Long.parseLong(uuid.replace("-", "").substring(0, 12), 16);
    }
}
