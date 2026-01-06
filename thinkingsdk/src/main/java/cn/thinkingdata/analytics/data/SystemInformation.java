/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.data;

import static android.content.Context.ACTIVITY_SERVICE;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import org.json.JSONObject;

import cn.thinkingdata.analytics.TDConfig;
import cn.thinkingdata.analytics.TDPresetProperties;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.analytics.utils.TDTime;
import cn.thinkingdata.analytics.utils.TDUtils;
import cn.thinkingdata.core.preset.TDPresetModel;
import cn.thinkingdata.core.utils.TDLog;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


public class SystemInformation {

    private static String sLibName = "Android";
    private static String sLibVersion = TDConfig.VERSION;
    private static SystemInformation sInstance;
    private static final Object sInstanceLock = new Object();
    private boolean hasNotUpdated;
    //private PackageInfo packageInfo;
    private long firstInstallTime;
    private final TimeZone mTimeZone;
    private static final String TAG = "ThinkingAnalytics.SystemInformation";
    private final Map<String, Object> mDeviceInfo;
    private final Context mContext;
    private final boolean mHasPermission;
    private String mStoragePath; //Save the external card path of the mobile phone

    public static void setLibraryInfo(String libName, String libVersion) {
        if (!TextUtils.isEmpty(libName)) {
            sLibName = libName;
            TDLog.d(TAG, "#lib has been changed to: " + libName);
        }

        if (!TextUtils.isEmpty(libVersion)) {
            sLibVersion = libVersion;
            TDLog.d(TAG, "#lib_version has been changed to: " + libVersion);
        }
    }

    public long getFirstInstallTime() {
        return firstInstallTime;
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
                sInstance = new SystemInformation(context, null);
            }
            return sInstance;
        }
    }

    public static SystemInformation getInstance(Context context, TimeZone timeZone) {
        synchronized (sInstanceLock) {
            if (null == sInstance) {
                sInstance = new SystemInformation(context, timeZone);
            }
            return sInstance;
        }
    }

    public boolean hasNotBeenUpdatedSinceInstall() {
        return hasNotUpdated;
    }

    private SystemInformation(Context context, TimeZone timeZone) {
        mContext = context.getApplicationContext();
        mTimeZone = timeZone;
        mHasPermission = checkHasPermission(mContext, "android.permission.ACCESS_NETWORK_STATE");
        try {
            final PackageManager manager = context.getPackageManager();
            PackageInfo packageInfo = manager.getPackageInfo(context.getPackageName(), 0);
            firstInstallTime = packageInfo.firstInstallTime;
            hasNotUpdated = firstInstallTime == packageInfo.lastUpdateTime;
            TDLog.d(TAG, "First Install Time: " + packageInfo.firstInstallTime);
            TDLog.d(TAG, "Last Update Time: " + packageInfo.lastUpdateTime);
        } catch (final Exception e) {
            TDLog.d(TAG, "Exception occurred in getting app version");
        }
        mDeviceInfo = setupDeviceInfo(context);
    }

    private Map<String, Object> setupDeviceInfo(Context mContext) {
        final Map<String, Object> deviceInfo = new HashMap<>();
        if (!TDPresetProperties.disableList.contains(TDConstants.KEY_LIB)) {
            deviceInfo.put(TDConstants.KEY_LIB, sLibName);
        }
        if (!TDPresetProperties.disableList.contains(TDConstants.KEY_LIB_VERSION)) {
            deviceInfo.put(TDConstants.KEY_LIB_VERSION, sLibVersion);
        }
        if (mTimeZone != null
                && !TDPresetProperties.disableList.contains(TDConstants.KEY_INSTALL_TIME)) {
            TDTime installTime = new TDTime(new Date(firstInstallTime), mTimeZone);
            deviceInfo.put(TDConstants.KEY_INSTALL_TIME, installTime.getTime());
        }
        Map<String, Object> presetProperties = TDPresetModel.getInstance(mContext).getPresetProperties();
        for (String key : presetProperties.keySet()) {
            deviceInfo.put(key,presetProperties.get(key));
        }
        return Collections.unmodifiableMap(deviceInfo);
    }

    public String getAppVersionName() {
        return TDPresetModel.getInstance(mContext).getAppVersionName();
    }

    public  Map<String, Object> getDeviceInfo() {
        return mDeviceInfo;
    }

    public String getDeviceId() {
        return TDPresetModel.getInstance(mContext).getDeviceId();
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

            Method checkSelfPermissionMethod = contextCompat.getMethod("checkSelfPermission",
                    new Class[]{Context.class, String.class});
            int result = (int) checkSelfPermissionMethod.invoke(null,
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
            return true;
        }
    }

    boolean isOnline() {
        if (!mHasPermission) {
            return false;
        }
        try {
            ConnectivityManager cm
                    = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getCurrentNetworkType() {
        return TDPresetModel.getInstance(mContext).getCurrentNetworkType();
    }

    public JSONObject currentPresetProperties() {
        JSONObject presetProperties = null;
        if (mDeviceInfo != null) {
            presetProperties = new JSONObject(mDeviceInfo);
            presetProperties.remove(TDConstants.KEY_LIB);
            presetProperties.remove(TDConstants.KEY_LIB_VERSION);
        } else {
            presetProperties = new JSONObject();
        }
        return presetProperties;
    }

    /**
     * Obtain the RAM information of the phone.
     * */
    public  String getRAM(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityManager activityManager = (ActivityManager) context
                    .getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            long totalSize = memoryInfo.totalMem;
            long availableSize = memoryInfo.availMem;
            double total = TDUtils.formatNumber(totalSize / 1024.0 / 1024.0 / 1024.0);
            double available = TDUtils.formatNumber(availableSize / 1024.0 / 1024.0 / 1024.0);
            return available + "/" + total;
        } else {
            return "0";
        }
    }

    /**
     *  Get internal memory and external sd card root path via reflection call (common)
     *
     * @param mContext  context
     * @param isRemovable Can be removed
     * @return Path
     */
    private static String getStoragePath(Context mContext, boolean isRemovable) {
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
            Method isRemovableMethod = storageVolumeClazz.getMethod("isRemovable");
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
                boolean removable = (Boolean) isRemovableMethod.invoke(storageVolumeElement);
                if (isRemovable == removable) {
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
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public String getDisk(Context context, boolean isExternal) {
        if (TextUtils.isEmpty(mStoragePath)) {
            mStoragePath = getStoragePath(context, isExternal);
        }
        if (TextUtils.isEmpty(mStoragePath)) {
            return "0";
        }
        try {
            File file = new File(mStoragePath);
//        if (!file.exists()) {
//            return "0";
//        }
            StatFs statFs = new StatFs(file.getPath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                long blockCount = statFs.getBlockCountLong();
                long blockSize = statFs.getBlockSizeLong();
                long totalSpace = blockSize * blockCount;
                long availableBlocks = statFs.getAvailableBlocksLong();
                long availableSpace = availableBlocks * blockSize;
                double total = TDUtils.formatNumber(totalSpace / 1024.0 / 1024.0 / 1024.0);
                double available = TDUtils.formatNumber(availableSpace / 1024.0 / 1024.0 / 1024.0);
                return available + "/" + total;
            }
        } catch (Exception e) {

        }
        return "0";

    }
}
