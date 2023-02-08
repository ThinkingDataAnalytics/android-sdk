/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.utils;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import cn.thinkingdata.android.PathFinder;
import cn.thinkingdata.android.R;
import cn.thinkingdata.android.ScreenAutoTracker;
import cn.thinkingdata.android.TDConfig;
import cn.thinkingdata.android.TDContextConfig;
import cn.thinkingdata.android.TDPresetProperties;
import cn.thinkingdata.android.ThinkingDataFragmentTitle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * TA工具类.
 * */
public class TDUtils {
    static long firstVsync;
    static long secondVsync;
    static volatile int fps;
    static final Object frameLock = new Object();
    public static final String COMMAND_HARMONY_OS_VERSION = "getprop hw_sc.build.platform.version";

    private static int getChildIndex(ViewParent parent, View child) {
        try {
            if (!(parent instanceof ViewGroup)) {
                return -1;
            }

            ViewGroup viewGroup = (ViewGroup) parent;
            final String childIdName = TDUtils.getViewId(child);

            String childClassName = child.getClass().getCanonicalName();
            int index = 0;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View brother = viewGroup.getChildAt(i);

                if (!PathFinder.hasClassName(brother, childClassName)) {
                    continue;
                }

                String brotherIdName = TDUtils.getViewId(brother);

                if (null != childIdName && !childIdName.equals(brotherIdName)) {
                    index++;
                    continue;
                }

                if (brother == child) {
                    return index;
                }

                index++;
            }

            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * < addViewPathProperties >.
     *
     * @param activity Activity
     * @param view View
     * @param properties 事件属性
     */
    public static void addViewPathProperties(Activity activity, View view, JSONObject properties) {
        try {
            if (view == null) {
                return;
            }

            if (properties == null) {
                properties = new JSONObject();
            }

            ViewParent viewParent;
            List<String> viewPath = new ArrayList<>();
            do {
                viewParent = view.getParent();
                int index = getChildIndex(viewParent, view);
                viewPath.add(view.getClass().getCanonicalName() + "[" + index + "]");
                if (viewParent instanceof ViewGroup) {
                    view = (ViewGroup) viewParent;
                }

            } while (viewParent instanceof ViewGroup);

            Collections.reverse(viewPath);
            StringBuilder stringBuffer = new StringBuilder();
            for (int i = 1; i < viewPath.size(); i++) {
                stringBuffer.append(viewPath.get(i));
                if (i != (viewPath.size() - 1)) {
                    stringBuffer.append("/");
                }
            }
            if (!TDPresetProperties.disableList.contains(TDConstants.ELEMENT_SELECTOR)) {
                properties.put(TDConstants.ELEMENT_SELECTOR, stringBuffer.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * < traverseView >.
     *
     * @param stringBuilder StringBuilder
     * @param root ViewGroup
     * @return {@link String}
     */
    public static String traverseView(StringBuilder stringBuilder, ViewGroup root) {
        try {
            if (root == null) {
                return stringBuilder.toString();
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);

                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }

                if (child instanceof ViewGroup) {
                    traverseView(stringBuilder, (ViewGroup) child);
                } else {
                    Class<?> switchCompatClass = null;
                    try {
                        switchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
                    } catch (Exception e) {
                        //ignored
                    }

                    if (switchCompatClass == null) {
                        try {
                            switchCompatClass = Class.forName("androidx.appcompat.widget.SwitchCompat");
                        } catch (Exception e) {
                            //ignored
                        }
                    }

                    CharSequence viewText = null;
                    if (child instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) child;
                        viewText = checkBox.getText();
                    } else if (switchCompatClass != null && switchCompatClass.isInstance(child)) {
                        CompoundButton switchCompat = (CompoundButton) child;
                        Method method;
                        if (switchCompat.isChecked()) {
                            method = child.getClass().getMethod("getTextOn");
                        } else {
                            method = child.getClass().getMethod("getTextOff");
                        }
                        viewText = (String) method.invoke(child);
                    } else if (child instanceof RadioButton) {
                        RadioButton radioButton = (RadioButton) child;
                        viewText = radioButton.getText();
                    } else if (child instanceof ToggleButton) {
                        ToggleButton toggleButton = (ToggleButton) child;
                        boolean isChecked = toggleButton.isChecked();
                        if (isChecked) {
                            viewText = toggleButton.getTextOn();
                        } else {
                            viewText = toggleButton.getTextOff();
                        }
                    } else if (child instanceof Button) {
                        Button button = (Button) child;
                        viewText = button.getText();
                    } else if (child instanceof CheckedTextView) {
                        CheckedTextView textView = (CheckedTextView) child;
                        viewText = textView.getText();
                    } else if (child instanceof TextView) {
                        TextView textView = (TextView) child;
                        viewText = textView.getText();
                    } else if (child instanceof ImageView) {
                        ImageView imageView = (ImageView) child;
                        if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                            viewText = imageView.getContentDescription().toString();
                        }
                    }

                    if (!TextUtils.isEmpty(viewText)) {
                        stringBuilder.append(viewText.toString());
                        stringBuilder.append("-");
                    }
                }
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return stringBuilder.toString();
        }
    }

    /**
     * < getFragmentNameFromView >.
     *
     * @param view View
     * @param properties JSONObject
     */
    public static void getFragmentNameFromView(View view, JSONObject properties) {
        try {
            if (view != null) {
                String fragmentName = (String) view.getTag(R.id.thinking_analytics_tag_view_fragment_name);
                if (TextUtils.isEmpty(fragmentName) && null != view.getParent() && view.getParent() instanceof View) {
                    fragmentName = (String) ((View) view.getParent()).getTag(R.id.thinking_analytics_tag_view_fragment_name);
                }

                if (!TextUtils.isEmpty(fragmentName)) {
                    String screenName = properties.optString(TDConstants.SCREEN_NAME);
                    if (!TextUtils.isEmpty(fragmentName)) {
                        if (!TDPresetProperties.disableList.contains(TDConstants.SCREEN_NAME)) {
                            properties.put(TDConstants.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", screenName, fragmentName));
                        }
                    } else {
                        if (!TDPresetProperties.disableList.contains(TDConstants.SCREEN_NAME)) {
                            properties.put(TDConstants.SCREEN_NAME, fragmentName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * < 获取fragmentTitle >.
     *
     * @param fragment Fragment
     * @param token 项目ID
     * @return {@link String}
     */
    public static String getTitleFromFragment(final Object fragment, final String token) {
        String title = null;
        try {
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                JSONObject trackProperties = screenAutoTracker.getTrackProperties();
                if (trackProperties != null) {
                    if (trackProperties.has(TDConstants.TITLE)) {
                        title = trackProperties.optString(TDConstants.TITLE);
                    }
                }
            }

            if (TextUtils.isEmpty(title) && fragment.getClass().isAnnotationPresent(ThinkingDataFragmentTitle.class)) {
                ThinkingDataFragmentTitle thinkingDataFragmentTitle = fragment.getClass().getAnnotation(ThinkingDataFragmentTitle.class);
                if (thinkingDataFragmentTitle != null) {
                    if (TextUtils.isEmpty(thinkingDataFragmentTitle.appId())
                            || token.equals(thinkingDataFragmentTitle.appId())) {
                        title = thinkingDataFragmentTitle.title();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return title;
    }

    /**
     * < 通过Context获取 Activity>.
     *
     * @param context Context
     * @return {@link Activity}
     */
    public static Activity getActivityFromContext(Context context) {
        Activity activity = null;
        try {
            if (context != null) {
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (!(context instanceof Activity) && context instanceof ContextWrapper) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activity;
    }

    public static String getViewId(View view) {
        return getViewId(view, null);
    }

    /**
     * < getViewId >.
     *
     * @param view View
     * @param token 项目ID
     * @return {@link String}
     */
    public static String getViewId(View view, String token) {
        String idString = null;
        try {
            //idString = (String) view.getTag(R.id.thinking_analytics_tag_view_id);
            idString = (String) getTag(token, view, R.id.thinking_analytics_tag_view_id);
            if (TextUtils.isEmpty(idString)) {
                if (view.getId() != View.NO_ID) {
                    idString = view.getContext().getResources().getResourceEntryName(view.getId());
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return idString;
    }

    /**
     * < getActivityTitle >.
     *
     * @param activity Activity
     * @return {@link String}
     */
    public static String getActivityTitle(Activity activity) {
        try {
            if (activity != null) {
                try {
                    String activityTitle = null;
                    if (!TextUtils.isEmpty(activity.getTitle())) {
                        activityTitle = activity.getTitle().toString();
                    }

                    if (Build.VERSION.SDK_INT >= 11) {
                        String toolbarTitle = getToolbarTitle(activity);
                        if (!TextUtils.isEmpty(toolbarTitle)) {
                            activityTitle = toolbarTitle;
                        }
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        PackageManager packageManager = activity.getPackageManager();
                        if (packageManager != null) {
                            ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                            if (!TextUtils.isEmpty(activityInfo.loadLabel(packageManager))) {
                                activityTitle = activityInfo.loadLabel(packageManager).toString();
                            }
                        }
                    }

                    return activityTitle;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * < setTag >.
     *
     * @param token 项目ID
     * @param view View
     * @param tagId ID
     * @param value Value
     */
    public static synchronized void setTag(final String token, final View view, final int tagId, final Object value) {
        if (null == token) {
            return;
        }

        HashMap<String, Object> tagMap = (HashMap<String, Object>) view.getTag(tagId);
        if (null == tagMap) {
            tagMap = new HashMap<>();
        }

        tagMap.put(token, value);
        view.setTag(tagId, tagMap);
    }

    /**
     * < getTag >.
     *
     * @param token 项目ID
     * @param view View
     * @param tagId ID
     * @return {@link Object}
     */
    public static synchronized Object getTag(final String token, final View view, final int tagId) {
        HashMap<String, Object> tagMap = (HashMap<String, Object>) view.getTag(tagId);
        if (null == tagMap) {
            return null;
        } else {
            return tagMap.get(token);
        }
    }

    /**
     * < getScreenNameAndTitleFromActivity >.
     *
     * @param properties JSONObject
     * @param activity Activity
     */
    public static void getScreenNameAndTitleFromActivity(JSONObject properties, Activity activity) {
        if (activity == null || properties == null) {
            return;
        }

        try {
            if (!TDPresetProperties.disableList.contains(TDConstants.SCREEN_NAME)) {
                properties.put(TDConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            }

            String activityTitle = activity.getTitle().toString();

            if (Build.VERSION.SDK_INT >= 11) {
                String toolbarTitle = getToolbarTitle(activity);
                if (!TextUtils.isEmpty(toolbarTitle)) {
                    activityTitle = toolbarTitle;
                }
            }

            if (TextUtils.isEmpty(activityTitle)) {
                PackageManager packageManager = activity.getPackageManager();
                if (packageManager != null) {
                    ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                    activityTitle = activityInfo.loadLabel(packageManager).toString();
                }
            }
            if (!TextUtils.isEmpty(activityTitle) && !TDPresetProperties.disableList.contains(TDConstants.TITLE)) {
                properties.put(TDConstants.TITLE, activityTitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * < getToolbarTitle >.
     *
     * @param activity Activity
     * @return {@link String}
     */
    @TargetApi(11)
    public static String getToolbarTitle(Activity activity) {
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            if (!TextUtils.isEmpty(actionBar.getTitle())) {
                return actionBar.getTitle().toString();
            }
        } else {
            try {
                Class<?> appCompatActivityClass = null;
                try {
                    appCompatActivityClass = Class.forName("android.support.v7.app.AppCompatActivity");
                } catch (Exception e) {
                    //ignored
                }
                if (appCompatActivityClass == null) {
                    try {
                        appCompatActivityClass = Class.forName("androidx.appcompat.app.AppCompatActivity");
                    } catch (Exception e) {
                        //ignored
                    }
                }
                if (appCompatActivityClass != null && appCompatActivityClass.isInstance(activity)) {
                    Method method = activity.getClass().getMethod("getSupportActionBar");
                    Object supportActionBar = method.invoke(activity);
                    if (supportActionBar != null) {
                        method = supportActionBar.getClass().getMethod("getTitle");
                        CharSequence charSequence = (CharSequence) method.invoke(supportActionBar);
                        if (charSequence != null) {
                            return charSequence.toString();
                        }
                    }
                }
            } catch (Exception e) {
                //ignored
            }
        }
        return null;
    }

    /**
     * < mergeJSONObject >.
     *
     * @param source sourceJSONObject
     * @param dest destJSONObject
     * @param timeZone TimeZone
     */
    public static void mergeJSONObject(final JSONObject source, JSONObject dest, TimeZone timeZone)
            throws JSONException {
        Iterator<String> sourceIterator = source.keys();
        while (sourceIterator.hasNext()) {
            String key = sourceIterator.next();
            Object value = source.get(key);
            if (value instanceof Date) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
                if (null != timeZone) {
                    dateFormat.setTimeZone(timeZone);
                }
                String time = dateFormat.format((Date) value);
                if (!Pattern.compile(TDConstants.TIME_CHECK_PATTERN).matcher(time).find()) {
                    time = TDUtils.formatTime((Date) value, timeZone);
                }
                dest.put(key, time);
            } else if (value instanceof JSONArray) {
                dest.put(key, formatJSONArray((JSONArray) value, timeZone));
            } else if (value instanceof JSONObject) {
                dest.put(key, formatJSONObject((JSONObject) value, timeZone));
            } else {
                dest.put(key, value);
            }
        }
    }

    /**
     * 用于合并两个嵌套json对象
     * [示例] JSONObject{key:JSONObject{key:value}}.
     * */
    public static void mergeNestedJSONObject(final JSONObject source, JSONObject dest, TimeZone timeZone)
            throws JSONException {
        Iterator<String> sourceIterator = source.keys();
        while (sourceIterator.hasNext()) {
            String sourceKey = sourceIterator.next();
            JSONObject sourceValue = source.optJSONObject(sourceKey);
            JSONObject destValue = dest.optJSONObject(sourceKey);
            if (sourceValue != null) {
                if (destValue == null) {
                    JSONObject newProperties = new JSONObject();
                    mergeJSONObject(sourceValue, newProperties, timeZone);
                    dest.put(sourceKey, newProperties);
                } else {
                    mergeJSONObject(sourceValue, destValue, timeZone);
                }
            }
        }
    }

    /**
     * < formatJSONArray with TimeZone >.
     *
     * @param jsonArr JSONArray
     * @param timeZone TimeZone
     * @return {@link JSONArray}
     */
    public static  JSONArray formatJSONArray(JSONArray jsonArr, TimeZone timeZone) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < jsonArr.length(); i++) {
            Object value = jsonArr.opt(i);
            if (value != null) {
                if (value instanceof  Date) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
                    if (null != timeZone) {
                        dateFormat.setTimeZone(timeZone);
                    }
                    String time = dateFormat.format((Date) value);
                    if (!Pattern.compile(TDConstants.TIME_CHECK_PATTERN).matcher(time).find()) {
                        time = TDUtils.formatTime((Date) value, timeZone);
                    }
                    result.put(time);
                } else if (value instanceof JSONArray) {
                    result.put(formatJSONArray((JSONArray) value, timeZone));
                } else if (value instanceof JSONObject) {
                    JSONObject newObject = formatJSONObject((JSONObject) value, timeZone);
                    result.put(newObject);
                } else {
                    result.put(value);
                }
            }

        }
        return result;
    }

    /**
     * < formatJSONObject with TimeZone >.
     *
     * @param jsonObject JSONObject
     * @param timeZone TimeZone
     * @return {@link JSONObject}
     */
    public static  JSONObject formatJSONObject(JSONObject jsonObject, TimeZone timeZone) {
        JSONObject result = new JSONObject();
        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = null;
            try {
                value = jsonObject.get(key);
                if (value instanceof Date) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
                    if (null != timeZone) {
                        dateFormat.setTimeZone(timeZone);
                    }
                    String time = dateFormat.format((Date) value);
                    if (!Pattern.compile(TDConstants.TIME_CHECK_PATTERN).matcher(time).find()) {
                        time = TDUtils.formatTime((Date) value, timeZone);
                    }
                    result.put(key, time);
                } else if (value instanceof JSONArray) {
                    result.put(key, formatJSONArray((JSONArray) value, timeZone));
                } else if (value instanceof  JSONObject) {
                    result.put(key, formatJSONObject((JSONObject) value, timeZone));
                } else {
                    result.put(key, value);
                }
            } catch (JSONException exception) {
                exception.printStackTrace();
            }

        }
        return result;

    }

    // 返回当前时区偏移，单位毫秒
    public static double getTimezoneOffset(long time, TimeZone timeZone) {
        TimeZone tz = (null == timeZone) ? TimeZone.getDefault() : timeZone;
        return tz.getOffset(time) / (1000.0 * 60 * 60);
    }

    /**
     * < getSuffix >.
     *
     * @param source String
     * @param length Len
     * @return {@link String}
     */
    public static String getSuffix(String source, int length) {
        if (TextUtils.isEmpty(source)) {
            return source;
        }
        if (source.length() <= length) {
            return source;
        }
        return source.substring(source.length() - 4);
    }

    /**
     * 获取主进程名字.
     */
    public  static  String getMainProcessName(Context context) {
        String processName = "";
        if (context == null) {
            return "";
        }
        TDContextConfig contextConfig = TDContextConfig.getInstance(context);
        processName = contextConfig.getMainProcessName();
        if (processName.length() == 0) {
            try {
                processName =  context.getApplicationInfo().processName;
            } catch (Exception ex) {
                //ignored
            }
        }
        return processName;
    }

    /**
     * 获取当前进程名字.
     * */
    public static  String getCurrentProcessName(Context context) {
        try {
            return ProcessUtil.getCurrentProcessName(context);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 判断当前进程是否为主进程.
     * */
    public static boolean isMainProcess(Context context) {
        if (context == null) {
            return true;
        }
        String currentProcess = TDUtils.getCurrentProcessName(context.getApplicationContext());
        String mainProcess = getMainProcessName(context);
        return !TextUtils.isEmpty(currentProcess) && mainProcess.equals(currentProcess);
    }

    /**
     * 获取OS名.
     * */
    public static String osName(Context context) {
        String osName = "Android";
        if (isHarmonyOS()) {
            osName = "HarmonyOS";
        }
        return osName;
    }

    /**
     * 获取OS版本.
     * */
    public static String osVersion(Context context) {
        String osVersion = exec(COMMAND_HARMONY_OS_VERSION);
        if (TextUtils.isEmpty(osVersion)) {
            return Build.VERSION.RELEASE;
        }
        return osVersion;
    }



    /**
     * 判断当前是否为鸿蒙系统.
     *
     * @return  是否是鸿蒙系统，是：true，不是：false
     */
    public static boolean isHarmonyOS() {
        try {
            Class<?> buildExClass = Class.forName("com.huawei.system.BuildEx");
            Object osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass);
            if (osBrand == null) {
                return false;
            }
            return "harmony".equalsIgnoreCase(osBrand.toString());
        } catch (Throwable e) {
            TDLog.i("HasHarmonyOS", e.getMessage());
            return false;
        }
    }

    /**
     * 新方法 获取鸿蒙系统 Version.
     *
     * @return HarmonyOS Version
     */
    public static String getHarmonyOSVersion() {
        String version = null;

        if (isHarmonyOS()) {
            version = getProp("hw_sc.build.platform.version", "");
            if (TextUtils.isEmpty(version)) {
                version = exec(COMMAND_HARMONY_OS_VERSION);
            }
        }
        return version;
    }

    private static String getProp(String property, String defaultValue) {
        try {
            Class spClz = Class.forName("android.os.SystemProperties");
            Method method = spClz.getDeclaredMethod("get", String.class);
            String value = (String) method.invoke(spClz, property);
            if (TextUtils.isEmpty(value)) {
                return defaultValue;
            }
            return value;
        } catch (Throwable throwable) {
            TDLog.i("TA.SystemProperties", throwable.getMessage());
        }
        return defaultValue;
    }


    /**
     * 执行命令获取对应内容.
     *
     * @param command 命令
     * @return 命令返回内容
     */
    public static String exec(String command) {
        InputStreamReader ir = null;
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec(command);
            ir = new InputStreamReader(process.getInputStream());
            input = new BufferedReader(ir);
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = input.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Throwable e) {
            TDLog.i("TDExec", e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable e) {
                    TDLog.i("TDExec", e.getMessage());
                }
            }
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException e) {
                    TDLog.i("TDExec", e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 获取FPS.
     * */
    public static int getFPS() {
        if (fps == 0) {
            fps = 60;
        }
        return fps;
    }

    /**
     * 监听FPS.
     * */
    public static void listenFPS() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final Choreographer.FrameCallback secondCallBack = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    synchronized (frameLock) {
                        secondVsync = frameTimeNanos;
                        if (secondVsync <= firstVsync) {
                            fps = 60;
                        } else {
                            try {
                                long hz = 1000000000 / (secondVsync - firstVsync);
                                if (hz > 70) {
                                    fps = 60;
                                } else {
                                    fps = (int) hz;
                                }
                            } catch (Exception e) {
                                fps = 60;
                            }
                        }
                    }
                }
            };

            final Choreographer.FrameCallback firstCallBack = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    synchronized (frameLock) {
                        firstVsync = frameTimeNanos;
                        Choreographer.getInstance().postFrameCallback(secondCallBack);
                    }
                }
            };
            final Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    handler.postDelayed(this, 500);
                    Choreographer.getInstance().postFrameCallback(firstCallBack);
                }
            };
            handler.postDelayed(runnable, 500);
        }
    }

    /**
     * 产生numSize位16进制随机数.
     *
     * @param numSize Len
     * @return String
     */
    public static String getRandomHEXValue(int numSize) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < numSize; i++) {
            char temp = 0;
            int key = (int) (Math.random() * 2);
            switch (key) {
                case 0:
                    temp = (char) (Math.random() * 10 + 48); //产生随机数字
                    break;
                case 1:
                    temp = (char) (Math.random() * 6 + 'a'); //产生a-f
                    break;
                default:
                    break;
            }
            str.append(temp);
        }
        return str.toString();
    }

    /**
     * 保留一位小数.
     *
     * @param num double
     * @return 一位小数double
     */
    public static double formatNumber(double num) {
        return (double) Math.round(num * 10) / 10;
    }

    /**
     * 判断当前应用是否在前台.
     * */
    public static boolean isForeground(Context context) {
        ActivityManager activityManager
                = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (null == ProcessUtil.runningAppList) {
            ProcessUtil.runningAppList = activityManager.getRunningAppProcesses();
        }
        String processName = "";
        for (ActivityManager.RunningAppProcessInfo appProcess : ProcessUtil.runningAppList) {
            processName = appProcess.processName;
            int p = processName.indexOf(":");
            if (p != -1) {
                processName = processName.substring(0, p);
            }
            if (processName.equals(context.getPackageName())) {
                return appProcess.importance
                        == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        || appProcess.importance
                        == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
            }
        }
        return false;
    }


    /**
     * 网络类型转换
     * @param networkType
     * @return
     */
    public static int convertToNetworkType(String networkType) {
        if ("NULL".equals(networkType)) {
            return TDConfig.NetworkType.TYPE_ALL;
        } else if ("WIFI".equals(networkType)) {
            return TDConfig.NetworkType.TYPE_WIFI;
        } else if ("2G".equals(networkType)) {
            return TDConfig.NetworkType.TYPE_2G;
        } else if ("3G".equals(networkType)) {
            return TDConfig.NetworkType.TYPE_3G;
        } else if ("4G".equals(networkType)) {
            return TDConfig.NetworkType.TYPE_4G;
        } else if ("5G".equals(networkType)) {
            return TDConfig.NetworkType.TYPE_5G;
        }
        return TDConfig.NetworkType.TYPE_ALL;
    }

    /**
     * 手机为Phone，平板为Tablet.
     *
     * @author bugliee
     * @create 2022/8/16
     * @param context Context
     * @return {@link boolean}
     */
    public static String getDeviceType(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                < Configuration.SCREENLAYOUT_SIZE_LARGE ? "Phone" : "Tablet";
    }

    /**
     * 判断本地日志开关文件是否存在.
     */
    public static boolean isLogControlFileExist() {
        return new File(TDConstants.KEY_LOG_CONTROL_FILE_NAME).exists();
    }

    /**
     * < 时间格式化 >.
     *
     * @author bugliee
     * @create 2022/9/21
     * @param mDate 需要格式化的时间
     * @param mTimeZone 时区
     * @return {@link String}
     */
    public static String formatTime(Date mDate, TimeZone mTimeZone) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setTimeZone(mTimeZone);
        calendar.setTime(mDate);
        return String.format(Locale.CHINA, "%04d-%02d-%02d %02d:%02d:%02d.%3d",
                calendar.get(Calendar.YEAR),
                (calendar.get(Calendar.MONTH) + 1),
                calendar.get(Calendar.DAY_OF_MONTH),
                (calendar.get(Calendar.AM_PM) == Calendar.AM ? calendar.get(Calendar.HOUR) : calendar.get(Calendar.HOUR) + 12),
                (calendar.get(Calendar.MINUTE)),
                calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND));
    }
}
