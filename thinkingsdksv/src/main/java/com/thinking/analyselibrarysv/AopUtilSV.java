package com.thinking.analyselibrarysv;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AopUtilSV {

    private static int getChildIndex(ViewParent parent, View child) {
        try {
            if (parent == null || !(parent instanceof ViewGroup)) {
                return -1;
            }

            ViewGroup _parent = (ViewGroup) parent;
            final String childIdName = AopUtilSV.getViewId(child);

            String childClassName = child.getClass().getCanonicalName();
            int index = 0;
            for (int i = 0; i < _parent.getChildCount(); i++) {
                View brother = _parent.getChildAt(i);

                if (!PathfinderSV.hasClassName(brother, childClassName)) {
                    continue;
                }

                String brotherIdName = AopUtilSV.getViewId(brother);

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
            properties.put("#element_selector", stringBuffer.toString());
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
                    if (isViewIgnored(child)) {
                        continue;
                    }

                    CharSequence viewText = null;
                    if (child instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) child;
                        viewText = checkBox.getText();
                    } else if (child instanceof SwitchCompat) {
                        SwitchCompat switchCompat = (SwitchCompat) child;
                        viewText = switchCompat.getTextOn();
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
                String fragmentName = (String) view.getTag(R.id.thinking_analytics_sv_tag_view_fragment_name);
                String fragmentName2 = (String) view.getTag(R.id.thinking_analytics_sv_tag_view_fragment_name2);
                if (!TextUtils.isEmpty(fragmentName2)) {
                    fragmentName = fragmentName2;
                }
                if (!TextUtils.isEmpty(fragmentName)) {
                    String screenName = properties.optString(AopConstantsSV.SCREEN_NAME);
                    if (!TextUtils.isEmpty(fragmentName)) {
                        properties.put(AopConstantsSV.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", screenName, fragmentName));
                    } else {
                        properties.put(AopConstantsSV.SCREEN_NAME, fragmentName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Activity getActivityFromContext(Context context, View view) {
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
                } else {
                    if (view != null) {
                        Object object = view.getTag(R.id.thinking_analytics_sv_tag_view_activity);
                        if (object != null) {
                            if (object instanceof Activity) {
                                activity = (Activity) object;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activity;
    }

    public static String getViewId(View view) {
        String idString = null;
        try {
            idString = (String) view.getTag(R.id.thinking_analytics_sv_tag_view_id);
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

    public static boolean isViewIgnored(Class viewType) {
        try {
            if (viewType == null) {
                return true;
            }

            List<Class> mIgnoredViewTypeList = ThinkingAnalyticsSDKSV.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                for (Class clazz : mIgnoredViewTypeList) {
                    if (clazz.isAssignableFrom(viewType)) {
                        return true;
                    }

                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isViewIgnored(View view) {
        try {
            if (view == null) {
                return true;
            }

            List<Class> mIgnoredViewTypeList = ThinkingAnalyticsSDKSV.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                for (Class clazz : mIgnoredViewTypeList) {
                    if (clazz.isAssignableFrom(view.getClass())) {
                        return true;
                    }
                }
            }

            if ("1".equals(view.getTag(R.id.thinking_analytics_sv_tag_view_ignored))) {
                return true;
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
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
                        String toolbarTitle = TDUtilSV.getToolbarTitle(activity);
                        if (!TextUtils.isEmpty(toolbarTitle)) {
                            activityTitle = toolbarTitle;
                        }
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        PackageManager packageManager = activity.getPackageManager();
                        if (packageManager != null) {
                            ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                            if (activityInfo != null) {
                                if (!TextUtils.isEmpty(activityInfo.loadLabel(packageManager))) {
                                    activityTitle = activityInfo.loadLabel(packageManager).toString();
                                }
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

    public static void mergeJSONObject(final JSONObject source, JSONObject dest)
            throws JSONException {
        try {
            Iterator<String> superPropertiesIterator = source.keys();
            while (superPropertiesIterator.hasNext()) {
                String key = superPropertiesIterator.next();
                Object value = source.get(key);
                if (value instanceof Date) {
                    synchronized (mDateFormat) {
                        dest.put(key, mDateFormat.format((Date) value));
                    }
                } else {
                    dest.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"
            + ".SSS", Locale.CHINA);
}
