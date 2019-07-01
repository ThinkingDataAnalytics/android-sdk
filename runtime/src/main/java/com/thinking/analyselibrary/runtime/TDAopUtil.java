package com.thinking.analyselibrary.runtime;

import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import org.aspectj.lang.JoinPoint;
import java.lang.reflect.Method;

public class TDAopUtil {
    private final static String RUNTIME_BRIDGE_CLASS = "com.thinking.analyselibrary.ThinkingDataRuntimeBridge";
    private static Class clazz;
    private static Object object;

    public static void sendTrackEventToSDK(final JoinPoint joinPoint, final String methodName, Object result) {
        try {
            if (joinPoint == null || TextUtils.isEmpty(methodName)) {
                return;
            }

            if (null == clazz) clazz = Class.forName(RUNTIME_BRIDGE_CLASS);
            if (clazz == null) {
                return;
            }

            if (object == null) {
                object = clazz.newInstance();
            }
            if (object == null) {
                return;
            }

            int paramLength = (null == result) ? 1 : 2;
            Class[] params = new Class[paramLength];

            params[0] = JoinPoint.class;
            if (null != result) {
                params[1] = result instanceof Integer ? Integer.class : Object.class;
            }

            Method method = clazz.getDeclaredMethod(methodName, params);
            if (method == null) {
                return;
            }

            if (null == result) {
                method.invoke(object, joinPoint);
            } else {
                method.invoke(object, joinPoint, result);
            }
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    public static void sendTrackEventToSDK(final JoinPoint joinPoint, final String methodName) {
        sendTrackEventToSDK(joinPoint, methodName, null);
    }

    public static void sendTrackEventToSDK(final String methodName, Object... args) {
        if (TextUtils.isEmpty(methodName)) {
            return;
        }

        try {
            if (null == clazz) {
                clazz = Class.forName(RUNTIME_BRIDGE_CLASS);
            }
            if (clazz == null) {
                return;
            }

            if (null == object) {
                object = clazz.newInstance();
            }

            if (object == null) {
                return;
            }
            Class[] params = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                params[i] = args[i] instanceof Integer ? int.class
                        : args[i] instanceof Boolean ? boolean.class
                        : args[i] instanceof String ? String.class
                        : args[i] instanceof MenuItem ? MenuItem.class
                        : args[i] instanceof View ? View.class : Object.class;
            }

            Method method = clazz.getDeclaredMethod(methodName, params);
            if (method == null) {
                return;
            }
            method.invoke(object, args);

        } catch(Exception e) {
            // ignore
            e.printStackTrace();
        }

    }
}

