package cn.thinkingdata.android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import cn.thinkingdata.android.utils.PropertyUtils;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDUtils;

/**
 * TA单元测试
 * Log打印日志有些bug，运行会报错，可以使用System.out.println打印
 * 单元测试中的 android.jar 本身不包含任何代码
 * build.gradle中 testOptions{ unitTests.returnDefaultValues = true }
 * 这个设置可以让android.jar中报错的方法返回默认值，null、0等
 * 如果确实需要使用一些类，需要单独使用testImplementation单独引入
 */
@RunWith(PowerMockRunner.class)
public class UnitTest {

    private static final String TAG = "TA_TEST.UnitTest ";
    public static String currentMethod = "";
    TestProperties testProperties = new TestProperties();

    @After
    public void afterTest() {
        TestUtils.postToServer(testProperties);
    }

    @Test
    public void Test_20000() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("获取viewGroup中指定view的index");
        testProperties.setStep("step1:mock对象LinearLayout、view1、view2，mock对象LinearLayout的方法getChildCount、getChildAt，调用TDUtils的getChildIndex方法");
        testProperties.setExcept("step1:输出和mock方法模拟输入一致");
        LinearLayout ll = PowerMockito.mock(LinearLayout.class);
        View view1 = PowerMockito.mock(View.class);
        View view2 = PowerMockito.mock(View.class);
        PowerMockito.when(ll.getChildCount()).thenReturn(2);
        PowerMockito.when(ll.getChildAt(0)).thenReturn(view1);
        PowerMockito.when(ll.getChildAt(1)).thenReturn(view2);

