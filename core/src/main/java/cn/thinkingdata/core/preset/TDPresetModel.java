/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.preset;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.LocaleList;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.thinkingdata.core.receiver.TDNetWorkObservable;
import cn.thinkingdata.core.receiver.TDNetWorkObserver;
import cn.thinkingdata.core.utils.EmulatorDetector;
import cn.thinkingdata.core.utils.TAReflectUtils;
import cn.thinkingdata.core.utils.TDLog;

/**
 * @author liulongbing
 * @create 2024/3/4
 */
public class TDPresetModel implements TDNetWorkObserver {

    private static final String TAG = "ThinkingAnalytics.PresetProperties";
    private volatile static TDPresetModel instance = null;
    private final Map<String, Object> mPresetProperties = new HashMap<>();
    private String mAppVersionName;
    private String currentNetworkType;
    private boolean isNetWorkChanged = false;
    private final Context mContext;
    private final boolean mHasPermission;
    private final PresetStoragePlugin presetStoragePlugin;
    private volatile String mDeviceId = null;
    List<String> disableList = new ArrayList<>();

    private TDPresetModel(Context context) {
        mContext = context.getApplicationContext();
        mHasPermission = checkHasPermission(mContext, "android.permission.ACCESS_NETWORK_STATE");
        presetStoragePlugin = new PresetStoragePlugin(mContext);
        initPresetProperties(context);
        TDNetWorkObservable.getInstance(mContext).addNetWorkObserver(this);
    }

    public static TDPresetModel getInstance(Context context) {
        if (instance == null) {
            synchronized (TDPresetModel.class) {
                if (instance == null) {
                    instance = new TDPresetModel(context);
                }
            }
        }
        return instance;
    }

    public Map<String, Object> getPresetProperties() {
        return mPresetProperties;
    }

