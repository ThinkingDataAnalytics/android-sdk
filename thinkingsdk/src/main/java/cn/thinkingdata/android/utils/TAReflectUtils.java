/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * < 反射工具类 >.
 *
 * @author bugliee
 * @create 2022/5/16
 * @since 1.0.0
 */
public class TAReflectUtils {
    static String TAG = "ThinkingAnalytics.TAReflectUtils";

    /**
     * < 获取类对象 Constructor >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param className 完整类名
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
     * < 获取类实例 getInstance >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param className 完整类名
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
     * < 调用Getter方法 >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param obj 对象
     * @param propertyName 属性名
     * @return {@link Object}
     */
    public static Object invokeGetterMethod(Object obj, String propertyName) {
        String getterMethodName = "get" + propertyName.trim();
        return invokeMethod(obj, getterMethodName, new Object[]{});
    }

    /**
     * < 调用Setter方法.使用value的Class来查找Setter方法 >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param obj 对象
     * @param propertyName 属性名
     * @param value 传入值
     */
    public static void invokeSetterMethod(Object obj, String propertyName, Object value) {
        invokeSetterMethod(obj, propertyName, value, null);
    }

    /**
     * < 调用Setter方法 >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param obj 对象
     * @param propertyName 属性名
     * @param value 传入值
     * @param propertyType setter的参数类型，为空默认使用value的类型
     */
    public static void invokeSetterMethod(Object obj, String propertyName, Object value, Class<?> propertyType) {
        Class<?> type = propertyType != null ? propertyType : value.getClass();
        String setterMethodName = "set" + propertyName.trim();
        invokeMethod(obj, setterMethodName, new Object[]{value}, type);
    }

    /**
     * < 直接读取对象属性值 >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param obj 对象
     * @param fieldName 属性名
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
     * < 直接设置对象属性值 >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param obj 对象
     * @param fieldName 属性名
     * @param value 传入值
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
     * < 循环向上转型, 获取对象的DeclaredField >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param obj 对象
     * @param fieldName 属性名
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
     * < 直接调用对象方法 >.
     *
     * @param obj            对象
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @param args           参数列表
     * @return {@link Object}
     * @author bugliee
     * @create 2022/5/16
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
     * < 循环向上转型, 获取对象的DeclaredMethod >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param obj 对象
     * @param methodName 方法名
     * @param parameterTypes 参数类型
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
     * < 获取父类泛型参数类型 >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param clazz 类
     * @return {@link Class}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Class<T> getSuperClassGenericType(final Class clazz) {
        return getSuperClassGenericType(clazz, 0);
    }

    /**
     * < description >.
     *
     * @author bugliee
     * @create 2022/5/16
     * @param clazz 类
     * @param index 索引，0..
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
     * 反射调用静态方法
     * @param className
     * @param methodName
     * @param args
     * @param parameterTypes
     */
    public static void invokeStaticMethod(String className, String methodName, final Object[] args, final Class<?>... parameterTypes) throws Exception {
        Class<?> clazz = Class.forName(className);
        Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.invoke(null, args);
    }
}