        Class<?> tdUtilsClass = Class.forName("cn.thinkingdata.android.utils.TDUtils");
        Method method = tdUtilsClass.getDeclaredMethod("getChildIndex", new Class[]{ViewParent.class, View.class});
        method.setAccessible(true);
        int index = (int) (method.invoke(TDUtils.class, ll, view1));
        Assert.assertEquals(0, index);
        index = (int) (method.invoke(TDUtils.class, ll, view2));
        Assert.assertEquals(1, index);
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 获取viewGroup中指定view的index <-");
    }

    @Test
    public void Test_20001() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("添加view的viewPath到属性中");
        testProperties.setStep("step1:mock LinearLayout、View对象，mock View的getParent方法，调用TDUtils.addViewPathProperties方法，传入view和一个空的JSONObject引用");
        testProperties.setExcept("step1:传入的JSONObject对象包含#element_selector的值");
        LinearLayout ll = PowerMockito.mock(LinearLayout.class);
        View view = PowerMockito.mock(View.class);
        PowerMockito.when(view.getParent()).thenReturn(ll);
        JSONObject jsonObject = new JSONObject();
        TDUtils.addViewPathProperties(null, view, jsonObject);
        Assert.assertTrue(jsonObject.has("#element_selector"));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 添加view的viewPath到属性中 <-");
    }

    @Test
    public void Test_20002() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("获取viewGroup下所有view的text内容");
        testProperties.setStep("step1:mock LinearLayout对象及childView:LinearLayout[TextView]、Button、TextView、ImageView、CheckBox、" +
                "CompoundButton、RadioButton、ToggleButton，mock LinearLayout的getChildCount()和getChildAt()方法给定包含关系，mock view的getText方法，" +
                "调用TDUtils.traverseView传入空的StringBuilder和ll");
        testProperties.setExcept("step1:输出为和各个view mock值拼接的值");
        LinearLayout ll = PowerMockito.mock(LinearLayout.class);
        LinearLayout child_ll = PowerMockito.mock(LinearLayout.class);
        Button button = PowerMockito.mock(Button.class);
        TextView tv = PowerMockito.mock(TextView.class);
        TextView child_tv = PowerMockito.mock(TextView.class);
        ImageView imageView = PowerMockito.mock(ImageView.class);
        CheckBox checkBox = PowerMockito.mock(CheckBox.class);
        CompoundButton compoundButton = PowerMockito.mock(CompoundButton.class);
        RadioButton radioButton = PowerMockito.mock(RadioButton.class);
        ToggleButton toggleButton = PowerMockito.mock(ToggleButton.class);

        PowerMockito.when(ll.getChildCount()).thenReturn(8);
        PowerMockito.when(child_ll.getChildCount()).thenReturn(1);
        PowerMockito.when(child_ll.getChildAt(0)).thenReturn(child_tv);
        PowerMockito.when(ll.getChildAt(0)).thenReturn(child_ll);
        PowerMockito.when(ll.getChildAt(1)).thenReturn(tv);
        PowerMockito.when(ll.getChildAt(2)).thenReturn(button);
        PowerMockito.when(ll.getChildAt(3)).thenReturn(imageView);
        PowerMockito.when(ll.getChildAt(4)).thenReturn(checkBox);
        PowerMockito.when(ll.getChildAt(5)).thenReturn(compoundButton);
        PowerMockito.when(ll.getChildAt(6)).thenReturn(radioButton);
        PowerMockito.when(ll.getChildAt(7)).thenReturn(toggleButton);

        PowerMockito.when(checkBox.getText()).thenReturn("checkBox");
        PowerMockito.when(compoundButton.getText()).thenReturn("compoundButton off");
        PowerMockito.when(tv.getText()).thenReturn("tv");
        PowerMockito.when(child_tv.getText()).thenReturn("child_tv");
        PowerMockito.when(button.getText()).thenReturn("button");
        PowerMockito.when(toggleButton.getTextOff()).thenReturn("toggleButton off");
        PowerMockito.when(radioButton.getText()).thenReturn("radioButton");
        PowerMockito.when(imageView.getContentDescription()).thenReturn("imageView");

        Assert.assertEquals("child_tv-tv-button-imageView-checkBox-compoundButton off-radioButton-toggleButton off-", TDUtils.traverseView(new StringBuilder(), ll));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 获取viewGroup下所有view的text内容 <-");
    }


    @Test
    public void Test_20003() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("获取Fragment的title");
        testProperties.setStep("step1:mock View对象，mock View的getTag方法，调用TDUtils.getFragmentNameFromView方法传入view和空的JSONObject");
        testProperties.setExcept("step1:JSONObject中包含#screen_name 为 mock的值");
        View view = PowerMockito.mock(View.class);
        PowerMockito.when(view.getTag(R.id.thinking_analytics_tag_view_fragment_name)).thenReturn("view Name");
        JSONObject jsonObject = new JSONObject();
        TDUtils.getFragmentNameFromView(view, jsonObject);
        Assert.assertEquals("|view Name", jsonObject.optString(TDConstants.SCREEN_NAME));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 获取Fragment的title <-");
    }

    @Test
    public void Test_20004() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("根据Context获取Activity");
        testProperties.setStep("step1:mock Activity、ContextWrapper对象，mock Activity的getApplicationContext方法、ContextWrapper的getBaseContext方法，调用TDUtils.getActivityFromContext方法分别传入activity和activity.getApplicationContext()");
        testProperties.setExcept("step1:两次返回都为mock的Activity");
        Activity activity = PowerMockito.mock(Activity.class);
        ContextWrapper contextWrapper = PowerMockito.mock(ContextWrapper.class);
        PowerMockito.when(activity.getApplicationContext()).thenReturn(contextWrapper);
        PowerMockito.when(contextWrapper.getBaseContext()).thenReturn(activity);
        Assert.assertEquals(activity, TDUtils.getActivityFromContext(activity));
        Assert.assertEquals(activity, TDUtils.getActivityFromContext(activity.getApplicationContext()));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 根据Context获取Activity <-");
    }

    @Test
    public void Test_20005() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("获取Activity的title");
        testProperties.setStep("step1:mock Activity对象，mock getTitle方法，调用TDUtils.getActivityTitle方法");
        testProperties.setExcept("step1:返回值和mock方法模拟title一致");
        Activity activity = PowerMockito.mock(Activity.class);
        PowerMockito.when(activity.getTitle()).thenReturn("test Title");
        String title = TDUtils.getActivityTitle(activity);
        Assert.assertEquals("test Title", title);
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 获取Activity的title <-");
    }

    @Test
    public void Test_20006() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("根据Activity获取当前屏幕Name和Title");
        testProperties.setStep("step1:mock Activity对象，mock getTitle方法，调用TDUtils.getScreenNameAndTitleFromActivity方法传入JSONObject和Activity对象");
        testProperties.setExcept("step1:JSONObject中包含#screen_name和#title，并且值和mock模拟输入一致");

        Activity activity = PowerMockito.mock(Activity.class);
        PowerMockito.when(activity.getTitle()).thenReturn("title");
        JSONObject jsonObject = new JSONObject();
        TDUtils.getScreenNameAndTitleFromActivity(jsonObject, activity);
        Assert.assertEquals("android.app.Activity", jsonObject.optString(TDConstants.SCREEN_NAME));
        Assert.assertEquals("title", jsonObject.optString(TDConstants.TITLE));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 根据Activity获取当前屏幕Name和Title <-");
    }

    @Test
    public void Test_20007() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("获取Activity的toolbarTitle");
        testProperties.setStep("step1:mock Activity、ActionBar对象和AppCompatActivity、androidx.appcompat.app.ActionBar对象，mock 对应的actionbar的getTitle方法，调用TDUtils.getToolbarTitle方法");
        testProperties.setExcept("step1:返回和mock方法模拟的title一致");
        Activity activity = PowerMockito.mock(Activity.class);
        ActionBar actionBar = PowerMockito.mock(ActionBar.class);
        AppCompatActivity appCompatActivity = PowerMockito.mock(AppCompatActivity.class);
        androidx.appcompat.app.ActionBar actionBarX = PowerMockito.mock(androidx.appcompat.app.ActionBar.class);
        PowerMockito.when(activity.getActionBar()).thenReturn(actionBar);
        PowerMockito.when(actionBar.getTitle()).thenReturn("actionBar");
        PowerMockito.when(appCompatActivity.getSupportActionBar()).thenReturn(actionBarX);
        PowerMockito.when(actionBarX.getTitle()).thenReturn("actionBarX");
        Assert.assertEquals("actionBar", TDUtils.getToolbarTitle(activity));
        Assert.assertEquals("actionBarX", TDUtils.getToolbarTitle(appCompatActivity));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 获取Activity的toolbarTitle <-");
    }

    @Test
    public void Test_20008() throws JSONException {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("合并JSONObject对象[处理时区]");
        testProperties.setStep("step1:调用mergeJSONObject方法传入key1:value1；key2:value2的JOSNObject和key2:value22；key3:new Date()的JSONObject");
        testProperties.setExcept("step1:合并后的JSONObject应该为key1:value1；key2:value22:key3:转换格式后的Date字符串");
        JSONObject oldJSONObject = new JSONObject();
        oldJSONObject.put("key1", "value1");
        oldJSONObject.put("key2", "value2");
        JSONObject newJSONObject = new JSONObject();
        newJSONObject.put("key2", "value22");
        Date date = new Date();
        newJSONObject.put("key3", date);
        TDUtils.mergeJSONObject(newJSONObject, oldJSONObject, TimeZone.getTimeZone("Asia/Shanghai"));
        Assert.assertEquals("value1", oldJSONObject.optString("key1"));
        Assert.assertEquals("value22", oldJSONObject.optString("key2"));
        Assert.assertEquals(new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).format(date), oldJSONObject.optString("key3"));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 合并JSONObject对象[处理时区] <-");
    }

    @Test
    public void Test_20009() throws JSONException {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("合并包含JSONObject对象的JSONObejct[处理时区]");
        testProperties.setStep("step1:分别创建两个JSONObject，各自包含一个key为key1的JSONObject对象；oldJSONObjectChild:{\"childKey1\": \"childValue1\",\"childKey2\":\"childValue2\",\"childKey3\":\"childValue3\"}" +
                "newJSONObjectChild:{\"childKey1\":\"childValue111\",\"childKey2\": new Date(),\"childKey4\":\"childValue4\"};调用mergeNestedJSONObject传入两个父JSONObject对象和时区");

        testProperties.setExcept("step1:合并后的JSONObject应该为{\"key1\":\"{\"childKey1\":\"childValue111\",\"childKey3\":\"childValue3\",\"childKey4\":\"childValue4\",\"childKey2\":根据时区转换后的时间字符串}\"}");
        JSONObject oldJSONObject = new JSONObject();
        JSONObject oldJSONObjectChild = new JSONObject();
        oldJSONObjectChild.put("childKey1", "childValue1");
        oldJSONObjectChild.put("childKey2", "childValue2");
        oldJSONObjectChild.put("childKey3", "childValue3");
        oldJSONObject.put("key1", oldJSONObjectChild);

        Date date = new Date();
        JSONObject newJSONObject = new JSONObject();
        JSONObject newJSONObjectChild = new JSONObject();
        newJSONObjectChild.put("childKey1", "childValue111");
        newJSONObjectChild.put("childKey2", date);
        newJSONObjectChild.put("childKey4", "childValue4");
        newJSONObject.put("key1", newJSONObjectChild);

        TDUtils.mergeNestedJSONObject(newJSONObject, oldJSONObject, TimeZone.getTimeZone("Asia/Shanghai"));
        JSONObject child = oldJSONObject.optJSONObject("key1");
        Assert.assertEquals("childValue111", child.optString("childKey1"));
        Assert.assertEquals("childValue3", child.optString("childKey3"));
        Assert.assertEquals("childValue4", child.optString("childKey4"));
        Assert.assertEquals(new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).format(date), child.optString("childKey2"));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 合并包含JSONObject对象的JSONObejct[处理时区] <-");
    }

    @Test
    public void Test_200010() throws JSONException {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("根据时区转换JSONArray");
        testProperties.setStep("step1:调用formatJSONArray方法传入JSONArray:[\"value1\",[时间],{\"key1\":\"value1\",\"key2\":时间},时间]");
        testProperties.setExcept("step1:返回结果：[\"value1\",[根据时区转换后的时间字符串],{\"key1\":\"value1\",\"key2\":根据时区转换后的时间字符串},根据时区转换后的时间字符串]");
        JSONArray jsonArray = new JSONArray();
        JSONArray childJsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        Date date = new Date();
        jsonArray.put("value1");
        childJsonArray.put(date);
        jsonArray.put(childJsonArray);
        jsonObject.put("key1", "value1");
        jsonObject.put("key2", date);
        jsonArray.put(jsonObject);
        jsonArray.put(date);
        JSONArray result = TDUtils.formatJSONArray(jsonArray, TimeZone.getTimeZone("Asia/Shanghai"));
        Assert.assertEquals("value1", result.get(0));
        Assert.assertEquals(new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).format(date), ((JSONArray) result.get(1)).get(0));
        Assert.assertEquals("value1", ((JSONObject) result.get(2)).optString("key1"));
        Assert.assertEquals(new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).format(date), ((JSONObject) result.get(2)).optString("key2"));
        Assert.assertEquals(new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).format(date), result.get(3));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 根据时区转换JSONArray <-");
    }

    @Test
    public void Test_20011() throws JSONException {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("根据时区转换JSONObject");
        testProperties.setStep("step1:调用formatJSONObject方法传入JSONObject：{\"key1\":\"value1\",\"key2\":[时间],\"key3\":{\"key1\":\"value1\",\"key2\":时间},\"key4\":时间}和时区");
        testProperties.setExcept("step1:返回结果：{\"key1\":\"value1\",\"key2\":[根据时区格式化后的时间字符串],\"key3\":{\"key1\":\"value1\",\"key2\":根据时区格式化后的时间字符串},\"key4\":根据时区格式化后的时间字符串}");
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONObject childJsonObject = new JSONObject();
        Date date = new Date();
        jsonObject.put("key1", "value1");
        jsonArray.put(date);
        jsonObject.put("key2", jsonArray);
        childJsonObject.put("key1", "value1");
        childJsonObject.put("key2", date);
        jsonObject.put("key3", childJsonObject);
        jsonObject.put("key4", date);
        JSONObject result = TDUtils.formatJSONObject(jsonObject, TimeZone.getTimeZone("Asia/Shanghai"));
        Assert.assertEquals("value1", result.optString("key1"));
        Assert.assertEquals(new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).format(date), ((JSONArray) result.optJSONArray("key2")).get(0));
        Assert.assertEquals("value1", ((JSONObject) result.optJSONObject("key3")).optString("key1"));
        Assert.assertEquals(new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).format(date), ((JSONObject) result.optJSONObject("key3")).optString("key2"));
        Assert.assertEquals(new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA).format(date), result.optString("key4"));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 根据时区转换JSONObject <-");
    }

    @Test
    public void Test_20012() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("根据时区和时间获取当前时区偏移");
        testProperties.setStep("step1:调用getTimezoneOffset方法传入当前时间，以及不同的Timezone：Asia/Shanghai、GMT+8:00、America/New_York、GMT-5:00");
        testProperties.setExcept("step1:返回结果：8.0、8.0、-5.0、-5.0");
        Assert.assertEquals(8.0, TDUtils.getTimezoneOffset(new Date().getTime(), TimeZone.getTimeZone("Asia/Shanghai")), 0);
        Assert.assertEquals(8.0, TDUtils.getTimezoneOffset(new Date().getTime(), TimeZone.getTimeZone("GMT+8:00")), 0);
        Assert.assertEquals(-5.0, TDUtils.getTimezoneOffset(new Date().getTime(), TimeZone.getTimeZone("America/New_York")), 0);
        Assert.assertEquals(-5.0, TDUtils.getTimezoneOffset(new Date().getTime(), TimeZone.getTimeZone("GMT-5:00")), 0);
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 根据时区和时间获取当前时区偏移 <-");
    }

    @Test
    public void Test_20013() throws IllegalAccessException {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("获取主进程name");
        testProperties.setStep("step1:mock Context 和Application对象，mock ApplicationInfo的processName属性 ，调用getMainProcessName方法");
        testProperties.setExcept("step1:返回结果为mock的processName");
        Context context = PowerMockito.mock(Context.class);
        ApplicationInfo applicationInfo = PowerMockito.mock(ApplicationInfo.class);
        PowerMockito.field(ApplicationInfo.class, "processName").set(applicationInfo, "testMainProcessName");
        PowerMockito.when(context.getApplicationInfo()).thenReturn(applicationInfo);
        Assert.assertEquals("testMainProcessName", TDUtils.getMainProcessName(context));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 获取主进程name <-");
    }

    @Test
    public void Test_20014() throws IllegalAccessException {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("获取当前进程name");
        testProperties.setStep("step1:mock Context、ActivityManager、ActivityManager.RunningAppProcessInfo、android.os.Process对象以及Context的getSystemService方法、RunningAppProcessInfo的pid和processName属性、Process类的静态方法myPid，调用getCurrentProcessName方法传入mock对象context");
        testProperties.setExcept("step1:返回结果和mock的processName值一致");

        Context context = PowerMockito.mock(Context.class);
        ActivityManager activityManager = PowerMockito.mock(ActivityManager.class);
        PowerMockito.when(context.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager);
        ActivityManager.RunningAppProcessInfo runningAppProcessInfo = PowerMockito.mock(ActivityManager.RunningAppProcessInfo.class);
        List<ActivityManager.RunningAppProcessInfo> list = new ArrayList<>();
        list.add(runningAppProcessInfo);
        PowerMockito.when(activityManager.getRunningAppProcesses()).thenReturn(list);
        PowerMockito.field(ActivityManager.RunningAppProcessInfo.class, "pid").set(runningAppProcessInfo, 12138);
        PowerMockito.field(ActivityManager.RunningAppProcessInfo.class, "processName").set(runningAppProcessInfo, "testProcessName");
        try (MockedStatic<Process> processMock = Mockito.mockStatic(Process.class)) {
            processMock.when(Process::myPid).thenReturn(12138);
            Assert.assertEquals("testProcessName", TDUtils.getCurrentProcessName(context));
        }

        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 获取当前进程name <-");
    }

    @Test
    public void Test_20015() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("获取osName");
        testProperties.setStep("step1:mock TextUtils对象，mock isEmpty方法分别返回true和false，调用TDUtils.osName方法");
        testProperties.setExcept("step1:返回值和预期一致");
        try (MockedStatic<TextUtils> theMock = Mockito.mockStatic(TextUtils.class)) {
            theMock.when(() -> TextUtils.isEmpty(null)).thenReturn(true);
            Assert.assertEquals("Android", TDUtils.osName(null));
            theMock.when(() -> TextUtils.isEmpty(null)).thenReturn(false);
            Assert.assertEquals("HarmonyOS", TDUtils.osName(null));
        }
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 获取osName <-");
    }

    @Test
    public void Test_20016() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("成指定位数的随机16进制数");
        testProperties.setStep("step1:调用getRandomHEXValue方法分别传入10和16生成16进制字符串");
        testProperties.setExcept("step1:生成的字符串为16进制字符组成，并且位数和输入一致");
        String s1 = TDUtils.getRandomHEXValue(10);
        String s2 = TDUtils.getRandomHEXValue(16);
        Assert.assertEquals(10, s1.length());
        Assert.assertEquals(16, s2.length());
        String ss = "0123456789abcdef";
        for (int i = 0; i < 10; i++) {
            Assert.assertTrue(ss.contains(String.valueOf(s1.charAt(i))));
        }
        for (int i = 0; i < 16; i++) {
            Assert.assertTrue(ss.contains(String.valueOf(s2.charAt(i))));
        }
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 成指定位数的随机16进制数 <-");
    }

    @Test
    public void Test_21000() {
        currentMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
        System.out.println(TAG + currentMethod + " ->");
        testProperties.setResult(false);
        testProperties.setId(currentMethod);
        testProperties.setName("检查eventName是否非法");
        testProperties.setStep("step1:调用PropertyUtils.isInvalidName传入a_;" +
                "step2:调用PropertyUtils.isInvalidName传入a?;" +
                "step3:调用PropertyUtils.isInvalidName传入a!;" +
                "step4:调用PropertyUtils.isInvalidName传入a@;" +
                "step5:调用PropertyUtils.isInvalidName传入a&;" +
                "step6:调用PropertyUtils.isInvalidName传入a%;" +
                "step7:调用PropertyUtils.isInvalidName传入a^;" +
                "step8:调用PropertyUtils.isInvalidName传入a*;" +
                "step9:调用PropertyUtils.isInvalidName传入a(;" +
                "step10:调用PropertyUtils.isInvalidName传入a);" +
                "step11:调用PropertyUtils.isInvalidName传入a+;" +
                "step12:调用PropertyUtils.isInvalidName传入a#;" +
                "step13:调用PropertyUtils.isInvalidName传入abc;" +
                "step14:调用PropertyUtils.isInvalidName传入123;" +
                "step15:调用PropertyUtils.isInvalidName传入空串;" +
                "step16:调用PropertyUtils.isInvalidName传入_abc;" +
                "step17:调用PropertyUtils.isInvalidName传入50位正常字符;" +
                "step18:调用PropertyUtils.isInvalidName传入_51位正常字符;"
        );
        testProperties.setExcept("step1:false" +
                "step2:true" +
                "step3:true" +
                "step4:true" +
                "step5:true" +
                "step6:true" +
                "step7:true" +
                "step8:true" +
                "step9:true" +
                "step10:true" +
                "step11:true" +
                "step12:true" +
                "step13:false" +
                "step14:true" +
                "step15:true" +
                "step16:true" +
                "step17:false" +
                "step18:true"
        );
        Assert.assertFalse(PropertyUtils.isInvalidName("a_"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a?"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a!"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a@"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a&"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a%"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a^"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a*"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a("));
        Assert.assertTrue(PropertyUtils.isInvalidName("a)"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a+"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a#"));
        Assert.assertTrue(PropertyUtils.isInvalidName("a$"));
        Assert.assertFalse(PropertyUtils.isInvalidName("abc"));
        Assert.assertTrue(PropertyUtils.isInvalidName("123"));
        Assert.assertTrue(PropertyUtils.isInvalidName(""));
        Assert.assertTrue(PropertyUtils.isInvalidName("_abc"));
        //50位
        String s1 = "abddbakjdabdacnakchoaihowndalndacnakchoaihowndalnf";
        //51位
        String s2 = "abddbakjdabdacnakchoaihowndalndacnakchoaihowndalnfa";
        Assert.assertFalse(PropertyUtils.isInvalidName(s1));
        Assert.assertTrue(PropertyUtils.isInvalidName(s2));
        testProperties.setResult(true);
        System.out.println(TAG + currentMethod + " -> 检查eventName是否非法 <-");
    }

}
