/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.thinkingdata.core.TDSettings;

/**
 * @author liulongbing
 * @since 2024/5/7
 */
public class TDCommonUtil {
    public static String getProp(String property, String defaultValue) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> spClz = Class.forName("android.os.SystemProperties");
            Method method = spClz.getDeclaredMethod("get", String.class);
            String value = ( String ) method.invoke(spClz, property);
            if (TextUtils.isEmpty(value)) {
                return defaultValue;
            }
            return value;
        } catch (Throwable throwable) {
            TDLog.i("TA.SystemProperties", throwable.getMessage());
        }
        return defaultValue;
    }

    private static String getJsonFromAssets(String fileName, Context context) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            AssetManager assetManager = context.getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assetManager.open(fileName)));

            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException var6) {
            var6.printStackTrace();
        }

        return stringBuilder.toString();
    }

    public static List<TDSettings> getConfigFromAssets(Context context) {
        String str = getJsonFromAssets("td_settings.json", context);
        List<TDSettings> settings = new ArrayList<>();
        try {
            JSONArray configList = new JSONArray(str);
            for (int i = 0; i < configList.length(); i++) {
                JSONObject configJson = configList.getJSONObject(i);
                if (configJson == null) continue;
                TDSettings setting = new TDSettings();
                setting.appId = configJson.optString("appId");
                setting.serverUrl = configJson.optString("serverUrl");
                if (TextUtils.isEmpty(setting.appId) || TextUtils.isEmpty(setting.serverUrl))
                    continue;
                setting.instanceName = configJson.optString("instanceName");
                int mode = configJson.optInt("mode");
                setting.mode = TDSettings.TDMode.values()[mode];
                double timeZone = configJson.optDouble("defaultTimeZone");
                if (timeZone != 0) {
                    setting.defaultTimeZone = TimeUtil.getTimeZone(timeZone);
                }
                setting.encryptVersion = configJson.optInt("encryptVersion");
                setting.encryptKey = configJson.optString("encryptKey");
                setting.enableAutoPush = configJson.optBoolean("enableAutoPush");
                setting.enableAutoCalibrated = configJson.optBoolean("enableAutoCalibrated");
                setting.enableLog = configJson.optBoolean("enableLog");
                setting.rccFetchParams = configJson.optJSONObject("rccFetchParams");
                settings.add(setting);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return settings;
    }
}
