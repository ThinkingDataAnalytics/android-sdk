package cn.thinkingdata.android;

import static android.content.Context.ACTIVITY_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.LocaleList;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import org.json.JSONObject;

import cn.thinkingdata.android.persistence.StorageRandomDeviceID;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;
import cn.thinkingdata.android.utils.TDTime;
import cn.thinkingdata.android.utils.TDUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

class SystemInformation {

    private static final int INTERNAL_STORAGE = 0;
    private static final int EXTERNAL_STORAGE = 1;
    private static String sLibName = "Android";
    private static String sLibVersion = TDConfig.VERSION;
    private static SystemInformation sInstance;
    private final static Object sInstanceLock = new Object();
    private boolean hasNotUpdated;
    private PackageInfo packageInfo;
    private TimeZone    mTimeZone;
    private final static  String TAG = "ThinkingAnalytics.SystemInformation";
    private String mAppVersionName;
    private Map<String, Object> mDeviceInfo;
    private final Context mContext;
    private final boolean mHasPermission;

    static void setLibraryInfo(String libName, String libVersion) {
        if (!TextUtils.isEmpty(libName)) {
            sLibName = libName;
            TDLog.d(TAG, "#lib has been changed to: " + libName);
        }

        if (!TextUtils.isEmpty(libVersion)) {
            sLibVersion = libVersion;
            TDLog.d(TAG, "#lib_version has been changed to: " + libVersion);
        }
    }

    static String getLibName() {
        return sLibName;
    }

    static String getLibVersion() {
        return sLibVersion;
    }

