/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import cn.thinkingdata.android.aidl.ITAToolClient;
import cn.thinkingdata.android.aidl.ITAToolServer;
import cn.thinkingdata.android.encrypt.TDSecreteKey;
import cn.thinkingdata.android.utils.TDLog;

/**
 * < AIDL >.
 *
 * @author bugliee
 * @create 2022/6/15
 * @since 1.0.0
 */
public class TAToolServiceServer extends Service {

    private Context mContext;
    private Map<String, ThinkingAnalyticsSDK> mInstances;

    @Override

    public IBinder onBind(Intent intent) {
        mContext = this;
        return iBinder;
    }

    private final IBinder iBinder = new ITAToolServer.Stub() {

        @Override
        public void trackLog(String cmdOrder) throws RemoteException {
            trackCurrentLog(cmdOrder);
        }

        @Override
        public void enableLog(boolean enable) throws RemoteException {
            TDLog.setEnableLogInner(enable);
        }

        @Override
        public void enableTransLog(boolean enable) throws RemoteException {
            enableTransLog = enable;
        }

        @Override
        public void clearLog() throws RemoteException {

        }

        @Override
        public void mockTrack(String eventName, String props) throws RemoteException {

        }

        @Override
        public String showSharedPreferences() throws RemoteException {
            return null;
        }

        @Override
        public void unbindThis() throws RemoteException {
            if (mTAToolClient != null) {
                mContext.getApplicationContext().unbindService(serviceConnection);
                if (readLogThread != null) {
                    readLogThread.interrupt();
                    readLogThread = null;
                }
                mTAToolClient = null;
                timer.cancel();
            }
        }

        @Override
        public String checkAppID(String appIDs) throws RemoteException {
            StringBuilder ret = new StringBuilder();
            mInstances = ThinkingAnalyticsSDK.getInstanceMap(mContext.getApplicationContext());
            String[] appIDArray = appIDs.split(",");
            for (String appID : appIDArray) {
                if (!mInstances.containsKey(appIDs)) {
                    ret.append(appID);
                    ret.append("  ");
                }
            }

            if (TextUtils.isEmpty(ret.toString())) {
                //验证通过，返回全局信息
                JSONObject jsonObject = new JSONObject();
                ThinkingAnalyticsSDK instance = mInstances.get(appIDArray[0]);
                TDPresetProperties presetProperties = instance.getPresetProperties();
                JSONObject presetObject = new JSONObject();
                try {
                    presetObject.put("appVersionName", presetProperties.appVersion);
                    presetObject.put("deviceID", presetProperties.deviceId);
                    presetObject.put("os", presetProperties.os);
                    presetObject.put("osVersion", presetProperties.osVersion);
                    presetObject.put("fps", presetProperties.fps);
                    presetObject.put("isSimulator", presetProperties.isSimulator);
                    presetObject.put("screenWidth", presetProperties.screenWidth);
                    presetObject.put("screenHeight", presetProperties.screenHeight);
                    presetObject.put("bundleId", presetProperties.bundleId);
                    presetObject.put("zoneOffset", presetProperties.zoneOffset);
                    presetObject.put("carrier", presetProperties.carrier);
                    presetObject.put("deviceModel", presetProperties.deviceModel);
                    presetObject.put("manufacture", presetProperties.manufacture);
                    presetObject.put("installTime", presetProperties.installTime);
                    presetObject.put("networkType", presetProperties.networkType);
                    presetObject.put("ram", presetProperties.ram);
                    presetObject.put("disk", presetProperties.disk);
                    presetObject.put("systemLanguage", presetProperties.systemLanguage);
                    jsonObject.put("presetProps", presetObject);
                    //
                    final PackageManager manager = mContext.getPackageManager();
                    String packageName = mContext.getPackageName();
                    PackageInfo packageInfo = manager.getPackageInfo(packageName, 0);
                    jsonObject.put("appVersionCode", packageInfo.versionCode);
                    jsonObject.put("appVersionName", packageInfo.versionName);
                    jsonObject.put("libVersion", TDConfig.VERSION);
                    jsonObject.put("enableLog", TDLog.getEnableLog());
                    jsonObject.put("setCalibrateTime", ThinkingAnalyticsSDK.getCalibratedTime() != null);

                    Resources resources = mContext.getResources();
                    try {
                        String[] array = resources.getStringArray(resources.getIdentifier(
                                "TACrashConfig", "array", packageName));
                        StringBuilder arrBuilder = new StringBuilder();
                        for (int i = 0; i < array.length; i++) {
                            arrBuilder.append(array[i]);
                            if (i < array.length - 1) {
                                arrBuilder.append(",");
                            }
                        }
                        jsonObject.put("crashConfig", arrBuilder.toString());
                    } catch (Resources.NotFoundException ex) {
                        //ignored
                    }
                    try {
                        String[] array = resources.getStringArray(resources.getIdentifier(
                                "TDDisPresetProperties", "array", packageName));
                        StringBuilder arrBuilder = new StringBuilder();
                        for (int i = 0; i < array.length; i++) {
                            arrBuilder.append(array[i]);
                            if (i < array.length - 1) {
                                arrBuilder.append(",");
                            }
                        }
                        jsonObject.put("disPresetProps", arrBuilder.toString());
                    } catch (Resources.NotFoundException ex) {
                        //ignored
                    }
                    try {
                        jsonObject.put("enableBGStart", resources.getBoolean(resources.getIdentifier("TAEnableBackgroundStartEvent", "bool", packageName)));
                    } catch (Resources.NotFoundException ex) {
                        jsonObject.put("enableBGStart", false);
                    }
                    try {
                        jsonObject.put("retentionDays", resources.getInteger(resources.getIdentifier("TARetentionDays", "integer", packageName)));
                    } catch (Resources.NotFoundException ex) {
                        jsonObject.put("retentionDays", 10);
                    }
                    try {
                        jsonObject.put("databaseLimit", resources.getInteger(resources.getIdentifier("TADatabaseLimit", "integer", packageName)));
                    } catch (Resources.NotFoundException ex) {
                        jsonObject.put("databaseLimit", 5000);
                    }
                    try {
                        jsonObject.put("mainProcessName", resources.getString(resources.getIdentifier("TADeFaultMainProcessName", "string", packageName)));
                    } catch (Resources.NotFoundException ex) {
                        jsonObject.put("mainProcessName", packageName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return jsonObject.toString();
            } else {
                return ret.toString();
            }

        }

        boolean enableBGStart = false;

        @Override
        public String getAppAndSDKInfo() throws RemoteException {
            Map<String, ThinkingAnalyticsSDK> instances = ThinkingAnalyticsSDK.getInstanceMap(mContext.getApplicationContext());
            if (instances.isEmpty()) {
                return "";
            }
            StringBuilder appIDs = new StringBuilder();
            StringBuilder urls = new StringBuilder();
            int count = 0;
            for (String key : instances.keySet()) {
                count++;
                appIDs.append(instances.get(key).mConfig.mToken);
                urls.append(instances.get(key).mConfig.getServerUrl());
                if (count < instances.size()) {
                    appIDs.append(",");
                    urls.append(",");
                }
            }

            ThinkingAnalyticsSDK instance = instances.get(appIDs.toString().split(",")[0]);

            if (instance != null) {
                TDPresetProperties presetProperties = instance.getPresetProperties();
                JSONObject object = new JSONObject();
                try {
                    final PackageManager manager = mContext.getPackageManager();
                    PackageInfo packageInfo = manager.getPackageInfo(mContext.getPackageName(), 0);
                    object.put("app_name", manager.getApplicationLabel(packageInfo.applicationInfo));
                    object.put("app_version_name", packageInfo.versionName);
                    object.put("app_version_code", packageInfo.versionCode);
                    object.put("lib_version", TDConfig.VERSION);
                    object.put("device_id", presetProperties.deviceId);
                    object.put("app_id", appIDs.toString());
                    object.put("server_url", urls.toString());
                    object.put("is_multi_process", instance.mConfig.isEnableMutiprocess());
                    object.put("sdk_mode", instance.mConfig.getMode().name());
                    String status = "";
                    if (!instance.isEnabled()) {
                        status = "PAUSE";
                    }
                    if (instance.hasOptOut()) {
                        status = "STOP";
                    }
                    if (instance.mPausePostFlag.get()) {
                        status = "SAVE_ONLY";
                    }
                    if (!instance.mPausePostFlag.get() && instance.isEnabled() && !instance.hasOptOut()) {
                        status = "NORMAL";
                    }
                    object.put("track_status", status);
                    Resources resources = mContext.getResources();
                    enableBGStart = resources.getBoolean(resources.getIdentifier("TAEnableBackgroundStartEvent", "bool", mContext.getPackageName()));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        object.put("enable_bg_start", enableBGStart);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return object.toString();
            }
            return "";
        }

        @Override
        public String getAppAndSDKInfoWithAppID(String appID) throws RemoteException {
            if (appID.isEmpty()) {
                return "";
            }
            Map<String, ThinkingAnalyticsSDK> instances = ThinkingAnalyticsSDK.getInstanceMap(mContext.getApplicationContext());
            if (instances.isEmpty()) {
                return "";
            }

            ThinkingAnalyticsSDK instance = instances.get(appID);

            if (instance != null) {
                JSONObject object = new JSONObject();
                try {
                    object.put("accountID", instance.getLoginId());
                    object.put("distinctID", instance.getDistinctId());
                    object.put("name", instance.mConfig.getName());
                    object.put("url", instance.mConfig.getServerUrl());
                    object.put("sdkMode", instance.mConfig.getMode().name());
                    int zoneOffset = instance.mConfig.getDefaultTimeZone().getRawOffset() / (3600 * 1000);
                    object.put("timeZone", ("GMT" + (zoneOffset > 0 ? ("+" + zoneOffset) : zoneOffset) + ":00"));
                    object.put("isMultiProcess", instance.mConfig.isEnableMutiprocess());
                    StringBuilder eventList = new StringBuilder();
                    List<ThinkingAnalyticsSDK.AutoTrackEventType> list = instance.getAutoTrackEventTypeList();
                    for (int i = 0; i < list.size(); i++) {
                        eventList.append(list.get(i).getEventName());
                        if (i < list.size() - 1) {
                            eventList.append(",");
                        }
                    }
                    object.put("autoTrackList", eventList.toString());
                    object.put("enabledEncrypt", instance.mConfig.mEnableEncrypt);
                    TDSecreteKey secreteKey = instance.mConfig.getSecreteKey();
                    if (secreteKey != null) {
                        object.put("encryptPublicKey", secreteKey.publicKey);
                        object.put("symmetricEncryption", secreteKey.symmetricEncryption);
                        object.put("asymmetricEncryption", secreteKey.asymmetricEncryption);
                        object.put("encryptVersion", secreteKey.version);
                    }
                    object.put("superProps", instance.getSuperProperties());
                    object.put("flushInterval", instance.mConfig.getFlushInterval());
                    object.put("flushBulkSize", instance.mConfig.getFlushBulkSize());
                    String status = "";
                    if (!instance.isEnabled()) {
                        status = "PAUSE";
                    }
                    if (instance.hasOptOut()) {
                        status = "STOP";
                    }
                    if (instance.mPausePostFlag.get()) {
                        status = "SAVE_ONLY";
                    }
                    if (!instance.mPausePostFlag.get() && instance.isEnabled() && !instance.hasOptOut()) {
                        status = "NORMAL";
                    }
                    object.put("trackState", status);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return object.toString();
            }
            return "";
        }


    };

    private void trackCurrentLog(String cmdOrder) {
        if (!cmdOrder.equals(cmdStr)) {
            cmdStr = cmdOrder;
            if (mTAToolClient == null) {
                Intent intent = new Intent();
                intent.setAction("cn.thinkingdata.android.TAToolClient");
                intent.setPackage("com.thinkingdata.tadebugtool");
                mContext.getApplicationContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
            if (readLogThread != null) {
                readLogThread.interrupt();
                readLogThread = null;
            }
            readLogThread = new ReadLogThread(cmdStr);
            readLogThread.start();
            if (!timerRunning) {
                timer.schedule(sendTask, 3000, 3000);
                timerRunning = true;
            }
        }
    }

    private ReadLogThread readLogThread = null;
    private boolean timerRunning = false;

    class ReadLogThread extends Thread {
        Process exec;
        InputStream inputStream;

        ReadLogThread(String cmdOrder) {
            try {
                exec = Runtime.getRuntime().exec(cmdOrder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = exec.getInputStream();
        }

        @Override
        public void run() {
            try {
                int len = 0;
                byte[] buf = new byte[1024];
                while (-1 != (len = inputStream.read(buf))) {
                    sendLog(new String(buf, 0, len));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendLog(String logStr) {
        if ((countLine >= 30) && enableTransLog && !sendStr.isEmpty()) {
            if (mTAToolClient != null) {
                try {
                    mTAToolClient.sendLog(sendStr);
                    sendStr = "";
                    countLine = 0;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            countLine++;
            sendStr += logStr;
        }
    }

    private final Timer timer = new Timer();

    private String cmdStr = "";

    private final TimerTask sendTask = new TimerTask() {
        @Override
        public void run() {
            if (mTAToolClient != null && enableTransLog) {
                try {
                    if (!sendStr.isEmpty()) {
                        mTAToolClient.sendLog(sendStr);
                        sendStr = "";
                        countLine = 0;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private ITAToolClient mTAToolClient;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTAToolClient = ITAToolClient.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("buglee", "onServiceDisconnected ==> ");
            if (readLogThread != null) {
                readLogThread.interrupt();
                readLogThread = null;
            }
            mTAToolClient = null;
            timer.cancel();
        }
    };

    private int countLine = 0;

    private String sendStr = "";

    private boolean enableTransLog = true;
}
