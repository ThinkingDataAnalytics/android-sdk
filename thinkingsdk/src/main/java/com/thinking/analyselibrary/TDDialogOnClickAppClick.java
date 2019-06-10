package com.thinking.analyselibrary;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.thinking.analyselibrary.utils.AopUtil;
import com.thinking.analyselibrary.utils.TDLog;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

import java.lang.reflect.Method;

public class TDDialogOnClickAppClick {
    private final static String TAG = "TDDialogOnClickAppClick";

    public static void onAppClick(final JoinPoint joinPoint) {
        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK instance) {
                try {
                    if (!instance.isAutoTrackEnabled()) {
                        return;
                    }

                    if (instance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                        return;
                    }

                    if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 2) {
                        return;
                    }

                    DialogInterface dialogInterface = (DialogInterface) joinPoint.getArgs()[0];
                    if (dialogInterface == null) {
                        return;
                    }

                    int whichButton = (int) joinPoint.getArgs()[1];

                    Dialog dialog = null;
                    if (dialogInterface instanceof Dialog) {
                        dialog = (Dialog) dialogInterface;
                    }

                    if (dialog == null) {
                        return;
                    }

                    Context context = dialog.getContext();
                    Activity activity = AopUtil.getActivityFromContext(context);

                    if (activity == null) {
                        activity = dialog.getOwnerActivity();
                    }

                    if (activity != null) {
                        if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    if (AopUtil.isViewIgnored(instance, Dialog.class)) {
                        return;
                    }

                    JSONObject properties = new JSONObject();

                    try {
                        if (dialog.getWindow() != null) {
                            String idString = (String) AopUtil.getTag(instance.getToken(), dialog.getWindow().getDecorView(),
                                    R.id.thinking_analytics_tag_view_id);
                            if (!TextUtils.isEmpty(idString)) {
                                properties.put(AopConstants.ELEMENT_ID, idString);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    properties.put(AopConstants.ELEMENT_TYPE, "Dialog");

                    Class<?> alertDialogClass = null;
                    try {
                        alertDialogClass = Class.forName("android.support.v7.app.AlertDialog)");
                    } catch (Exception e ) {
                        // ignore
                    }
                    if (null == alertDialogClass) {
                        try {
                            alertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    if (dialog instanceof android.app.AlertDialog) {
                        android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                        Button button = alertDialog.getButton(whichButton);
                        if (button != null) {
                            if (!TextUtils.isEmpty(button.getText())) {
                                properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                            }
                        } else {
                            ListView listView = alertDialog.getListView();
                            if (listView != null) {
                                ListAdapter listAdapter = listView.getAdapter();
                                Object object = listAdapter.getItem(whichButton);
                                if (object != null) {
                                    if (object instanceof String) {
                                        properties.put(AopConstants.ELEMENT_CONTENT, (String) object);
                                    }
                                }
                            }
                        }

                    } else if (null != alertDialogClass && alertDialogClass.isInstance(dialog)) {
                        Button button = null;
                        try {
                            Method getButtonMethod = dialog.getClass().getMethod("getButton", new Class[]{int.class});
                            if (getButtonMethod != null) {
                                button = (Button) getButtonMethod.invoke(dialog, whichButton);
                            }
                        } catch (Exception e) {
                            //ignored
                        }

                        if (button != null) {
                            if (!TextUtils.isEmpty(button.getText())) {
                                properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                            }
                        } else {
                            try {
                                Method getListViewMethod = dialog.getClass().getMethod("getListView");
                                if (getListViewMethod != null) {
                                    ListView listView = (ListView) getListViewMethod.invoke(dialog);
                                    if (listView != null) {
                                        ListAdapter listAdapter = listView.getAdapter();
                                        Object object = listAdapter.getItem(whichButton);
                                        if (object != null) {
                                            if (object instanceof String) {
                                                properties.put(AopConstants.ELEMENT_CONTENT, object);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                //ignored
                            }
                        }
                    }

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    TDLog.i(TAG, " DialogInterface.OnClickListener.onClick AOP ERROR: " + e.getMessage());
                }

            }
        });
    }

    public static void onMultiChoiceAppClick(final JoinPoint joinPoint) {
        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK instance) {
                try {
                    if (!instance.isAutoTrackEnabled()) {
                        return;
                    }

                    if (instance.isAutoTrackEventTypeIgnored(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CLICK)) {
                        return;
                    }

                    if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 3) {
                        return;
                    }

                    DialogInterface dialogInterface = (DialogInterface) joinPoint.getArgs()[0];
                    if (dialogInterface == null) {
                        return;
                    }

                    int whichButton = (int) joinPoint.getArgs()[1];

                    Dialog dialog = null;
                    if (dialogInterface instanceof Dialog) {
                        dialog = (Dialog) dialogInterface;
                    }

                    if (dialog == null) {
                        return;
                    }

                    Context context = dialog.getContext();

                    Activity activity = null;
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }

                    if (activity != null) {
                        if (instance.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    if (AopUtil.isViewIgnored(instance, Dialog.class)) {
                        return;
                    }

                    JSONObject properties = new JSONObject();

                    try {
                        if (dialog.getWindow() != null) {
                            String idString = (String) AopUtil.getTag(instance.getToken(), dialog.getWindow().getDecorView(),
                                    R.id.thinking_analytics_tag_view_id);
                            if (!TextUtils.isEmpty(idString)) {
                                properties.put(AopConstants.ELEMENT_ID, idString);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    properties.put(AopConstants.ELEMENT_TYPE, "Dialog");
                    Class<?> alertDialogClass = null;
                    try {
                        alertDialogClass = Class.forName("android.support.v7.app.AlertDialog)");
                    } catch (Exception e ) {
                        // ignore
                    }
                    if (null == alertDialogClass) {
                        try {
                            alertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    if (dialog instanceof android.app.AlertDialog) {
                        android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                        Button button = alertDialog.getButton(whichButton);
                        if (button != null) {
                            if (!TextUtils.isEmpty(button.getText())) {
                                properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                            }
                        } else {
                            ListView listView = alertDialog.getListView();
                            if (listView != null) {
                                ListAdapter listAdapter = listView.getAdapter();
                                Object object = listAdapter.getItem(whichButton);
                                if (object != null) {
                                    if (object instanceof String) {
                                        properties.put(AopConstants.ELEMENT_CONTENT, (String) object);
                                    }
                                }
                            }
                        }

                    } else if (null != alertDialogClass && alertDialogClass.isInstance(dialog)) {
                        Button button = null;
                        try {
                            Method getButtonMethod = dialog.getClass().getMethod("getButton", new Class[]{int.class});
                            if (getButtonMethod != null) {
                                button = (Button) getButtonMethod.invoke(dialog, whichButton);
                            }
                        } catch (Exception e) {
                            //ignored
                        }

                        if (button != null) {
                            if (!TextUtils.isEmpty(button.getText())) {
                                properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                            }
                        } else {
                            try {
                                Method getListViewMethod = dialog.getClass().getMethod("getListView");
                                if (getListViewMethod != null) {
                                    ListView listView = (ListView) getListViewMethod.invoke(dialog);
                                    if (listView != null) {
                                        ListAdapter listAdapter = listView.getAdapter();
                                        Object object = listAdapter.getItem(whichButton);
                                        if (object != null) {
                                            if (object instanceof String) {
                                                properties.put(AopConstants.ELEMENT_CONTENT, object);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                //ignored
                            }
                        }
                    }

                    instance.autoTrack(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    TDLog.i(TAG, " DialogInterface.OnMultiChoiceClickListener.onClick AOP ERROR: " + e.getMessage());
                }

            }
        });
    }
}