   public static SystemInformation getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (null == sInstance) {
                sInstance = new SystemInformation(context);
            }
            return sInstance;
        }
    }
    public static SystemInformation getInstance(Context context,TimeZone timeZone) {
        synchronized (sInstanceLock) {
            if (null == sInstance) {
                sInstance = new SystemInformation(context,timeZone);
            }
            return sInstance;
        }
    }

    public boolean hasNotBeenUpdatedSinceInstall() {
        return hasNotUpdated;
    }
    private SystemInformation(Context context,TimeZone timeZone)
    {
        this(context);
        mTimeZone = timeZone;
        mDeviceInfo = setupDeviceInfo(context);
    }
    private SystemInformation(Context context) {
        mContext = context.getApplicationContext();
        mHasPermission = checkHasPermission(mContext, "android.permission.ACCESS_NETWORK_STATE");
        try {
            final PackageManager manager = context.getPackageManager();
            packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
            mAppVersionName = packageInfo.versionName;
            hasNotUpdated = packageInfo.firstInstallTime == packageInfo.lastUpdateTime;
            TDLog.d(TAG, "First Install Time: " + packageInfo.firstInstallTime);
            TDLog.d(TAG, "Last Update Time: " + packageInfo.lastUpdateTime);
        } catch (final Exception e) {
            TDLog.d(TAG, "Exception occurred in getting app version");
        }
        mDeviceInfo = setupDeviceInfo(context);
    }


    private Map<String, Object> setupDeviceInfo(Context mContext)
    {
        final Map<String, Object> deviceInfo = new HashMap<>();
        {
            deviceInfo.put(TDConstants.KEY_LIB, sLibName);
            deviceInfo.put(TDConstants.KEY_LIB_VERSION, sLibVersion);
            if(mTimeZone != null)
            {
                TDTime installTime = new TDTime(new Date(packageInfo.firstInstallTime),mTimeZone);
                //to-do
                deviceInfo.put(TDConstants.KEY_INSTALL_TIME, installTime.getTime());
            }
            deviceInfo.put(TDConstants.KEY_OS, TDUtils.osName(mContext));
            deviceInfo.put(TDConstants.KEY_BUNDLE_ID, TDUtils.getCurrentProcessName(mContext));
            deviceInfo.put(TDConstants.KEY_OS_VERSION, TDUtils.osVersion(mContext));
            deviceInfo.put(TDConstants.KEY_MANUFACTURER, Build.MANUFACTURER);
            deviceInfo.put(TDConstants.KEY_DEVICE_MODEL, Build.MODEL);
            int[] size = getDeviceSize(mContext);
            deviceInfo.put(TDConstants.KEY_SCREEN_WIDTH, size[0]);
            deviceInfo.put(TDConstants.KEY_SCREEN_HEIGHT,size[1]);
            String operatorString = getCarrier(mContext);
            deviceInfo.put(TDConstants.KEY_CARRIER, operatorString);
            String androidID = getAndroidID(mContext);
            deviceInfo.put(TDConstants.KEY_DEVICE_ID, androidID);
            String systemLanguage = getSystemLanguage();
            deviceInfo.put(TDConstants.KEY_SYSTEM_LANGUAGE, systemLanguage);
            if (!TextUtils.isEmpty(mAppVersionName)){
                deviceInfo.put(TDConstants.KEY_APP_VERSION, mAppVersionName);
            }
            //to-do
            deviceInfo.put(TDConstants.KEY_SIMULATOR,isSimulator());
        }
        return Collections.unmodifiableMap(deviceInfo);
    }

    /**
     *根据CPU是否为电脑来判断是否为模拟器
     *返回:true 为模拟器
     */
    public static boolean isSimulator() {
        String cpuInfo = readCpuInfo();
        if ((cpuInfo.contains("intel") || cpuInfo.contains("amd") || !(cpuInfo.contains("hardware") || cpuInfo.contains("Hardware")))) {
            return true;
        }
        return false;
    }
    public static String readCpuInfo() {
        String result = "";
        try {
            String[] args = {"/system/bin/cat", "/proc/cpuinfo"};
            ProcessBuilder cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            StringBuffer sb = new StringBuffer();
            String readLine = "";
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"));
            while ((readLine = responseReader.readLine()) != null) {
                sb.append(readLine);
            }
            responseReader.close();
            result = sb.toString().toLowerCase();
        } catch (IOException ex) {
        }
        return result;
    }

    // 获取运营商信息
    private static String getCarrier(Context context) {
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
        return "";

    }

    @SuppressLint("HardwareIds")
    String getAndroidID(Context mContext) {
        String androidID = "";
        try {
            androidID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (TextUtils.isEmpty(androidID)) {
                //使用16位随机数id
                androidID = new StorageRandomDeviceID(new SharedPreferencesLoader().loadPreferences(mContext, "com.thinkingdata.analyse")).get();
            }
        }
        return androidID;
    }

    private String getSystemLanguage() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = LocaleList.getDefault().get(0);
        } else locale = Locale.getDefault();

        return locale.getLanguage();
    }

    String getAppVersionName() {
        return mAppVersionName;
    }

    public  Map<String, Object> getDeviceInfo() {
        return mDeviceInfo;
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
                return true;
            }

            Method checkSelfPermissionMethod = contextCompat.getMethod("checkSelfPermission", new Class[]{Context.class, String.class});
            int result = (int)checkSelfPermissionMethod.invoke(null, new Object[]{context, permission});
            if (result != PackageManager.PERMISSION_GRANTED) {
                TDLog.w(TAG, "You can fix this by adding the following to your AndroidManifest.xml file:\n"
                        + "<uses-permission android:name=\"" + permission + "\" />");
                return false;
            }

            return true;
        } catch (Exception e) {
            TDLog.w(TAG, e.toString());
            return true;
        }
    }

    boolean isOnline() {
        if (!mHasPermission) {
            return false;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
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

    String getNetworkType() {
        try {
            if (!mHasPermission) {
                return "NULL";
            }
            // Wifi
            ConnectivityManager manager = (ConnectivityManager)
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = null;
            if (manager != null) {
                networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                    return "WIFI";
                }
            }
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context
                    .TELEPHONY_SERVICE);
            return mobileNetworkType(mContext, telephonyManager, manager);
        } catch (Exception e) {
            return "NULL";
        }
    }
    @SuppressLint("MissingPermission")
    private String mobileNetworkType(Context context, TelephonyManager telephonyManager, ConnectivityManager connectivityManager) {
        // Mobile network
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (telephonyManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && (checkHasPermission(context, Manifest.permission.READ_PHONE_STATE) || telephonyManager.hasCarrierPrivileges())) {
                networkType = telephonyManager.getDataNetworkType();
            } else {
                try {
                    networkType = telephonyManager.getNetworkType();
                } catch (Exception ignored) {
                }
            }
        }
        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
