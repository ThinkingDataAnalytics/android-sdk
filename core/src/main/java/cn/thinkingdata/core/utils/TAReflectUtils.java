/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.core.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import cn.thinkingdata.core.utils.TDLog;

/**
 * Reflection tool class
 *
 * @author bugliee
 * @since 2022/5/16
 */
public class TAReflectUtils {
    static String TAG = "ThinkingAnalytics.TAReflectUtils";

    /**
     * get object class Constructor
     *
     * @author bugliee
     * @since  2022/5/16
     * @param className class name
     * @return {@link Object}
     */
    public static Object createObject(String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clazz;
    }

    /**
     * getInstance
     *
     * @author bugliee
     * @since 2022/5/16
     * @param className class name
     * @return {@link Object}
     */
    public static Object getObjectInstance(String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
            return (clazz.getMethod("getInstance").invoke(clazz));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clazz;
    }

    /**
     * call Getter method
     *
     * @author bugliee
     * @since  2022/5/16
     * @param obj obj
     * @param propertyName field name
     * @return {@link Object}
     */
    public static Object invokeGetterMethod(Object obj, String propertyName) {
        String getterMethodName = "get" + propertyName.trim();
        return invokeMethod(obj, getterMethodName, new Object[]{});
    }

    /**
     * call Setter method
     *
     * @author bugliee
     * @since 2022/5/16
     * @param obj obj
     * @param propertyName method name
     * @param value value
     */
    public static void invokeSetterMethod(Object obj, String propertyName, Object value) {
        invokeSetterMethod(obj, propertyName, value, null);
    }

    /**
     * call Setter method
     *
     * @author bugliee
     * @since 2022/5/16
     * @param obj obj
     * @param propertyName method name
     * @param value value
     * @param propertyType property type
     */
    public static void invokeSetterMethod(Object obj, String propertyName, Object value, Class<?> propertyType) {
        Class<?> type = propertyType != null ? propertyType : value.getClass();
        String setterMethodName = "set" + propertyName.trim();
        invokeMethod(obj, setterMethodName, new Object[]{value}, type);
    }

    /**
     * Read object property values directly
     *
     * @author bugliee
     * @since 2022/5/16
     * @param obj obj
     * @param fieldName field name
     * @return {@link Object}
     */
    public static Object getFieldValue(final Object obj, final String fieldName) {
        Field field = getAccessibleField(obj, fieldName);

        if (field == null) {
            throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + obj + "]");
        }

        Object result = null;
        try {
            result = field.get(obj);
        } catch (IllegalAccessException e) {
            TDLog.e(TAG, e.getMessage());
        }
        return result;
    }

    /**
     * Set the object property value directly
     *
     * @author bugliee
     * @since 2022/5/16
     * @param obj obj
     * @param fieldName field name
     * @param value value
     */
    public static void setFieldValue(final Object obj, final String fieldName, final Object value) {
        Field field = getAccessibleField(obj, fieldName);

        if (field == null) {
            throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + obj + "]");
        }

        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            TDLog.e(TAG, e.getMessage());
        }
    }

    /**
     * Loop up and get the DeclaredField of the object
     *
     * @author bugliee
     * @since 2022/5/16
     * @param obj obj
     * @param fieldName field name
     * @return {@link Field}
     */
    public static Field getAccessibleField(final Object obj, final String fieldName) {
        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                if (superClass != null) {
                    Field field = superClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     *
     * @param obj obj
     * @param methodName method name
     * @param parameterTypes method parameter types
     * @param args method parameter
     * @return {@link Object}
     * @author bugliee
     * @since  2022/5/16
     */
    public static Object invokeMethod(final Object obj, final String methodName, final Object[] args, final Class<?>... parameterTypes) {
        Method method = getAccessibleMethod(obj, methodName, parameterTypes);
        if (method == null) {
            TDLog.i(TAG, "Could not find method [" + methodName + "] on target [" + obj + "]");
            return null;
        }
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            TDLog.e(TAG, e.getMessage());
            return null;
        }
    }

    /**
     *
     * @author bugliee
     * @since 2022/5/16
     * @param obj obj
     * @param methodName method name
     * @param parameterTypes method parameter types
     * @return {@link Method}
     */
    public static Method getAccessibleMethod(final Object obj, final String methodName,
                                             final Class<?>... parameterTypes) {
        if (obj == null) {
            TDLog.i(TAG, "obj is null!");
            return null;
        }

        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
            try {
                Method method = superClass.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;

            } catch (NoSuchMethodException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Gets the parent generic parameter type
     * @param clazz class
     * @param <T> T
     * @return T
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Class<T> getSuperClassGenericType(final Class clazz) {
        return getSuperClassGenericType(clazz, 0);
    }

    /**
     * description
     *
     * @author bugliee
     * @since 2022/5/16
     * @param clazz class
     * @param index index
     * @return {@link Class}
     */
    @SuppressWarnings("rawtypes")
    public static Class getSuperClassGenericType(final Class clazz, final int index) {
        Type genType = clazz.getGenericSuperclass();

        if (!(genType instanceof ParameterizedType)) {
            TDLog.w(TAG, clazz.getSimpleName() + "'s superclass not ParameterizedType");
            return Object.class;
        }

        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            TDLog.w(TAG, "Index: " + index + ", Size of " + clazz.getSimpleName() + "'s Parameterized Type: "
                    + params.length);
            return Object.class;
        }
        if (!(params[index] instanceof Class)) {
            TDLog.w(TAG, clazz.getSimpleName() + " not set the actual class on superclass generic parameter");
            return Object.class;
        }

        return (Class) params[index];
    }

    /**
     * Reflection calls static methods
     * @param className class name
     * @param methodName method name
     * @param args method parameter
     * @param parameterTypes method parameter types
     * @throws Exception java exception
     */
    public static void invokeStaticMethod(String className, String methodName, final Object[] args, final Class<?>... parameterTypes) throws Exception {
        Class<?> clazz = Class.forName(className);
        Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.invoke(null, args);
    }
}
