package com.thinking.analyselibrary.runtime;

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
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.aspectj.lang.JoinPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class TDAopUtil {
    private static Class clazz;
    private static Object object;

    public static void sendTrackEventToSDK3(final JoinPoint joinPoint, final String methodName, Object result) {
        try {
            if (joinPoint == null || TextUtils.isEmpty(methodName)) {
                return;
            }

            if (clazz == null) {
                clazz = Class.forName("com.thinking.analyselibrary.ThinkingDataRuntimeBridge");
            }
            if (clazz == null) {
                return;
            }

            if (object == null) {
                object = clazz.newInstance();
            }
            if (object == null) {
                return;
            }

            Class[] params = new Class[2];
            params[0] = JoinPoint.class;
            params[1] = Object.class;

            Method method = clazz.getDeclaredMethod(methodName, params);
            if (method == null) {
                return;
            }

            method.invoke(object, joinPoint, result);
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    public static void sendTrackEventToSDK2(final JoinPoint joinPoint, final String methodName, int menuItemIndex) {
        try {
            if (joinPoint == null || TextUtils.isEmpty(methodName)) {
                return;
            }

            if (clazz == null) {
                clazz = Class.forName("com.thinking.analyselibrary.ThinkingDataRuntimeBridge");
            }
            if (clazz == null) {
                return;
            }

            if (object == null) {
                object = clazz.newInstance();
            }
            if (object == null) {
                return;
            }

            Class[] params = new Class[2];
            params[0] = JoinPoint.class;
            params[1] = Integer.class;

            Method method = clazz.getDeclaredMethod(methodName, params);
            if (method == null) {
                return;
            }

            method.invoke(object, joinPoint, menuItemIndex);
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    public static void sendTrackEventToSDK(final JoinPoint joinPoint, final String methodName) {
        try {
            if (joinPoint == null || TextUtils.isEmpty(methodName)) {
                return;
            }

            if (clazz == null) {
                clazz = Class.forName("com.thinking.analyselibrary.ThinkingDataRuntimeBridge");
            }
            if (clazz == null) {
                return;
            }

            if (object == null) {
                object = clazz.newInstance();
            }
            if (object == null) {
                return;
            }

            Class[] params = new Class[1];
            params[0] = JoinPoint.class;

            Method method = clazz.getDeclaredMethod(methodName, params);
            if (method == null) {
                return;
            }

            method.invoke(object, joinPoint);
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }


}

