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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import cn.thinkingdata.analytics.autotrack.ThinkingDataActivityLifecycleCallbacks;
import cn.thinkingdata.analytics.utils.TDConstants;

import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.*;

/**
 * Instrumented tests for {@link TDAnalytics#enableAutoTrack} lifecycle events.
 *
 * <p>Uses {@link android.app.Instrumentation#newActivity} + direct lifecycle callbacks so tests
 * do not depend on starting a visible Activity (which hangs on some library-test setups).
 * Activity construction and callbacks run on the main thread (Activity needs a Looper).
 */
@RunWith(AndroidJUnit4.class)
public class AutoTrackTest extends BaseSdkTest {

    private static final String APP_ID = "at_autotrack_001";

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
        mTestActivity = null;
        prepareBetweenTestsFacade(context, APP_ID, server, sdk, 20);
        TDAnalytics.enableAutoTrack(
                TDAnalytics.TDAutoTrackEventType.APP_START
                        | TDAnalytics.TDAutoTrackEventType.APP_END
                        | TDAnalytics.TDAutoTrackEventType.APP_VIEW_SCREEN);
        waitForTrackTasks();
        drainPendingRequests(server);
        clearDbEvents(context, APP_ID);
        // enableAutoTrack(APP_START) already consumed the cold-start path via trackAppStartDelay;
        // reset lifecycle flags so each test can drive start/end/view deterministically.
        resetLifecycleState(lifecycleCallbacks(sdk));
    }

    @After
    public void tearDown() throws Exception {
        cleanupAfterTest(server);
    }

    @Test
    public void autoTrack_appView_eventUploadedOnResume() throws Exception {
        runLifecycle(new LifecycleScript() {
            @Override
            public void run(Activity activity, ThinkingDataActivityLifecycleCallbacks callbacks) {
                callbacks.onActivityCreated(activity, null);
                callbacks.onActivityStarted(activity);
                callbacks.onActivityResumed(activity);
            }
        });
        Thread.sleep(700);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull("ta_app_view must be uploaded when activity resumes",
                findEventByName(events, TDConstants.APP_VIEW_EVENT_NAME));
    }

    @Test
    public void autoTrack_appStartAndEnd_eventsUploaded() throws Exception {
        runLifecycle(new LifecycleScript() {
            @Override
            public void run(Activity activity, ThinkingDataActivityLifecycleCallbacks callbacks) {
                callbacks.onActivityCreated(activity, null);
                callbacks.onActivityStarted(activity);
                callbacks.onActivityResumed(activity);
            }
        });
        Thread.sleep(800);
        runLifecycle(new LifecycleScript() {
            @Override
            public void run(Activity activity, ThinkingDataActivityLifecycleCallbacks callbacks) {
                callbacks.onActivityPaused(activity);
                callbacks.onActivityStopped(activity);
            }
        });
        Thread.sleep(800);

        JSONArray events = decodeRequestBody(flushAndAssertRequest(sdk, server));
        assertNotNull(findEventByName(events, TDConstants.APP_START_EVENT_NAME));
        assertNotNull(findEventByName(events, TDConstants.APP_END_EVENT_NAME));
    }

    private interface LifecycleScript {
        void run(Activity activity, ThinkingDataActivityLifecycleCallbacks callbacks)
                throws Exception;
    }

    /**
     * Create (or reuse) a test Activity on the main thread and invoke lifecycle callbacks there.
     * {@link Activity} construction requires a prepared Looper; {@code @Test(timeout=…)} would
     * otherwise run on a worker thread and crash with "Can't create handler".
     */
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

    private Activity mTestActivity;

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
                "AutoTrackTest",
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
