/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import cn.thinkingdata.analytics.autotrack.ThinkingDataActivityLifecycleCallbacks;
import cn.thinkingdata.analytics.utils.TDConstants;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Tests for {@link TDAnalytics#enableAutoTrack(int, JSONObject)} and
 * {@link TDAnalytics#enableAutoTrack(int, TDAnalytics.TDAutoTrackEventHandler)}.
 */
@RunWith(AndroidJUnit4.class)
public class AutoTrackExtrasTest extends BaseSdkTest {

    private static final String APP_ID = "at_autotrack_extras_001";
    private static final int LIFECYCLE_TYPES =
            TDAnalytics.TDAutoTrackEventType.APP_START
                    | TDAnalytics.TDAutoTrackEventType.APP_END
                    | TDAnalytics.TDAutoTrackEventType.APP_VIEW_SCREEN;

    private static Context context;
    private static MockWebServer server;
    private static ThinkingAnalyticsSDK sdk;

    private Activity mTestActivity;

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
        mTestActivity = null;
        prepareBetweenTestsFacade(context, APP_ID, server, sdk, 20);
        clearAutoTrackListener();
        clearAutoTrackStaticProperties();
        waitForTrackTasks();
        drainPendingRequests(server);
        clearDbEvents(context, APP_ID);
        resetLifecycleState(lifecycleCallbacks(sdk));
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    @Test
    public void enableAutoTrack_withProperties_mergedIntoUploadedEvents() throws Exception {
        JSONObject autoProps = new JSONObject();
        autoProps.put("static_auto_key", "static_auto_val");
        autoProps.put("static_auto_num", 42);

        TDAnalytics.enableAutoTrack(LIFECYCLE_TYPES, autoProps);
        Thread.sleep(700);
        drainPendingRequests(server);
        clearDbEvents(context, APP_ID);
        resetLifecycleState(lifecycleCallbacks(sdk));

        driveStartAndResume();
        Thread.sleep(700);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject start = findEventByName(events, TDConstants.APP_START_EVENT_NAME);
        JSONObject view = findEventByName(events, TDConstants.APP_VIEW_EVENT_NAME);
        assertNotNull(start);
        assertNotNull(view);

        assertEquals("static_auto_val",
                start.getJSONObject("properties").getString("static_auto_key"));
        assertEquals(42, start.getJSONObject("properties").getInt("static_auto_num"));
        assertEquals("static_auto_val",
                view.getJSONObject("properties").getString("static_auto_key"));
        assertEquals(42, view.getJSONObject("properties").getInt("static_auto_num"));
    }

    @Test
    public void enableAutoTrack_withEventHandler_mergedIntoUploadedEvents() throws Exception {
        final AtomicBoolean handlerInvoked = new AtomicBoolean(false);
        final AtomicInteger lastEventType = new AtomicInteger(0);

        TDAnalytics.enableAutoTrack(LIFECYCLE_TYPES, new TDAnalytics.TDAutoTrackEventHandler() {
            @Override
            public JSONObject getAutoTrackEventProperties(int eventType, JSONObject properties) {
                handlerInvoked.set(true);
                lastEventType.set(eventType);
                try {
                    JSONObject extra = new JSONObject();
                    extra.put("handler_prop", "from_handler");
                    extra.put("handler_event_type", eventType);
                    return extra;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread.sleep(700);
        drainPendingRequests(server);
        clearDbEvents(context, APP_ID);
        resetLifecycleState(lifecycleCallbacks(sdk));

        driveStartAndResume();
        Thread.sleep(700);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        JSONObject start = findEventByName(events, TDConstants.APP_START_EVENT_NAME);
        JSONObject view = findEventByName(events, TDConstants.APP_VIEW_EVENT_NAME);
        assertNotNull(start);
        assertNotNull(view);

        assertTrue("TDAutoTrackEventHandler must be invoked", handlerInvoked.get());
        assertEquals("from_handler",
                start.getJSONObject("properties").getString("handler_prop"));
        assertEquals("from_handler",
                view.getJSONObject("properties").getString("handler_prop"));
        assertTrue("handler should receive a non-zero auto-track type",
                lastEventType.get() != 0);
    }

    private void driveStartAndResume() throws Exception {
        runLifecycle(new LifecycleScript() {
            @Override
            public void run(Activity activity, ThinkingDataActivityLifecycleCallbacks callbacks) {
                callbacks.onActivityCreated(activity, null);
                callbacks.onActivityStarted(activity);
                callbacks.onActivityResumed(activity);
            }
        });
    }

    private interface LifecycleScript {
        void run(Activity activity, ThinkingDataActivityLifecycleCallbacks callbacks)
                throws Exception;
    }

    private void runLifecycle(final LifecycleScript script) throws Exception {
        final AtomicReference<Exception> error = new AtomicReference<>();
        final ThinkingDataActivityLifecycleCallbacks callbacks = lifecycleCallbacks(sdk);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    Activity activity = obtainTestActivity();
                    script.run(activity, callbacks);
                } catch (Exception e) {
                    error.set(e);
                }
            }
        });
        if (error.get() != null) {
            throw error.get();
        }
    }

    private Activity obtainTestActivity() throws Exception {
        if (mTestActivity != null) {
            return mTestActivity;
        }
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Application app = (Application) target.getApplicationContext();
        Intent intent = new Intent(target, AutoTrackTestActivity.class);
        ActivityInfo info = new ActivityInfo();
        info.packageName = target.getPackageName();
        info.name = AutoTrackTestActivity.class.getName();
        mTestActivity = InstrumentationRegistry.getInstrumentation().newActivity(
                AutoTrackTestActivity.class,
                target,
                (IBinder) null,
                app,
                intent,
                info,
                "AutoTrackExtrasTest",
                null,
                null,
                null);
        return mTestActivity;
    }

    private static ThinkingDataActivityLifecycleCallbacks lifecycleCallbacks(
            ThinkingAnalyticsSDK instance) throws Exception {
        Field field = ThinkingAnalyticsSDK.class.getDeclaredField("mLifecycleCallbacks");
        field.setAccessible(true);
        return (ThinkingDataActivityLifecycleCallbacks) field.get(instance);
    }

    private static void resetLifecycleState(ThinkingDataActivityLifecycleCallbacks callbacks)
            throws Exception {
        setField(callbacks, "isLaunch", Boolean.TRUE);
        setField(callbacks, "resumeFromBackground", Boolean.FALSE);
        setField(callbacks, "shouldTrackEndEvent", Boolean.FALSE);
        setField(callbacks, "startTimer", null);
        Object startedList = getField(callbacks, "mStartedActivityList");
        if (startedList instanceof java.util.List) {
            ((java.util.List<?>) startedList).clear();
        }
    }

    private static void clearAutoTrackListener() throws Exception {
        Field field = ThinkingAnalyticsSDK.class.getDeclaredField("mAutoTrackEventListener");
        field.setAccessible(true);
        field.set(sdk, null);
    }

    private static void clearAutoTrackStaticProperties() throws Exception {
        Field field = ThinkingAnalyticsSDK.class.getDeclaredField("mAutoTrackEventProperties");
        field.setAccessible(true);
        field.set(sdk, new JSONObject());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = ThinkingDataActivityLifecycleCallbacks.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = ThinkingDataActivityLifecycleCallbacks.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
