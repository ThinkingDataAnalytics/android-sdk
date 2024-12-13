/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.List;

import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.analytics.utils.plugin.TDPluginMessage;
import cn.thinkingdata.analytics.utils.plugin.TDPluginUtils;
import cn.thinkingdata.core.TDSettings;
import cn.thinkingdata.core.router.TRouterMap;
import cn.thinkingdata.core.router.plugin.IPlugin;
import cn.thinkingdata.core.router.plugin.MethodCall;
import cn.thinkingdata.ta_apt.TRoute;

/**
 * thinking analytics receiver module
 *
 * @author liulongbing
 * @since 2023/6/21
 */
@TRoute(path = TRouterMap.ANALYTIC_ROUTE_PATH)
public class ThinkingAnalyticsPlugin implements IPlugin {

    @Override
    public void onMethodCall(MethodCall call) {
        try {
            String methodName = call.method;
            if (TextUtils.equals(methodName, "track") || TextUtils.equals(methodName, "userSet")) {
                String appId = call.argument("appId");
                JSONObject properties = call.argument("properties");
                int from = -1;
                if (call.hasKey("from")) {
                    from = call.argument("from");
                }
                String eventName = call.argument("eventName");
                int trackDebugType = call.argument("trackDebugType");
                TDPluginMessage msg = new TDPluginMessage();
                msg.appId = appId;
                msg.eventName = eventName;
                msg.properties = properties;
                msg.from = from;
                msg.type = getDataType(call);
                msg.trackDebugType = trackDebugType;
                TDPluginUtils.handlePluginMessage(msg);
            } else if (TextUtils.equals(methodName, "init")) {
                Context context = call.argument("context");
                List<TDSettings> settings = call.argument("settings");
                if (context == null || settings == null) return;

                for (TDSettings setting : settings) {
                    if (TextUtils.isEmpty(setting.instanceName)) {
                        setting.instanceName = setting.appId;
                    }
                    TDConfig config = TDConfig.getInstance(context, setting.appId, setting.serverUrl, setting.instanceName);
                    if (setting.mode != null) {
                        config.setMode(TDConfig.TDMode.values()[setting.mode.ordinal()]);
                    }
                    if (setting.defaultTimeZone != null) {
                        config.setDefaultTimeZone(setting.defaultTimeZone);
                    }
                    if (setting.encryptVersion > 0 && !TextUtils.isEmpty(setting.encryptKey)) {
                        config.enableEncrypt(setting.encryptVersion, setting.encryptKey);
                    }
                    config.enableAutoCalibrated = setting.enableAutoCalibrated;
                    if (setting.sslSocketFactory != null) {
                        config.setSSLSocketFactory(setting.sslSocketFactory);
                    }
                    TDAnalytics.enableLog(setting.enableLog);
                    TDAnalytics.init(config);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TDConstants.DataType getDataType(MethodCall call) {
        String methodName = call.method;
        if (TextUtils.equals(methodName, "track")) {
            return TDConstants.DataType.TRACK;
        } else if (TextUtils.equals(methodName, "userSet")) {
            return TDConstants.DataType.USER_SET;
        }
        return null;
    }

}
