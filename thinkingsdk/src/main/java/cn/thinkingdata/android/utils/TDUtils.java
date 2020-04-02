package cn.thinkingdata.android.utils;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
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
import cn.thinkingdata.android.ThinkingDataFragmentTitle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private static int getChildIndex(ViewParent parent, View child) {
        try {
            if (!(parent instanceof ViewGroup)) {
                return -1;
            }

            ViewGroup _parent = (ViewGroup) parent;
            final String childIdName = TDUtils.getViewId(child);

            String childClassName = child.getClass().getCanonicalName();
            int index = 0;
            for (int i = 0; i < _parent.getChildCount(); i++) {
                View brother = _parent.getChildAt(i);

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
            properties.put(TDConstants.ELEMENT_SELECTOR, stringBuffer.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                    //if (isViewIgnored(child)) {
                    //    continue;
                   // }

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
                        viewText = (String)method.invoke(child);
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
                        properties.put(TDConstants.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", screenName, fragmentName));
                    } else {
                        properties.put(TDConstants.SCREEN_NAME, fragmentName);
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
                    if (TextUtils.isEmpty(thinkingDataFragmentTitle.appId()) ||
                            token.equals(thinkingDataFragmentTitle.appId())) {
                        title = thinkingDataFragmentTitle.title();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return title;
    }

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
            properties.put(TDConstants.SCREEN_NAME, activity.getClass().getCanonicalName());

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
            if (!TextUtils.isEmpty(activityTitle)) {
                properties.put(TDConstants.TITLE, activityTitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
            } catch (Exception e) {
                //ignored
            }
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
                JSONArray finalArray = new JSONArray();
                JSONArray originalArray = (JSONArray) value;
                for (int i = 0; i < originalArray.length(); i++) {
                    Object element = originalArray.get(i);
                    if (element instanceof Date) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
                        if (null != timeZone) {
                            dateFormat.setTimeZone(timeZone);
                        }
                        finalArray.put(dateFormat.format((Date) element));
                    } else {
                        finalArray.put(element);
                    }
                }
                dest.put(key, finalArray);
            } else {
                dest.put(key, value);
            }
        }
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
}
