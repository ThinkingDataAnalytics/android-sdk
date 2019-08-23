package cn.thinkingdata.android.runtime;

import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AopUtils {
    private final static String RUNTIME_BRIDGE_CLASS = "cn.thinkingdata.android.ThinkingDataRuntimeBridge";
    private static Class clazz;
    private static Object object;

    public static void trackViewClickEvent(JoinPoint joinPoint, View view) {
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            if (null == methodSignature) return;

            Method method = methodSignature.getMethod();
            if (null == method) return;

            Annotation annotation = null;

            Class clazz = null;
            try {
                clazz = Class.forName("cn.thinkingdata.android.ThinkingDataIgnoreTrackOnClick");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (null != clazz) {
                annotation = method.getAnnotation(clazz);
            }
            sendTrackEventToSDK("onViewOnClick", view, annotation);
        } catch (Exception e) {
            // ignore the exception
        }
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
            // ignore the exception
            e.printStackTrace();
        }

    }
}

