package com.thinking.analyselibrary.utils;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.thinking.analyselibrary.TDConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class TDUtil {
    private static final String TAG = "ThinkingAnalyticsSDK.TDUtil";

    public static String getActivityTitle(Activity activity) {
        try {
            if (activity != null) {
                try {
                    String activityTitle = null;
                    if (!TextUtils.isEmpty(activity.getTitle())) {
                        activityTitle = activity.getTitle().toString();
                    }

                    if (Build.VERSION.SDK_INT >= 11) {
                        String toolbarTitle = TDUtil.getToolbarTitle(activity);
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

    @TargetApi(11)
    public static String getToolbarTitle(Activity activity) {
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            if (!TextUtils.isEmpty(actionBar.getTitle())) {
                return actionBar.getTitle().toString();
            }
        } else {
            try {
                Class<?> appCompatActivityClass = null;
                try {
                    appCompatActivityClass = Class.forName("android.support.v7.app.AppCompatActivity");
                } catch (Exception e) {
                    //ignored
                }
                if (appCompatActivityClass == null) {
                    try {
                        appCompatActivityClass = Class.forName("androidx.appcompat.app.AppCompatActivity");
                    } catch (Exception e) {
                        //ignored
                    }
                }
                if (appCompatActivityClass != null && appCompatActivityClass.isInstance(activity)) {
                    Method method = activity.getClass().getMethod("getSupportActionBar");
                    if (method != null) {
                        Object supportActionBar = method.invoke(activity);
                        if (supportActionBar != null) {
                            method = supportActionBar.getClass().getMethod("getTitle");
                            if (method != null) {
                                CharSequence charSequence = (CharSequence) method.invoke(supportActionBar);
                                if (charSequence != null) {
                                    return charSequence.toString();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                //ignored
            }
        }
        return null;
    }

    public static void mergeJSONObject(final JSONObject source, JSONObject dest)
            throws JSONException {
        Iterator<String> sourceIterator = source.keys();
        while (sourceIterator.hasNext()) {
            String key = sourceIterator.next();
            Object value = source.get(key);
            dest.put(key, value);
        }
    }

    public static void getScreenNameAndTitleFromActivity(JSONObject properties, Activity activity) {
        if (activity == null || properties == null) {
            return;
        }

        try {
            properties.put("#screen_name", activity.getClass().getCanonicalName());

            String activityTitle = activity.getTitle().toString();

            if (Build.VERSION.SDK_INT >= 11) {
                String toolbarTitle = getToolbarTitle(activity);
                if (!TextUtils.isEmpty(toolbarTitle)) {
                    activityTitle = toolbarTitle;
                }
            }

            if (TextUtils.isEmpty(activityTitle)) {
                PackageManager packageManager = activity.getPackageManager();
                if (packageManager != null) {
                    ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                    if (activityInfo != null) {
                        activityTitle = activityInfo.loadLabel(packageManager).toString();
                    }
                }
            }
            if (!TextUtils.isEmpty(activityTitle)) {
                properties.put("#title", activityTitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentProcessName(Context context) {

        try {
            int pid = android.os.Process.myPid();

            ActivityManager activityManager = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);


            if (activityManager == null) {
                return null;
            }

            List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList = activityManager.getRunningAppProcesses();
            if (runningAppProcessInfoList != null) {
                for (ActivityManager.RunningAppProcessInfo appProcess : runningAppProcessInfoList) {

                    if (appProcess != null) {
                        if (appProcess.pid == pid) {
                            return appProcess.processName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public static boolean isMainProcess(Context context, String mainProcessName) {
        if (TextUtils.isEmpty(mainProcessName)) {
            return true;
        }

        String currentProcess = getCurrentProcessName(context.getApplicationContext());
        if (TextUtils.isEmpty(currentProcess) || mainProcessName.equals(currentProcess)) {
            return true;
        }

        return false;
    }

    public static String networkType(Context context) {
        if (!checkHasPermission(context, "android.permission.ACCESS_NETWORK_STATE")) {
            return "NULL";
        }

        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            NetworkInfo networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                return "WIFI";
            }
        }

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context
                .TELEPHONY_SERVICE);

        int networkType = telephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
        }

        return "NULL";
    }

    public static boolean checkHasPermission(Context context, String permission) {
        try {
            Class<?> contextCompat = null;
            try {
                contextCompat = Class.forName("android.support.v4.content.ContextCompat");
            } catch (Exception e) {
                //ignored
            }
            if (contextCompat == null) {
                try {
                    contextCompat = Class.forName("androidx.core.content.ContextCompat");
                } catch (Exception e) {
                    //ignored
                }
            }

            if (contextCompat == null) {
                return true;
            }

            Method checkSelfPermissionMethod = contextCompat.getMethod("checkSelfPermission", new Class[]{Context.class, String.class});
            int result = (int)checkSelfPermissionMethod.invoke(null, new Object[]{context, permission});
            if (result != PackageManager.PERMISSION_GRANTED) {
                TDLog.i(TAG, "You can fix this by adding the following to your AndroidManifest.xml file:\n"
                        + "<uses-permission android:name=\"" + permission + "\" />");
                return false;
            }

            return true;
        } catch (Exception e) {
            TDLog.i(TAG, e.toString());
            return true;
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        if (!checkHasPermission(context, "android.permission.ACCESS_NETWORK_STATE")) {
            return false;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkNull(Object value) {
        if (value == null) {
            return true;
        }
        return false;
    }

    public static Map<String, Object> getDeviceInfo(Context mContext)
    {
        final Map<String, Object> deviceInfo = new HashMap<String, Object>();
        {
            deviceInfo.put("#lib", "Android");
            deviceInfo.put("#lib_version", TDConfig.VERSION);
            deviceInfo.put("#os", "Android");
            deviceInfo.put("#os_version",
                    Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            deviceInfo
                    .put("#manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
            deviceInfo.put("#device_model", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);

            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            deviceInfo.put("#screen_height", displayMetrics.heightPixels);
            deviceInfo.put("#screen_width", displayMetrics.widthPixels);

            String operatorString = getCarrier(mContext);
            if (!TextUtils.isEmpty(operatorString)) {
                deviceInfo.put("#carrier", operatorString);
            } else {
                deviceInfo.put("#carrier", "未知");
            }
            String androidID = getAndroidID(mContext);
            if (!TextUtils.isEmpty(androidID)) {
                deviceInfo.put("#device_id", androidID);
            }
        }
        return deviceInfo;
    }

    // 获取运营商信息
    static private String getCarrier(Context context) {
        final Map<String, String> carrierMap = new HashMap<String, String>() {
            {
                put("46000", "中国移动");
                put("46002", "中国移动");
                put("46007", "中国移动");
                put("46008", "中国移动");

                put("46001", "中国联通");
                put("46006", "中国联通");
                put("46009", "中国联通");

                put("46003", "中国电信");
                put("46005", "中国电信");
                put("46011", "中国电信");

                put("46004", "中国卫通");

                put("46020", "中国铁通");

            }
        };

        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String simOperator = tm.getSimOperator();
            if (!TextUtils.isEmpty(simOperator) && carrierMap.containsKey(simOperator)) {
                return carrierMap.get(simOperator);
            }

            String simOperatorName = tm.getSimOperatorName();
            if (!TextUtils.isEmpty(simOperatorName)) {
                return simOperatorName;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    static String getAndroidID(Context mContext) {
        String androidID = "";
        try {
            androidID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return androidID;
    }

    public static String getVersionName(Context mContext) {
        String version = null;
        try {
            final PackageManager manager = mContext.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            version = info.versionName;
        } catch (final Exception e) {
            TDLog.d(TAG, "Exception getting app version");
        }
        return version;
    }
}
