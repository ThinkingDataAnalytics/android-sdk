package cn.thinkingdata.android.demo;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import cn.thinkingdata.android.TestRunner;
import cn.thinkingdata.android.ThinkingAnalyticsSDK;

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
public class AutoTrackEventsTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = ApplicationProvider.getApplicationContext();

        assertEquals("cn.thinkingdata.android.demo", appContext.getPackageName());
    }

    @Test
    public void testAnnotationEvent() throws InterruptedException, JSONException {
        TestRunner.getInstance().enableAutoTrack(null);
        TestRunner.getDebugInstance().enableAutoTrack(null);

        ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.button_logout)).perform(click());

        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "log_out");
        assertEquals(event.getString("#distinct_id"), TestRunner.getDebugInstance().getDistinctId());
        JSONObject properties = event.getJSONObject("properties");
        assertEquals(properties.getString("paramString"), "value");
        assertEquals(properties.getInt("paramNumber"), 123);
        assertTrue(properties.getBoolean("paramBoolean"));

        event = TestRunner.getEvent();
        assertNull(event);
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
        assertEquals("cn.thinkingdata.android.demo.MainActivity", properties.getString("#screen_name"));
        assertEquals(ApplicationProvider.getApplicationContext().getString(R.string.app_name), properties.getString("#title"));
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_view");
        properties = event.getJSONObject("properties");
        assertEquals("cn.thinkingdata.android.demo.MainActivity", properties.getString("#screen_name"));
        assertEquals(ApplicationProvider.getApplicationContext().getString(R.string.app_name), properties.getString("#title"));

        scenario.moveToState(Lifecycle.State.DESTROYED);

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_end");
        properties = event.getJSONObject("properties");
        assertEquals("cn.thinkingdata.android.demo.MainActivity", properties.getString("#screen_name"));
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
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.RecyclerViewFragment");

        onView(withId(R.id.navigation_expandable)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_view");
        assertEquals(event.getString("#distinct_id"), TestRunner.getDebugInstance().getDistinctId());
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ExpandableListFragment");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.ExpandableListFragment");

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
            assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.ListViewFragment");
        }
    }

    @Test
    public void testWebView() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario.launch(WebViewActivity.class);

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
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.RecyclerViewFragment");
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
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity");
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
        assertEquals(properties.getString("#element_position"), "0");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.ExpandableListFragment");
        assertNotNull(properties.getString("#element_selector"));

        onView(withText("孙悟空")).perform(click());
        event = TestRunner.getEvent();
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "DisplayActivity");
        assertEquals(properties.getString("#element_content"), "孙悟空");
        assertEquals(properties.getString("#element_position"), "0:1");
        assertEquals(properties.getString("#element_id"), "expandable_list");
        assertEquals(properties.getString("#element_type"), "ExpandableListView");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.DisplayActivity|cn.thinkingdata.android.demo.fragment.ExpandableListFragment");
        assertNotNull(properties.getString("#element_selector"));
    }

    @Test
    public void testAlertDialog() throws InterruptedException, JSONException {
        // TODO Dialog 与 Activity 绑定
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
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.MainActivity");
        assertEquals(properties.getString("#element_id"), "button_login");
        assertNotNull(properties.getString("#element_selector"));


        onView(withText(R.string.ok)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ThinkingDataDemo");
        assertEquals(properties.getString("#element_content"), "OK");
        assertEquals(properties.getString("#element_type"), "Button");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.MainActivity");
        assertNotNull(properties.getString("#element_id"));
        assertNotNull(properties.getString("#element_selector"));

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#title"), "ThinkingDataDemo");
        assertEquals(properties.getString("#element_content"), "OK");
        assertEquals(properties.getString("#element_type"), "Dialog");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.MainActivity");
        assertEquals(properties.getString("#element_id"), "test_id");
        assertFalse(properties.has("#element_selector"));
    }

    @Test
    public void testClickEvents() throws InterruptedException, JSONException {
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK);
        TestRunner.getInstance().enableAutoTrack(eventTypeList);

        ActivityScenario.launch(ClickTestActivity.class);

        onView(withId(R.id.checkbox_button)).perform(click());
        JSONObject event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        JSONObject properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "checkbox_button");
        assertEquals(properties.getString("#element_content"), "MyCheckBox");
        assertEquals(properties.getString("#element_type"), "CheckBox");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        onView(withId(R.id.radio_button2)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "radio_group");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "MyRadioButton2");
        assertEquals(properties.getString("#element_type"), "RadioGroup");
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "radio_button2");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "MyRadioButton2");
        assertEquals(properties.getString("#element_type"), "RadioButton");

        onView(withId(R.id.switch_button)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "switch_button");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "ON");
        assertEquals(properties.getString("#element_type"), "SwitchButton");

        onView(withId(R.id.toggle_button)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "toggle_button");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "ON");
        assertEquals(properties.getString("#element_type"), "ToggleButton");

        onView(withText("Widgets")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "TabHost");
        assertEquals(properties.getString("#element_content"), "tab2");

        onView(withId(R.id.seekBar)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "seekBar");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertTrue(properties.getInt("#element_content") > 0);
        assertEquals(properties.getString("#element_type"), "SeekBar");

        onView(withId(R.id.ratingBar)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "ratingBar");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_content"), "3.0");
        assertEquals(properties.getString("#element_type"), "RatingBar");

        onView(withId(R.id.imageView)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "imageView");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "ImageView");

        onView(withText("Others")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "TabHost");
        assertEquals(properties.getString("#element_content"), "tab3");

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "spinner");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "Spinner");
        assertEquals(properties.getString("#element_position"), "0");
        assertEquals(properties.getString("#element_content"), "Android");

        onView(withId(R.id.button_show_timepicker)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "button_show_timepicker");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "Button");
        assertEquals(properties.getString("#element_content"), "Select Time");

        onView(withText("OK")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "timePicker");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "TimePicker");
        assertTrue(properties.has("#element_content"));

        onView(withId(R.id.button_show_datepicker)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "button_show_datepicker");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "Button");
        assertEquals(properties.getString("#element_content"), "Show Date Picker");

        onView(withText("OK")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "datePicker");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "DatePicker");
        assertTrue(properties.has("#element_content"));

        openContextualActionModeOverflowMenu();
        onView(withText("menu2")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_id"), "menu_item2");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "MenuItem");
        assertEquals(properties.getString("#element_content"), "menu2");

        // TODO remove redundant events
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "ListView");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_content"), "menu2");
        assertEquals(properties.getString("#element_position"), "1");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        onView(withId(R.id.button_multi_choice_dialog)).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "Button");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "button_multi_choice_dialog");
        assertEquals(properties.getString("#element_content"), "Test MultiChoice Dialog");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        // TODO remove redundant events
        onView(withText("java")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "Dialog");
        assertEquals(properties.getString("#element_content"), "java");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "ListView");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_position"), "1");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        onView(withText("OK")).perform(click());
        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#element_type"), "Button");
        assertTrue(properties.has("#element_selector"));
        assertEquals(properties.getString("#element_id"), "button1");
        assertEquals(properties.getString("#element_content"), "OK");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");

        event = TestRunner.getEvent();
        assertEquals(event.getString("#event_name"), "ta_app_click");
        properties = event.getJSONObject("properties");
        assertEquals(properties.getString("#screen_name"), "cn.thinkingdata.android.demo.ClickTestActivity");
        assertEquals(properties.getString("#title"), "ClickTestActivity");
        assertEquals(properties.getString("#element_type"), "Dialog");
        assertEquals(properties.getString("#element_content"), "OK");

    }
}
