/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.preset;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import cn.thinkingdata.core.utils.ProcessUtil;
import cn.thinkingdata.core.utils.TDCommonUtil;
import cn.thinkingdata.core.utils.TDLog;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2024/3/4
 * @since
 */
public class TDPresetUtils {
    public static final String KEY_OS = "#os";
    public static final String KEY_OS_VERSION = "#os_version";
    public static final String KEY_BUNDLE_ID = "#bundle_id";
    public static final String KEY_MANUFACTURER = "#manufacturer";
    public static final String KEY_DEVICE_MODEL = "#device_model";
    public static final String KEY_SCREEN_HEIGHT = "#screen_height";
    public static final String KEY_SCREEN_WIDTH = "#screen_width";
    public static final String KEY_CARRIER = "#carrier";
    public static final String KEY_SYSTEM_LANGUAGE = "#system_language";
    public static final String KEY_APP_VERSION = "#app_version";
    public static final String KEY_SIMULATOR = "#simulator";
    public static final String KEY_DEVICE_TYPE = "#device_type";
    public static final String KEY_DEVICE_ID = "#device_id";
    public static final String KEY_NETWORK_TYPE = "#network_type";
    public static final String COMMAND_HARMONY_OS_VERSION = "getprop hw_sc.build.platform.version";

    public static String getHarmonyOSVersion() {
        String version = null;

        if (isHarmonyOS()) {
            version = TDCommonUtil.getProp("hw_sc.build.platform.version", "");
            if (TextUtils.isEmpty(version)) {
                version = exec(COMMAND_HARMONY_OS_VERSION);
            }
        }
        return version;
    }

    public static boolean isHarmonyOS() {
        try {
            Class<?> buildExClass = Class.forName("com.huawei.system.BuildEx");
            Object osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass);
            if (osBrand == null) {
                return false;
            }
            return "harmony".equalsIgnoreCase(osBrand.toString());
        } catch (Throwable e) {
            return false;
        }
    }

    public static String exec(String command) {
        InputStreamReader ir = null;
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec(command);
            ir = new InputStreamReader(process.getInputStream());
            input = new BufferedReader(ir);
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = input.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Throwable e) {
            TDLog.i("TDExec", e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable e) {
                    TDLog.i("TDExec", e.getMessage());
                }
            }
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException e) {
                    TDLog.i("TDExec", e.getMessage());
                }
            }
        }
        return null;
    }

    public static String getCurrentProcessName(Context context) {
        try {
            return ProcessUtil.getCurrentProcessName(context);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getRandomHEXValue(int numSize) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < numSize; i++) {
            char temp = 0;
            int key = ( int ) (Math.random() * 2);
            switch (key) {
                case 0:
                    temp = ( char ) (Math.random() * 10 + 48);
                    break;
                case 1:
                    temp = ( char ) (Math.random() * 6 + 'a');
                    break;
                default:
                    break;
            }
            str.append(temp);
        }
        return str.toString();
    }

    public static String getDeviceType(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                < Configuration.SCREENLAYOUT_SIZE_LARGE ? "Phone" : "Tablet";
    }

}
