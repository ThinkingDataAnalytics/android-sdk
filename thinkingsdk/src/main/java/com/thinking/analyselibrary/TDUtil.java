package com.thinking.analyselibrary;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.thinking.analyselibrary.ThinkingAnalyticsSDK.VERSION;

public class TDUtil {
    private static final String TAG = "TD.TDUtil";

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
        }
        return null;
    }

    public static void mergeJSONObject(final JSONObject source, JSONObject dest)
            throws JSONException {
        Iterator<String> superPropertiesIterator = source.keys();
        while (superPropertiesIterator.hasNext()) {
            String key = superPropertiesIterator.next();
            Object value = source.get(key);
            if (value instanceof Date) {
                synchronized (mDateFormat) {
                    dest.put(key, mDateFormat.format((Date) value));
                }
            } else {
                dest.put(key, value);
            }
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
            final PackageManager packageManager = context.getPackageManager();
            final String packageName = context.getPackageName();

            if (packageManager == null || packageName == null) {
                TDLog.d(TAG, "Can't check configuration when using a Context with null packageManager or packageName");
                return false;
            }

            if (PackageManager.PERMISSION_GRANTED != packageManager
                    .checkPermission(permission, packageName)) {
                TDLog.d(TAG, "You can fix this by adding the following to your AndroidManifest.xml file:\n"
                        + "<uses-permission android:name=\"" + permission+ "\" />");
                return false;
            }

            return true;
        } catch (Exception e) {
            TDLog.d(TAG, e.toString());
            e.printStackTrace();
            return false;
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

    public static String operatorToCarrier(String operator) {

        final Map<String, String> sCarrierMap = new HashMap<String, String>() {
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
            }
        };

        String other = "其他";
        if (TextUtils.isEmpty(operator)) {
            return other;
        }

        for (Map.Entry<String, String> entry : sCarrierMap.entrySet()) {
            if (operator.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return other;
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
            deviceInfo.put("#lib_version", VERSION);
            deviceInfo.put("#os", "Android");
            deviceInfo.put("#os_version",
                    Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            deviceInfo
                    .put("#manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
            deviceInfo.put("#device_model", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);

            String appversion = getVersionName(mContext);
            if(appversion != null) {
                deviceInfo.put("#app_version", appversion);
            }

            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            deviceInfo.put("#screen_height", displayMetrics.heightPixels);
            deviceInfo.put("#screen_width", displayMetrics.widthPixels);

            if (TDUtil.checkHasPermission(mContext, "android.permission.READ_PHONE_STATE")) {
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context
                            .TELEPHONY_SERVICE);
                    String operatorString = telephonyManager.getSubscriberId();

                    if (!TextUtils.isEmpty(operatorString)) {
                        deviceInfo.put("#carrier", TDUtil.operatorToCarrier(operatorString));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String androidID = getAndroidID(mContext);
            if (!TextUtils.isEmpty(androidID)) {
                deviceInfo.put("#device_id", androidID);
            }
        }
        return deviceInfo;
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

    static String getVersionName(Context mContext) {
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

    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"
            + ".SSS",Locale.CHINA);
}
