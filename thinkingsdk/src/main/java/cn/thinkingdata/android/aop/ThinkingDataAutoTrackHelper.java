/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.aop;

import android.content.DialogInterface;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.RadioGroup;
import cn.thinkingdata.android.ThinkingDataRuntimeBridge;
import java.lang.reflect.Method;

/**
 * Fully buried auxiliary class
 * */
public class ThinkingDataAutoTrackHelper {

    public static final String TAG = "ThinkingAnalytics";

    public static void trackViewOnClick(View view) {
        ThinkingDataRuntimeBridge.onViewOnClick(view, null);
    }

    public static void trackViewOnClick(View view, String appId) {
        ThinkingDataRuntimeBridge.onViewOnClick(view, appId);
    }

    public static void track(String eventName, String properties, String appId) {
        ThinkingDataRuntimeBridge.trackEvent(eventName, properties, appId);
    }

    public static void trackListView(AdapterView<?> adapterView, View view, int position) {
        ThinkingDataRuntimeBridge.onAdapterViewItemClick(adapterView, view, position);
    }

    public static void trackRadioGroup(RadioGroup view, int checkedId) {
        ThinkingDataRuntimeBridge.onViewOnClick(view, null);
    }

    public static void trackRadioGroup(RadioGroup view, int checkedId, String ignoreAppId) {
        ThinkingDataRuntimeBridge.onViewOnClick(view, ignoreAppId);
    }

    public static void trackDialog(DialogInterface dialogInterface, int whichButton) {
        ThinkingDataRuntimeBridge.onDialogClick(dialogInterface, whichButton);
    }

    public static void trackMenuItem(final Object object, final MenuItem menuItem) {
        ThinkingDataRuntimeBridge.onMenuItemSelected(object, menuItem);
    }

    public static void trackMenuItem(final MenuItem menuItem) {
    }

    public static void trackTabHost(final String tabName) {
        ThinkingDataRuntimeBridge.onTabHostChanged(tabName);
    }

    public static void trackTabLayoutSelected(Object object, Object tab) {
        try {
            Method getTextMethod = tab.getClass().getDeclaredMethod("getText");
            Object text = getTextMethod.invoke(tab);
            if (null != text) {
                ThinkingDataRuntimeBridge.onTabHostChanged(text.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackExpandableListViewOnGroupClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition) {
        ThinkingDataRuntimeBridge.onExpandableListViewOnGroupClick(expandableListView, view, groupPosition);
    }

    public static void trackExpandableListViewOnChildClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition, int childPosition) {
        ThinkingDataRuntimeBridge.onExpandableListViewOnChildClick(expandableListView, view, groupPosition, childPosition);
    }

}