    private void initPresetProperties(Context context) {
        try {
            try {
                Resources resources = context.getResources();
                String[] array = resources.getStringArray(resources.getIdentifier("TDDisPresetProperties", "array", context.getPackageName()));
                disableList.addAll(Arrays.asList(array));
            } catch (Exception ignored) {
            }
            String osVersion = TDPresetUtils.getHarmonyOSVersion();
            if (!disableList.contains(TDPresetUtils.KEY_OS)) {
                if (TextUtils.isEmpty(osVersion)) {
                    mPresetProperties.put(TDPresetUtils.KEY_OS, "Android");
                } else {
                    mPresetProperties.put(TDPresetUtils.KEY_OS, "HarmonyOS");
                }
            }
            if (!disableList.contains(TDPresetUtils.KEY_OS_VERSION)) {
                if (TextUtils.isEmpty(osVersion)) {
                    mPresetProperties.put(TDPresetUtils.KEY_OS_VERSION, Build.VERSION.RELEASE);
                } else {
                    mPresetProperties.put(TDPresetUtils.KEY_OS_VERSION, osVersion);
                }
            }
            if (!disableList.contains(TDPresetUtils.KEY_BUNDLE_ID)) {
                mPresetProperties.put(TDPresetUtils.KEY_BUNDLE_ID, TDPresetUtils.getCurrentProcessName(context));
            }
            if (!disableList.contains(TDPresetUtils.KEY_MANUFACTURER)) {
                mPresetProperties.put(TDPresetUtils.KEY_MANUFACTURER, Build.MANUFACTURER);
            }
            if (!disableList.contains(TDPresetUtils.KEY_DEVICE_MODEL)) {
                mPresetProperties.put(TDPresetUtils.KEY_DEVICE_MODEL, Build.MODEL);
            }
            int[] size = getDeviceSize(context);
            if (!disableList.contains(TDPresetUtils.KEY_SCREEN_WIDTH)) {
                mPresetProperties.put(TDPresetUtils.KEY_SCREEN_WIDTH, size[0]);
            }
            if (!disableList.contains(TDPresetUtils.KEY_SCREEN_HEIGHT)) {
                mPresetProperties.put(TDPresetUtils.KEY_SCREEN_HEIGHT, size[1]);
            }
            if (!disableList.contains(TDPresetUtils.KEY_CARRIER)) {
                String operatorString = getCarrier(context);
                mPresetProperties.put(TDPresetUtils.KEY_CARRIER, operatorString);
            }
            if (!disableList.contains(TDPresetUtils.KEY_SYSTEM_LANGUAGE)) {
                String systemLanguage = getSystemLanguage();
                mPresetProperties.put(TDPresetUtils.KEY_SYSTEM_LANGUAGE, systemLanguage);
            }
            if (!disableList.contains(TDPresetUtils.KEY_APP_VERSION)) {
                final PackageManager manager = context.getPackageManager();
                PackageInfo packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
                mAppVersionName = packageInfo.versionName;
                if (!TextUtils.isEmpty(mAppVersionName)) {
                    mPresetProperties.put(TDPresetUtils.KEY_APP_VERSION, mAppVersionName);
                }
            }
            if (!disableList.contains(TDPresetUtils.KEY_SIMULATOR)) {
                mPresetProperties.put(TDPresetUtils.KEY_SIMULATOR, EmulatorDetector.isEmulator());
            }
            if (!disableList.contains(TDPresetUtils.KEY_DEVICE_TYPE)) {
                mPresetProperties.put(TDPresetUtils.KEY_DEVICE_TYPE, TDPresetUtils.getDeviceType(context));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getAppVersionName() {
        return mAppVersionName;
    }

    @Override
    public void onAvailable(Network network) {
        currentNetworkType = getNetworkType();
        isNetWorkChanged = true;
    }

    @Override
    public void onLost(Network network) {
        currentNetworkType = "NULL";
    }

    @Override
    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {

    }

    @Override
    public void onChange() {
        currentNetworkType = getNetworkType();
        isNetWorkChanged = true;
    }

    public String getCurrentNetworkType() {
        if ((isNetWorkChanged && "NULL".equals(currentNetworkType)) || null == currentNetworkType) {
            currentNetworkType = getNetworkType();
            if (!"NULL".equals(currentNetworkType)) {
                isNetWorkChanged = true;
            }
        }
        return currentNetworkType;
    }

    @SuppressLint("HardwareIds")
    private String getDeviceIDInternal() {
        String androidID = presetStoragePlugin.get(PresetStoragePlugin.DEVICE_ID, "");
        if (TextUtils.isEmpty(androidID)) {
            Object clazz = TAReflectUtils
                    .createObject("cn.thinkingdata.core.utils.TASensitiveInfo");
            Object result = TAReflectUtils
                    .invokeMethod(clazz, "getAndroidID", new Object[]{mContext}, Context.class);
            androidID = result == null ? "" : String.valueOf(result);
            if (TextUtils.isEmpty(androidID)) {
                androidID = TDPresetUtils.getRandomHEXValue(16);
            }
            try {
                if (Integer.parseInt(androidID) == 0) {
                    androidID = TDPresetUtils.getRandomHEXValue(16);
                }
            } catch (Exception e) {
                //ignore
            }
            //randomDeviceID.put(androidID);
            presetStoragePlugin.save(PresetStoragePlugin.DEVICE_ID, androidID);
        }
        return androidID;
    }

    public String getDeviceId() {
        if (mDeviceId == null && !disableList.contains(TDPresetUtils.KEY_DEVICE_ID)) {
            synchronized (this) {
                if (mDeviceId == null) {
                    mDeviceId = getDeviceIDInternal();
                }
            }
        }
        return mDeviceId;
    }

    private String getNetworkType() {
        try {
            if (!mHasPermission) {
                return "NULL";
            }
            // Wifi
            ConnectivityManager manager = ( ConnectivityManager )
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = null;
            if (manager != null) {
                networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                    return "WIFI";
                }
            }
            TelephonyManager telephonyManager = ( TelephonyManager ) mContext.getSystemService(Context
                    .TELEPHONY_SERVICE);
            return mobileNetworkType(mContext, telephonyManager, manager);
        } catch (Exception e) {
            return "NULL";
        }
    }

    @SuppressLint("MissingPermission")
    private String mobileNetworkType(Context context, TelephonyManager telephonyManager,
                                     ConnectivityManager connectivityManager) {
        // Mobile network
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (telephonyManager != null) {
            try {
                if (checkHasPermission(context, Manifest.permission.READ_PHONE_STATE) &&
                        telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY &&
                        isMobileNetworkConnected(context)){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        networkType = telephonyManager.getDataNetworkType();
                    } else {
                        networkType = telephonyManager.getNetworkType();
                    }
                }
            } catch (Exception ignored) {
                //ignored
            }
        }

        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN && isMobileNetworkConnected(context)) {
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    networkType = networkInfo.getSubtype();
                }
            }
        }

        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_GSM:
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
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
            case 19:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            default:
                return "NULL";
        }
    }

    public static boolean isMobileNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                    return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private int[] getDeviceSize(Context context) {
        int[] size = new int[2];
        try {
            int screenWidth;
            int screenHeight;
            WindowManager windowManager
                    = ( WindowManager ) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            int rotation = display.getRotation();
            Point point = new Point();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealSize(point);
                screenWidth = point.x;
                screenHeight = point.y;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                display.getSize(point);
                screenWidth = point.x;
                screenHeight = point.y;
            } else {
                screenWidth = display.getWidth();
                screenHeight = display.getHeight();
            }
            size[0] = getNaturalWidth(rotation, screenWidth, screenHeight);
            size[1] = getNaturalHeight(rotation, screenWidth, screenHeight);
        } catch (Exception e) {
            //context.getResources().getDisplayMetrics() not include the bottom virtual navigation bar
            if (context.getResources() != null) {
                final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                size[0] = displayMetrics.widthPixels;
                size[1] = displayMetrics.heightPixels;
            }
        }
        return size;
    }

    /**
     * According to the device rotation, determine the direction of the screen and obtain the natural direction width.
     *
     * @param rotation
     * @param width
     * @param height
     * @return Natural size
     */
    private int getNaturalWidth(int rotation, int width, int height) {
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180 ? width : height;
    }

    /**
     * According to the device rotation, determine the direction of the screen and obtain the natural direction high.
     *
     * @param rotation
     * @param width
     * @param height
     * @return Natural size
     */
    private int getNaturalHeight(int rotation, int width, int height) {
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180 ? height : width;
    }

    private String getCarrier(Context context) {
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
            TelephonyManager tm
                    = ( TelephonyManager ) context.getSystemService(Context.TELEPHONY_SERVICE);
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
        return "";

    }

    private String getSystemLanguage() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = LocaleList.getDefault().get(0);
        } else {
            locale = Locale.getDefault();
        }

        return locale.getLanguage();
    }

    private boolean checkHasPermission(Context context, String permission) {
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
                return false;
            }

            Method checkSelfPermissionMethod = contextCompat.getMethod("checkSelfPermission",
                    new Class[]{Context.class, String.class});
            int result = ( int ) checkSelfPermissionMethod.invoke(null,
                    new Object[]{context, permission});
            if (result != PackageManager.PERMISSION_GRANTED) {
                TDLog.w(TAG, "You can fix this by adding the following "
                        + "to your AndroidManifest.xml file:\n"
                        + "<uses-permission android:name=\"" + permission + "\" />");
                return false;
            }

            return true;
        } catch (Exception e) {
            TDLog.w(TAG, e.toString());
            return false;
        }
    }

}
