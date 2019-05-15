package com.thinking.analyselibrary.runtime;

import android.text.TextUtils;
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

            if (null == clazz) {
                clazz = Class.forName(RUNTIME_BRIDGE_CLASS);
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

}

