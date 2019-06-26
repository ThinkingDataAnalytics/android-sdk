package com.thinking.analyselibrary.demo;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.thinking.analyselibrary.TestRunner;
import com.thinking.analyselibrary.ThinkingAnalyticsSDK;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = ApplicationProvider.getApplicationContext();

        assertEquals("com.thinking.analyselibrary.demo", appContext.getPackageName());
    }

    @Test
    public void testStartEndEvents() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_start");
        JSONObject properties = event.getJSONObject("properties");
        assertFalse(properties.getBoolean("#resume_from_background"));
        assertEquals("com.thinking.analyselibrary.demo.MainActivity", properties.getString("#screen_name"));
        assertEquals(ApplicationProvider.getApplicationContext().getString(R.string.app_name), properties.getString("#title"));
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_view");
        properties = event.getJSONObject("properties");
        assertEquals("com.thinking.analyselibrary.demo.MainActivity", properties.getString("#screen_name"));
        assertEquals(ApplicationProvider.getApplicationContext().getString(R.string.app_name), properties.getString("#title"));
        scenario.moveToState(Lifecycle.State.DESTROYED);
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_end");
        properties = event.getJSONObject("properties");
        assertEquals("com.thinking.analyselibrary.demo.MainActivity", properties.getString("#screen_name"));
        assertEquals(ApplicationProvider.getApplicationContext().getString(R.string.app_name), properties.getString("#title"));
        assertTrue(properties.has("#duration"));
    }

    @Test
    public void testFragment() throws InterruptedException, JSONException {
        TestRunner.getDebugInstance().trackFragmentAppViewScreen();
        ActivityScenario.launch(DisplayActivity.class);
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_view");
        assertEquals(event.getString("#distinct_id"), TestRunner.getDebugInstance().getDistinctId());
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.DisplayActivity|com.thinking.analyselibrary.demo.fragment.RecyclerViewFragment");

        onView(withId(R.id.navigation_expandable)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_view");
        assertEquals(event.getString("#distinct_id"), TestRunner.getDebugInstance().getDistinctId());
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ExpandableListFragment");
        assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.DisplayActivity|com.thinking.analyselibrary.demo.fragment.ExpandableListFragment");

        TestRunner.getInstance().trackFragmentAppViewScreen();
        TestRunner.getInstance().identify("test_fragment_id_instance");
        onView(withId(R.id.navigation_list)).perform(click());
        for (int i = 0; i < 2; i++) {
            event = TestRunner.getEvent();
            assertEquals(event.getString("#event_name"), "ta_app_view");
            properties = event.getJSONObject("properties");
            if (event.getString("#distinct_id").equals(TestRunner.getDebugInstance().getDistinctId())) {
                assertEquals(properties.getString("#title"), "ListViewFragment");
            } else {
                assertEquals(properties.getString("#title"), "DisplayActivity");
            }
            assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.DisplayActivity|com.thinking.analyselibrary.demo.fragment.ListViewFragment");
        }
    }

    @Test
    public void testWebView() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario.launch(WebviewActivity.class);

        onWebView().withElement(findElement(Locator.ID, "test_button")).perform(webClick());
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_pageview");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#type"), "user_set");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#type"), "user_add");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "Purchase");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "Test");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "click");
        assertEquals(event.getString("#distinct_id"), TestRunner.getInstance().getDistinctId());
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("name"), "元素标识名");
        assertEquals(properties.getString("production"), "产品名");

        onView(withId(R.id.fab)).perform(click());
        event = TestRunner.getEvent();
        assertNull(event);
    }

    @Test
    public void testListItemClick() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario.launch(DisplayActivity.class);

        onView(withText("模拟数据 2")).perform(click());
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#element_content"), "模拟数据 2-1002");
        // TODO
        assertEquals(properties.getString("#element_type"), "android.widget.LinearLayout");
        assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.DisplayActivity|com.thinking.analyselibrary.demo.fragment.RecyclerViewFragment");
        assertNotNull(properties.getString("#element_selector"));
        // TODO
        //assertNotNull(properties.getString("#element_id"));

        onView(withId(R.id.navigation_expandable)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#element_content"), "Expandable");
        assertEquals(properties.getString("#element_id"), "navigation_expandable");
        // TODO
        assertEquals(properties.getString("#element_type"), "com.google.android.material.bottomnavigation.BottomNavigationItemView");
        assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.DisplayActivity");
        assertNotNull(properties.getString("#element_selector"));

        onView(withText("西游记")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        // TODO fragment title?
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#element_content"), "西游记");
        assertEquals(properties.getString("#element_id"), "expandable_list");
        assertEquals(properties.getString("#element_type"), "ExpandableListView");
        assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.DisplayActivity|com.thinking.analyselibrary.demo.fragment.ExpandableListFragment");
        assertNotNull(properties.getString("#element_selector"));

        onView(withText("孙悟空")).perform(click());
        event = TestRunner.getEvent();
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#element_content"), "孙悟空");
        assertEquals(properties.getString("#element_position"), "0:1");
        assertEquals(properties.getString("#element_id"), "expandable_list");
        assertEquals(properties.getString("#element_type"), "ExpandableListView");
        assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.DisplayActivity|com.thinking.analyselibrary.demo.fragment.ExpandableListFragment");
        assertNotNull(properties.getString("#element_selector"));
    }

    @Test
    public void testAlertDialog() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        //eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario.launch(MainActivity.class);

        onView(withId(R.id.button_login)).perform(click());
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ThinkingDataDemo");
        assertEquals(properties.getString("#element_content"), "Login");
        assertEquals(properties.getString("#element_type"), "Button");
        assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.MainActivity");
        assertEquals(properties.getString("#element_id"), "button_login");
        assertNotNull(properties.getString("#element_selector"));


        onView(withText(R.string.ok)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ThinkingDataDemo");
        assertEquals(properties.getString("#element_content"), "OK");
        assertEquals(properties.getString("#element_type"), "Button");
        assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.MainActivity");
        assertNotNull(properties.getString("#element_id"));
        assertNotNull(properties.getString("#element_selector"));

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ThinkingDataDemo");
        assertEquals(properties.getString("#element_content"), "OK");
        assertEquals(properties.getString("#element_type"), "Dialog");
        assertEquals(properties.getString("#screen_name"), "com.thinking.analyselibrary.demo.MainActivity");
        assertEquals(properties.getString("#element_id"), "test_id");
        assertFalse(properties.has("#element_selector"));
    }
}
