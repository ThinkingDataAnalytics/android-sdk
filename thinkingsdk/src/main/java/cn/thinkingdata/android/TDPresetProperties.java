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
 * Preset property class.
 * */
public class TDPresetProperties {

    private static final String TAG = "ThinkingAnalytics.TDPresetProperties";

    /**
     * package name
     */
    public String bundleId;
    /**
     * Mobile phone SIM card operator information. If two SIM cards are used, the operator information of the main SIM card is obtained by default.
     */
    public String carrier;

    public String deviceId;

    public String deviceModel;

    public String manufacture;

    public String networkType;

    public String os;

    public String osVersion;

    public int screenHeight;

    public int screenWidth;
    /**
     * Mobile phone system language
     */
    public String systemLanguage;
    /**
     * Time zone offset
     * */
    public double zoneOffset;

    public String appVersion;

    public String installTime;

    public boolean isSimulator;

    public String ram;

    public String disk;

    public int fps;

    public String deviceType;

    /**
     * The attribute filtering list is preset.
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

        this.zoneOffset = presetProperties.optDouble(TDConstants.KEY_ZONE_OFFSET);

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
        if (!disableList.contains(TDConstants.KEY_DEVICE_TYPE)) {
            this.deviceType = presetProperties.optString(TDConstants.KEY_DEVICE_TYPE);
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

    static void initDisableList(String[] mArray) {
        synchronized (disableList) {
            disableList.clear();
            disableList.addAll(Arrays.asList(mArray));
        }
    }
}