//            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                // 在 Android 11 平台上，没有 READ_PHONE_STATE 权限时
//                return "NULL";
//            }
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
            case TelephonyManager.NETWORK_TYPE_IWLAN:
            case 19:  //目前已知有车机客户使用该标记作为 4G 网络类型 TelephonyManager.NETWORK_TYPE_LTE_CA:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
        }
        return "NULL";
    }

    public  JSONObject currentPresetProperties()
    {
        JSONObject presetProperties = null;
        if(mDeviceInfo != null)
        {
            presetProperties = new JSONObject(mDeviceInfo);
            presetProperties.remove(TDConstants.KEY_LIB);
            presetProperties.remove(TDConstants.KEY_LIB_VERSION);
        }else
        {
            presetProperties = new JSONObject();
        }
        return presetProperties;
    }
    public static int[] getDeviceSize(Context context) {
        int[] size = new int[2];
        try {
            int screenWidth, screenHeight;
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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
            //context.getResources().getDisplayMetrics()这种方式获取屏幕高度不包括底部虚拟导航栏
            if (context.getResources() != null) {
                final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                size[0] = displayMetrics.widthPixels;
                size[1] = displayMetrics.heightPixels;
            }
        }
        return size;
    }


    /**
     * 根据设备 rotation，判断屏幕方向，获取自然方向宽
     *
     * @param rotation 设备方向
     * @param width 逻辑宽
     * @param height 逻辑高
     * @return 自然尺寸
     */
    private static int getNaturalWidth(int rotation, int width, int height) {
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180 ?
                width : height;
    }

    /**
     * 根据设备 rotation，判断屏幕方向，获取自然方向高
     *
     * @param rotation 设备方向
     * @param width 逻辑宽
     * @param height 逻辑高
     * @return 自然尺寸
     */
    private static int getNaturalHeight(int rotation, int width, int height) {
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180 ?
                height : width;
    }

    /**
     * 获取 手机 RAM 信息
     * */
    public  String getRAM(Context context) {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            ActivityManager activityManager = (ActivityManager) context
                    .getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo() ;
            activityManager.getMemoryInfo(memoryInfo);
            long totalSize = memoryInfo.totalMem;
            long availableSize = memoryInfo.availMem;
            String total = String.format(Locale.CHINA, "%.1f", totalSize / 1024.0 / 1024.0 / 1024.0);
            String available = String.format(Locale.CHINA, "%.1f", availableSize / 1024.0 / 1024.0 / 1024.0);
            return available + "/" + total;
        }else
        {
            return  "0";
        }
    }

    /**
     * 判断SD是否挂载
     */
    public boolean isSDCardMount() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /**
     * 通过反射调用获取内置存储和外置sd卡根路径(通用)
     * HarmonyOS 正常获取
     * ANDROID 11 接口有变动
     *
     * @param mContext    上下文
     * @param is_removable 是否可移除，false返回内部存储，true返回外置sd卡
     * @return
     */
    private static String getStoragePath(Context mContext, boolean is_removable) {
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                getPath = storageVolumeClazz.getMethod("getPath");
            } else {
                getPath = storageVolumeClazz.getMethod("getDirectory");
            }
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = "";
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    path = (String) getPath.invoke(storageVolumeElement);
                } else {
                    path = ((File) getPath.invoke(storageVolumeElement)).getAbsolutePath();
                }
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removable == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getDisk(Context context, boolean isExternal) {

        String path = getStoragePath(context, isExternal);
        if (TextUtils.isEmpty(path)) {
            return "0";
        }

        File file = new File(path);
        StatFs statFs = new StatFs(file.getPath());
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            long blockCount = statFs.getBlockCountLong();
            long blockSize = statFs.getBlockSizeLong();
            long totalSpace = blockSize * blockCount;
            long availableBlocks = statFs.getAvailableBlocksLong();
            long availableSpace = availableBlocks * blockSize;
            String total = String.format(Locale.CHINA, "%.1f", totalSpace / 1024.0 / 1024.0 / 1024.0);
            String available = String.format(Locale.CHINA, "%.1f", availableSpace / 1024.0 / 1024.0 / 1024.0);
            return available + "/" + total;
        }
        return "0";

    }
}
