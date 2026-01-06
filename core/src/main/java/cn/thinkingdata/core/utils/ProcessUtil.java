package cn.thinkingdata.core.utils;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Cache the current process name
 */
public class ProcessUtil {
    private static String currentProcessName = "";

    public static List<ActivityManager.RunningAppProcessInfo> runningAppList;

    /**
     * get current process name
     * @param context context
     * @return current process name
     */
    public static String getCurrentProcessName(Context context) {
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName;
        }

        //1)Gets the current process name through the Application's API
        currentProcessName = getCurrentProcessNameByApplication();
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName;
        }

        //2)Get the current process name by reflecting ActivityThread
        currentProcessName = getCurrentProcessNameByActivityThread();
        if (!TextUtils.isEmpty(currentProcessName)) {
            return currentProcessName;
        }

        //3)Get the current process name from ActivityManager
        currentProcessName = getCurrentProcessNameByActivityManager(context);
        return currentProcessName;
    }


    /**
     * Using the new API of Application to get the process name, no reflection, no IPC, the highest efficiency.
     */
    private static String getCurrentProcessNameByApplication() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        }
        return "";
    }

    /**
     * Get the current process name by reflecting ActivityThread,avoid ipc
     */
    private static String getCurrentProcessNameByActivityThread() {
        String processName = "";
        try {
            final Method declaredMethod = Class.forName("android.app.ActivityThread", false, Application.class.getClassLoader())
                    .getDeclaredMethod("currentProcessName", (Class<?>[]) new Class[0]);
            declaredMethod.setAccessible(true);
            final Object invoke = declaredMethod.invoke(null, new Object[0]);
            if (invoke instanceof String) {
                processName = (String) invoke;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return processName;
    }

    /**
     *  Getting the process name through ActivityManager requires IPC communication
     */
    private static String getCurrentProcessNameByActivityManager(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            if (null == runningAppList) {
                runningAppList = am.getRunningAppProcesses();
            }
            if (runningAppList != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningAppList) {
                    if (processInfo.pid == pid) {
                        return processInfo.processName;
                    }
                }
            }
        }
        return "";
    }
}