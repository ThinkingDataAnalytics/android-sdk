package cn.thinkingdata.android.utils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
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

import cn.thinkingdata.android.R;
import cn.thinkingdata.android.ScreenAutoTracker;
import cn.thinkingdata.android.TDContextConfig;
import cn.thinkingdata.android.TDPresetProperties;
import cn.thinkingdata.android.ThinkingDataFragmentTitle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TDUtils {
    static  long firstVsync;
    static  long secondVsync;
    static  volatile int fps;
    public static final String COMMAND_HARMONYOS_VERSION = "getprop hw_sc.build.platform.version";

    public static boolean hasClassName(Object o, String className) {
        Class<?> klass = o.getClass();
        while (klass.getCanonicalName() != null) {
            if (klass.getCanonicalName().equals(className)) {
                return true;
            }

            if (klass == Object.class) {
                break;
            }

            klass = klass.getSuperclass();
        }
        return false;
    }

    private static int getChildIndex(ViewParent parent, View child) {
        if (!(parent instanceof ViewGroup)) {
            return -1;
        }
        try {
            ViewGroup _parent = (ViewGroup) parent;
            String childId = TDUtils.getViewId(child);

            String childName = child.getClass().getCanonicalName();
            int index = 0;
            for (int i = 0; i < _parent.getChildCount(); i++) {
                View brother = _parent.getChildAt(i);
                Class<?> clazz = brother.getClass();
                String canonicalName = "";
                boolean classExist = false;
                do {
                    canonicalName = clazz.getCanonicalName();
                    if (canonicalName != null && canonicalName.equals(childName)) {
                        classExist = true;
                        break;
                    }
                    clazz = clazz.getSuperclass();
                } while (TextUtils.isEmpty(canonicalName) || clazz == Object.class);
                if (!classExist) {
                    continue;
                }
                String brotherId = TDUtils.getViewId(brother);
                if (childId != null && !childId.equals(brotherId)) {
                    index++;
                    continue;
                }
                if (child == brother) {
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


    public static void addViewPathProperties(Activity activity, View view, JSONObject properties) {
        try {
            if (view == null) {
                return;
            }

            List<String> viewPath = new ArrayList<>();
            ViewParent viewParent;

            if (properties == null) {
                properties = new JSONObject();
            }

            do {
                viewParent = view.getParent();
                int viewIndex = getChildIndex(viewParent, view);
                viewPath.add(view.getClass().getCanonicalName() + "[" + viewIndex + "]");
                if (viewParent instanceof ViewGroup) {
                    view = (ViewGroup) viewParent;
                }

            } while (viewParent instanceof ViewGroup);

            Collections.reverse(viewPath);

            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 1; i < viewPath.size(); i++) {
                stringBuilder.append(viewPath.get(i));
                if (i != (viewPath.size() - 1)) {
                    stringBuilder.append("/");
                }
            }
            if (!TDPresetProperties.disableList.contains(TDConstants.ELEMENT_SELECTOR)) {
                properties.put(TDConstants.ELEMENT_SELECTOR, stringBuilder.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String traverseView(StringBuilder stringBuilder, ViewGroup rootView) {
        try {
            if (rootView == null) {
                return stringBuilder.toString();
            }

            final int childCount = rootView.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View childView = rootView.getChildAt(i);

                if (childView.getVisibility() != View.VISIBLE) {
                    continue;
                }

                if (childView instanceof ViewGroup) {
                    traverseView(stringBuilder, (ViewGroup) childView);
                } else {
                    Class<?> switchCompatClass = null;

                    try {
                        switchCompatClass = Class.forName("androidx.appcompat.widget.SwitchCompat");
                    } catch (Exception e) {}

                    if (switchCompatClass == null) {
                        try {
                            switchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
                        } catch (Exception e) {}
                    }

                    CharSequence viewText = null;
                    if (childView instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) childView;
                        viewText = checkBox.getText();
                    } else if (switchCompatClass != null && switchCompatClass.isInstance(childView)) {
                        CompoundButton switchCompat = (CompoundButton) childView;
                        Method method;
                        if (switchCompat.isChecked()) {
                            method = childView.getClass().getMethod("getTextOn");
                        } else {
                            method = childView.getClass().getMethod("getTextOff");
                        }
                        viewText = (String) method.invoke(childView);
                    } else if (childView instanceof ImageView) {
                        ImageView imageView = (ImageView) childView;
                        if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                            viewText = imageView.getContentDescription().toString();
                        }
                    } else if (childView instanceof ToggleButton) {
                        ToggleButton toggleButton = (ToggleButton) childView;
                        boolean isChecked = toggleButton.isChecked();
                        if (isChecked) {
                            viewText = toggleButton.getTextOn();
                        } else {
                            viewText = toggleButton.getTextOff();
                        }
                    } else if (childView instanceof RadioButton) {
                        RadioButton radioButton = (RadioButton) childView;
                        viewText = radioButton.getText();
                    } else if (childView instanceof Button) {
                        Button button = (Button) childView;
                        viewText = button.getText();
                    } else if (childView instanceof CheckedTextView) {
                        CheckedTextView textView = (CheckedTextView) childView;
                        viewText = textView.getText();
                    } else if (childView instanceof TextView) {
                        TextView textView = (TextView) childView;
                        viewText = textView.getText();
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
                        if(!TDPresetProperties.disableList.contains(TDConstants.SCREEN_NAME)) {
                            properties.put(TDConstants.SCREEN_NAME, fragmentName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取fragmentTitle
    public static String getTitleFromFragment(final Object fragment, final String token) {
        String title = null;
        try {
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                JSONObject properties = screenAutoTracker.getScreenTrackProperties();
                if (properties != null) {
                    if (properties.has(TDConstants.TITLE)) {
                        title = properties.optString(TDConstants.TITLE);
                    }
                }
            }

            if (TextUtils.isEmpty(title) && fragment.getClass()
                    .isAnnotationPresent(ThinkingDataFragmentTitle.class)) {
                ThinkingDataFragmentTitle fragmentTitle =
                        fragment.getClass().getAnnotation(ThinkingDataFragmentTitle.class);
                if (fragmentTitle != null) {
                    if (TextUtils.isEmpty(fragmentTitle.appId())
                            || token.equals(fragmentTitle.appId())) {
                        title = fragmentTitle.title();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return title;
    }

    public static Activity getActivityFromContext(Context context) {
        try {
            if (context != null) {
                Activity activity = null;
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (context instanceof ContextWrapper && !(context instanceof Activity)) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                }
                return activity;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getViewId(View view) {
        return getViewId(view, null);
    }
    public static String getViewId(View view, String token) {
        try {
            String idStr = (String) getTag(token, view, R.id.thinking_analytics_tag_view_id);
            if (view.getId() != View.NO_ID && TextUtils.isEmpty(idStr)) {
                idStr = view.getContext().getResources().getResourceEntryName(view.getId());
            }
            return idStr;
        } catch (Exception ignore) {}
        return null;
    }

    public static String getActivityTitle(Activity activity) {
        String activityTitle = null;
        try {
            if (activity != null) {
                try {
                    if (!TextUtils.isEmpty(activity.getTitle())) {
                        activityTitle = activity.getTitle().toString();
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        PackageManager pm = activity.getPackageManager();
                        if (pm != null) {
                            ActivityInfo info = pm.getActivityInfo(activity.getComponentName(), 0);
                            if (!TextUtils.isEmpty(info.loadLabel(pm))) {
                                activityTitle = info.loadLabel(pm).toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    return activityTitle;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return activityTitle;
        }
        return activityTitle;
    }

    synchronized public static void setTag(final String token, final View view, final int tagId, final Object value) {
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

    synchronized public static Object getTag(final String token, final View view, final int tagId) {
        HashMap<String, Object> tagMap = (HashMap<String, Object>) view.getTag(tagId);
        if (null == tagMap) {
            return null;
        } else {
            return tagMap.get(token);
        }
    }

    public static void getScreenNameAndTitleFromActivity(JSONObject properties, Activity activity) {
        if (activity == null || properties == null) {
            return;
        }

        try {
            if (!TDPresetProperties.disableList.contains(TDConstants.SCREEN_NAME)) {
                properties.put(TDConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            }

            String activityTitle = activity.getTitle().toString();

            if (TextUtils.isEmpty(activityTitle)) {
                PackageManager pm = activity.getPackageManager();
                if (pm != null) {
                    ActivityInfo activityInfo = pm.getActivityInfo(activity.getComponentName(), 0);
                    activityTitle = activityInfo.loadLabel(pm).toString();
                }
            }
            if (!TextUtils.isEmpty(activityTitle) && !TDPresetProperties.disableList.contains(TDConstants.TITLE)) {
                properties.put(TDConstants.TITLE, activityTitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                    appCompatActivityClass = Class.forName("androidx.appcompat.app.AppCompatActivity");
                } catch (Exception e) { }
                if (appCompatActivityClass == null) {
                    try {
                        appCompatActivityClass = Class.forName("android.support.v7.app.AppCompatActivity");
                    } catch (Exception e) { }
                }
                if (appCompatActivityClass != null && appCompatActivityClass.isInstance(activity)) {
                    Method method = activity.getClass().getMethod("getSupportActionBar");
                    if (method != null) {
                        Object supportActionBar = method.invoke(activity);
                        if (supportActionBar != null) {
                            method = supportActionBar.getClass().getMethod("getTitle");
                            if (method != null) {
                                CharSequence charSequence = (CharSequence) method.invoke(supportActionBar);
                                if (charSequence != null) {
                                    return charSequence.toString();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) { }
        }
        return null;
    }

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
                dest.put(key, dateFormat.format((Date) value));
            } else if (value instanceof JSONArray) {
                dest.put(key,formatJSONArray((JSONArray)value,timeZone));
            } else if(value instanceof JSONObject)
            {
                dest.put(key,formatJSONObject((JSONObject) value,timeZone));
            }else
                {
                dest.put(key, value);
            }
        }
    }

    /**
     * 用于合并两个嵌套json对象
     * [示例] JSONObject{key:JSONObject{key:value}}
     * */
    public static void mergeNestedJSONObject(final JSONObject source, JSONObject dest, TimeZone timeZone)
            throws JSONException {
        Iterator<String> sourceIterator = source.keys();
        while (sourceIterator.hasNext()) {
            String sourceKey = sourceIterator.next();
            JSONObject sourceValue = source.optJSONObject(sourceKey);
            JSONObject destValue = dest.optJSONObject(sourceKey);
            if (sourceValue != null){
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

    public static  JSONArray formatJSONArray(JSONArray jsonArr,TimeZone timeZone)
    {
        JSONArray result = new JSONArray();
        for(int i = 0 ;i < jsonArr.length();i++)
        {
            Object value = jsonArr.opt(i);
            if(value != null)
            {
                if(value instanceof  Date)
                {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
                    if (null != timeZone) {
                        dateFormat.setTimeZone(timeZone);
                    }
                    result.put(dateFormat.format((Date) value));
                }else if(value instanceof JSONArray)
                {
                    result.put(formatJSONArray((JSONArray)value,timeZone));
                }else if(value instanceof JSONObject)
                {
                    JSONObject newObject = formatJSONObject((JSONObject) value,timeZone);
                    result.put(newObject);
                }else
                {
                    result.put(value);
                }
            }

        }
        return result;
    }
    public static  JSONObject formatJSONObject(JSONObject jsonObject,TimeZone timeZone)
    {
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
                    result.put(key, dateFormat.format((Date) value));
                } else if (value instanceof JSONArray) {
                    result.put(key, formatJSONArray((JSONArray) value,timeZone));
                } else if(value instanceof  JSONObject)
                {
                    result.put(key, formatJSONObject((JSONObject) value,timeZone));
                }else
                {
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

    public static String getSuffix(String source, int length) {
        if (TextUtils.isEmpty(source)) return source;
        if (source.length() <= length) return source;
        return source.substring(source.length() - 4);
    }
    /**
     * 获取主进程名字
     */
    public  static  String getMainProcessName(Context context) {
        String processName = "";
        if (context == null)
            return "";
        try {
           processName =  context.getApplicationInfo().processName;
        } catch (Exception ex) {
        }
        if(processName.length() == 0)
        {
            TDContextConfig contextConfig = TDContextConfig.getInstance(context);
            processName = contextConfig.getMainProcessName();
        }
        return processName;
    }
    /**
     * 获取当前进程名字
     * */
    public static  String getCurrentProcessName(Context context) {
        try {
            int pid = android.os.Process.myPid();
            ActivityManager activityManager = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return "";
            }
            List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList = activityManager.getRunningAppProcesses();
            if (runningAppProcessInfoList != null) {
                for (ActivityManager.RunningAppProcessInfo appProcess : runningAppProcessInfoList) {

                    if (appProcess != null) {
                        if (appProcess.pid == pid) {
                            return appProcess.processName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return "";
    }
    /**
     * 判断当前进程是否为主进程
     * */
    public static boolean isMainProcess(Context context) {
        if (context == null) {
            return true;
        }
        String currentProcess = TDUtils.getCurrentProcessName(context.getApplicationContext());
        String mainProcess = getMainProcessName(context);
        if (!TextUtils.isEmpty(currentProcess) && mainProcess.equals(currentProcess)) {
            return true;
        }
        return false;
    }

    public static String osName(Context context)
    {
        String osName = "Android";
        if(isHarmonyOS())
        {
            osName = "HarmonyOS";
        }
        return osName;
    }
    public static String osVersion(Context context)
    {
        String osVersion = exec(COMMAND_HARMONYOS_VERSION);
        if(TextUtils.isEmpty(osVersion))
        {
            return Build.VERSION.RELEASE;
        }
        return osVersion;
    }

    public static boolean isHarmonyOS() {
        return !TextUtils.isEmpty(exec(COMMAND_HARMONYOS_VERSION));
    }

    /**
     * 执行命令获取对应内容
     * @param command 命令
     * @return 命令返回内容
     */
    public static String exec(String command) {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            Process process = Runtime.getRuntime().exec(command);
            inputStreamReader = new InputStreamReader(process.getInputStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Throwable e) {
            TDLog.i("TDExec", e.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Throwable e) {
                    TDLog.i("TDExec", e.getMessage());
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    TDLog.i("TDExec", e.getMessage());
                }
            }
        }
        return null;
    }


    public  static  int getFPS()
    {
        if(fps == 0 )
        {
            fps = 60;
        }
        return fps;
    }
    public static void listenFPS()
    {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            final  Choreographer.FrameCallback secondCallBack = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    secondVsync = frameTimeNanos;
                    long hz = 1000000000 / (secondVsync - firstVsync);
                    if (hz > 70) {
                        fps = 60;
                    } else {
                        fps = (int)hz;
                    }
                }
            };

            final Choreographer.FrameCallback firstCallBack = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    firstVsync = frameTimeNanos;
                    Choreographer.getInstance().postFrameCallback(secondCallBack);
                }
            };
            final Handler handler=new Handler();
            Runnable runnable=new Runnable() {
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
     * 产生numSize位16进制随机数
     * @param numSize
     * @return String
     */
    public static String getRandomHEXValue(int numSize) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < numSize; i++) {
            char temp = 0;
            int key = (int) (Math.random() * 2);
            switch (key) {
                case 0:
                    temp = (char) (Math.random() * 10 + 48);//产生随机数字
                    break;
                case 1:
                    temp = (char) (Math.random() * 6 + 'a');//产生a-f
                    break;
                default:
                    break;
            }
            str.append(temp);
        }
        return str.toString();
    }
}
