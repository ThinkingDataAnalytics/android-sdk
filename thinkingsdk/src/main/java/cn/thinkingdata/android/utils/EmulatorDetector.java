/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.utils;

import android.text.TextUtils;
import java.lang.reflect.Method;

/**
 * EmulatorDetector.
 * */
public class EmulatorDetector {

    /**
     * Detects if app is currenly running on emulator, or real device.
     *
     * @return true for emulator, false for real devices
     */
    public static boolean isEmulator() {
        return mayOnEmulatorViaQEMU() || isEmulatorFromAbi();
    }

    private static boolean mayOnEmulatorViaQEMU() {
        String qemu = getProp("ro.kernel.qemu");
        return "1".equals(qemu);
    }

    private static boolean isEmulatorFromAbi() {
        String abi = getProp("ro.product.cpu.abi");
        if (abi == null) {
            return false;
        }
        return !TextUtils.isEmpty(abi) && abi.contains("x86");
    }


    private static String getProp(String property) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method method = systemProperties.getMethod("get", String.class);
            Object[] params = new Object[1];
            params[0] = property;
            return (String) method.invoke(systemProperties, params);
        } catch (Exception e) {
            return null;
        }
    }

}
