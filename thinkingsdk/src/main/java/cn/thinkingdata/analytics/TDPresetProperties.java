/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import android.content.Context;
import android.content.res.Resources;

import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.core.preset.TDPresetUtils;
import cn.thinkingdata.core.utils.TDLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;

/**
 * Preset property class.
 * */
public class TDPresetProperties {

    private static final String TAG = "ThinkingAnalytics.TDPresetProperties";

    /**
     * Package name
     */
    public String bundleId;
    /**
     * Mobile phone SIM card operator information. If two SIM cards are used, the operator information of the main SIM card is obtained by default.
     */
    public String carrier;

    /**
     * Device ID
     */
    public String deviceId;

    /**
     * Device model
     */
    public String deviceModel;

    /**
     * Device manufacture
     */
    public String manufacture;

    /**
     * Network Type
     */
    public String networkType;

    /**
     * System
     */
    public String os;

    /**
     * System version
     */
    public String osVersion;

    /**
     * Screen height
     */
    public int screenHeight;

    /**
     * Screen width
     */
    public int screenWidth;
    /**
     * Mobile phone system language
     */
    public String systemLanguage;
    /**
     * Time zone offset
     * */
    public double zoneOffset;

    /**
     * App version
     */
    public String appVersion;

    /**
     * App install time
     */
    public String installTime;

    /**
     * Is it a simulator
     */
    public boolean isSimulator;

    /**
     * Available memory and total memory
     */
    public String ram;

    /**
     * Available disk and total disk
     */
    public String disk;

    /**
     * Frame rate
     */
    public int fps;

    /**
     * Device type
     */
    public String deviceType;

    /**
     * The attribute filtering list is preset.
     */
    public static final List<String> disableList = new ArrayList<>();

    /**
     * Preset properties
     */
    private JSONObject presetProperties;

    /**
     * TDPresetProperties
     *
     * @param presetProperties JSONObject
     */
    public TDPresetProperties(JSONObject presetProperties) {
        this.presetProperties = presetProperties;
        if (!disableList.contains(TDPresetUtils.KEY_BUNDLE_ID)) {
            this.bundleId = presetProperties.optString(TDPresetUtils.KEY_BUNDLE_ID);
        }
        if (!disableList.contains(TDPresetUtils.KEY_CARRIER)) {
            this.carrier = presetProperties.optString(TDPresetUtils.KEY_CARRIER);
        }
        if (!disableList.contains(TDPresetUtils.KEY_DEVICE_ID)) {
            this.deviceId = presetProperties.optString(TDPresetUtils.KEY_DEVICE_ID);
        }
        if (!disableList.contains(TDPresetUtils.KEY_DEVICE_MODEL)) {
            this.deviceModel = presetProperties.optString(TDPresetUtils.KEY_DEVICE_MODEL);
        }
        if (!disableList.contains(TDPresetUtils.KEY_MANUFACTURER)) {
            this.manufacture = presetProperties.optString(TDPresetUtils.KEY_MANUFACTURER);
        }
        if (!disableList.contains(TDPresetUtils.KEY_NETWORK_TYPE)) {
            this.networkType = presetProperties.optString(TDPresetUtils.KEY_NETWORK_TYPE);
        }
        if (!disableList.contains(TDPresetUtils.KEY_OS)) {
            this.os = presetProperties.optString(TDPresetUtils.KEY_OS);
        }
        if (!disableList.contains(TDPresetUtils.KEY_OS_VERSION)) {
            this.osVersion = presetProperties.optString(TDPresetUtils.KEY_OS_VERSION);
        }
        if (!disableList.contains(TDPresetUtils.KEY_SCREEN_HEIGHT)) {
            this.screenHeight = presetProperties.optInt(TDPresetUtils.KEY_SCREEN_HEIGHT);
        }
        if (!disableList.contains(TDPresetUtils.KEY_SCREEN_WIDTH)) {
            this.screenWidth = presetProperties.optInt(TDPresetUtils.KEY_SCREEN_WIDTH);
        }
        if (!disableList.contains(TDPresetUtils.KEY_SYSTEM_LANGUAGE)) {
            this.systemLanguage = presetProperties.optString(TDPresetUtils.KEY_SYSTEM_LANGUAGE);
        }

        this.zoneOffset = presetProperties.optDouble(TDConstants.KEY_ZONE_OFFSET);

        if (!disableList.contains(TDPresetUtils.KEY_APP_VERSION)) {
            this.appVersion = presetProperties.optString(TDPresetUtils.KEY_APP_VERSION);
        }
        if (!disableList.contains(TDConstants.KEY_INSTALL_TIME)) {
            this.installTime = presetProperties.optString(TDConstants.KEY_INSTALL_TIME);
        }
        if (!disableList.contains(TDPresetUtils.KEY_SIMULATOR)) {
            this.isSimulator = presetProperties.optBoolean(TDPresetUtils.KEY_SIMULATOR);
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
        if (!disableList.contains(TDPresetUtils.KEY_DEVICE_TYPE)) {
            this.deviceType = presetProperties.optString(TDPresetUtils.KEY_DEVICE_TYPE);
        }
    }

    /**
     * Generate prefabricated event properties. You cannot set prefabricated event properties to user prefabricated properties.
     *
     * @return JSONObject
     */
    public JSONObject toEventPresetProperties() {
        return this.presetProperties;
    }

    public TDPresetProperties() {}

    /**
     * Initialize the static property configuration
     * @param context context
     */
    public static void initDisableList(Context context) {
        synchronized (disableList) {
            if (disableList.isEmpty()) {
                try {
                    Resources resources = context.getResources();
                    String[] array = resources.getStringArray(resources.getIdentifier("TDDisPresetProperties", "array", context.getPackageName()));
                    disableList.addAll(Arrays.asList(array));
                } catch (NoClassDefFoundError e) {
//                    TDLog.e(TAG, e.toString());
                } catch (Exception e) {
                    //TDLog.e(TAG, e.toString());
                    //disableList.add(TDConstants.KEY_FPS);
                    //disableList.add(TDConstants.KEY_RAM);
                    //disableList.add(TDConstants.KEY_DISK);
                }
            }
        }
    }

    static void initDisableList(String[] mArray) {
        synchronized (disableList) {
            disableList.clear();
            disableList.addAll(Arrays.asList(mArray));
        }
    }
}