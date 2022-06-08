/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import android.content.Context;
import android.content.res.Resources;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;

/**
 * 预置属性类.
 * */
public class TDPresetProperties {

    private static final String TAG = "ThinkingAnalytics.TDPresetProperties";

    /**
     * 应用包名(当进程名和包名不一致时，返回进程名).
     */
    public String bundleId;
    /**
     *手机SIM卡运营商信息，双卡双待时，默认获取主卡运营商信息.
     */
    public String carrier;
    /**
     * 设备ID（设备的AndroidId）.
     */
    public String deviceId;
    /**
     * 设备型号.
     */
    public String deviceModel;
    /**
     * 厂商信息.
     */
    public String manufacture;
    /**
     * 网络类型.
     */
    public String networkType;
    /**
     * 系统类型.
     */
    public String os;
    /**
     * 系统版本号.
     */
    public String osVersion;
    /**
     * 屏幕高度.
     */
    public int screenHeight;
    /**
     * 屏幕宽度.
     */
    public int screenWidth;
    /**
     * 手机系统语言.
     */
    public String systemLanguage;
    /**
     * 时区偏移值.
     * */
    public double zoneOffset;
    /**
     * 应用版本号.
     */
    public String appVersion;
    /**
     * 安装时间.
     * */
    public String installTime;
    /**
     * 是否为模拟器.
     * */
    public boolean isSimulator;
    /**
     * ram使用情况.
     * */
    public String ram;
    /**
     * disk使用情况.
     * */
    public String disk;
    /**
     * fps.
     * */
    public int fps;

    /**
     * 预置属性过滤列表.
     */
    public static final List<String> disableList = new ArrayList<>();

    private JSONObject presetProperties;

    /**
     * < TDPresetProperties >.
     *
     * @param presetProperties JSONObject
     */
    public TDPresetProperties(JSONObject presetProperties) {
        this.presetProperties = presetProperties;
        if (!disableList.contains(TDConstants.KEY_BUNDLE_ID)) {
            this.bundleId = presetProperties.optString(TDConstants.KEY_BUNDLE_ID);
        }
        if (!disableList.contains(TDConstants.KEY_CARRIER)) {
            this.carrier = presetProperties.optString(TDConstants.KEY_CARRIER);
        }
        if (!disableList.contains(TDConstants.KEY_DEVICE_ID)) {
            this.deviceId = presetProperties.optString(TDConstants.KEY_DEVICE_ID);
        }
        if (!disableList.contains(TDConstants.KEY_DEVICE_MODEL)) {
            this.deviceModel = presetProperties.optString(TDConstants.KEY_DEVICE_MODEL);
        }
        if (!disableList.contains(TDConstants.KEY_MANUFACTURER)) {
            this.manufacture = presetProperties.optString(TDConstants.KEY_MANUFACTURER);
        }
        if (!disableList.contains(TDConstants.KEY_NETWORK_TYPE)) {
            this.networkType = presetProperties.optString(TDConstants.KEY_NETWORK_TYPE);
        }
        if (!disableList.contains(TDConstants.KEY_OS)) {
            this.os = presetProperties.optString(TDConstants.KEY_OS);
        }
        if (!disableList.contains(TDConstants.KEY_OS_VERSION)) {
            this.osVersion = presetProperties.optString(TDConstants.KEY_OS_VERSION);
        }
        if (!disableList.contains(TDConstants.KEY_SCREEN_HEIGHT)) {
            this.screenHeight = presetProperties.optInt(TDConstants.KEY_SCREEN_HEIGHT);
        }
        if (!disableList.contains(TDConstants.KEY_SCREEN_WIDTH)) {
            this.screenWidth = presetProperties.optInt(TDConstants.KEY_SCREEN_WIDTH);
        }
        if (!disableList.contains(TDConstants.KEY_SYSTEM_LANGUAGE)) {
            this.systemLanguage = presetProperties.optString(TDConstants.KEY_SYSTEM_LANGUAGE);
        }
        if (!disableList.contains(TDConstants.KEY_ZONE_OFFSET)) {
            this.zoneOffset = presetProperties.optDouble(TDConstants.KEY_ZONE_OFFSET);
        }
        if (!disableList.contains(TDConstants.KEY_APP_VERSION)) {
            this.appVersion = presetProperties.optString(TDConstants.KEY_APP_VERSION);
        }
        if (!disableList.contains(TDConstants.KEY_INSTALL_TIME)) {
            this.installTime = presetProperties.optString(TDConstants.KEY_INSTALL_TIME);
        }
        if (!disableList.contains(TDConstants.KEY_SIMULATOR)) {
            this.isSimulator = presetProperties.optBoolean(TDConstants.KEY_SIMULATOR);
        }
        if (!disableList.contains(TDConstants.KEY_RAM)) {
            this.ram = presetProperties.optString(TDConstants.KEY_RAM);
        }
        if (!disableList.contains(TDConstants.KEY_DISK)) {
            this.disk = presetProperties.optString(TDConstants.KEY_DISK);
        }
        if (!disableList.contains(TDConstants.KEY_FPS)) {
            this.fps = presetProperties.optInt(TDConstants.KEY_FPS);
        }
    }

    /**
     * 生成事件预制属性，不支持把事件预制属性设置为用户预制属性.
     *
     * @return JSONObject
     */
    public JSONObject toEventPresetProperties() {
        return this.presetProperties;
    }

    public TDPresetProperties() {}

    /**
     * 初始化静态属性配置.
     */
    static void initDisableList(Context context) {
        synchronized (disableList) {
            if (disableList.isEmpty()) {
                try {
                    Resources resources = context.getResources();
                    String[] array = resources.getStringArray(resources.getIdentifier("TDDisPresetProperties", "array", context.getPackageName()));
                    disableList.addAll(Arrays.asList(array));
                } catch (NoClassDefFoundError e) {
                    TDLog.e(TAG, e.toString());
                } catch (Exception e) {
                    TDLog.e(TAG, e.toString());

                }
            }
        }
    }

    /**
     * 初始化属性过滤配置.
     */
    static void initDisableList(String[] mArray) {
        synchronized (disableList) {
            disableList.clear();
            disableList.addAll(Arrays.asList(mArray));
        }
    }
}