package cn.thinkingdata.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
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
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import org.json.JSONObject;

import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;
import cn.thinkingdata.android.utils.TDTime;
import cn.thinkingdata.android.utils.TDUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

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
                //deviceInfo.put(TDConstants.KEY_INSTALL_TIME, installTime.getTime());
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
            //to-do
            //deviceInfo.put(TDConstants.KEY_SIMULATOR,isSimulator());
        }
        return Collections.unmodifiableMap(deviceInfo);
    }
    /*
     *根据CPU是否为电脑来判断是否为模拟器
     *返回:true 为模拟器
     */
    public static boolean isSimulator() {
        String cpuInfo = readCpuInfo();
        if ((cpuInfo.contains("intel") || cpuInfo.contains("amd"))) {
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
    private String getAndroidID(Context mContext) {
        String androidID = "";
        try {
            androidID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
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
            long totalSize = 0;
            long availableSize = 0;
            ActivityManager activityManager = (ActivityManager) context
                    .getSystemService(context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo() ;
            activityManager.getMemoryInfo(memoryInfo);
            totalSize = memoryInfo.totalMem;
            availableSize = memoryInfo.availMem;
            double total =  Double.parseDouble(String.format("%.2f", totalSize/1024.0/1024.0/1024.0));
            double available = Double.parseDouble(String.format("%.2f", availableSize/1024.0/1024.0/1024.0));
            return available+"/"+total;
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
     * 使用反射方法 获取手机存储路径
     *
     * **/
    public String getStoragePath(Context context, int type) {

        StorageManager sm = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getPathsMethod = sm.getClass().getMethod("getVolumePaths",
                    (Class<?>) null);
            String[] path = (String[]) getPathsMethod.invoke(sm, (Object) null);

            switch (type) {
                case INTERNAL_STORAGE:
                    return path[type];
                case EXTERNAL_STORAGE:
                    if (path.length > 1) {
                        return path[type];
                    } else {
                        return null;
                    }

                default:
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getDisk(Context context, int type) {

        String path = getStoragePath(context, type);
        /**
         * 无外置SD 卡判断
         * **/
        if (isSDCardMount() == false || TextUtils.isEmpty(path) || path == null) {
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
            double total =  Double.parseDouble(String.format("%.2f", totalSpace/1024.0/1024.0/1024.0));
            double available = Double.parseDouble(String.format("%.2f", availableSpace/1024.0/1024.0/1024.0));
            return available+"/"+total;
        }
        return "0";

    }




}
