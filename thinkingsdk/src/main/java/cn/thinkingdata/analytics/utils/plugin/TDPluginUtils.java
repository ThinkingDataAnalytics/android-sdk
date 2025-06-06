/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.analytics.utils.plugin;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import cn.thinkingdata.analytics.ThinkingAnalyticsSDK;
import cn.thinkingdata.analytics.utils.TDConstants;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2024/1/30
 * @since
 */
public class TDPluginUtils {

    private static final List<TDPluginMessage> tdMsgList = new ArrayList<>();

    public static void handlePluginMessage(final TDPluginMessage msg) {
        if (msg == null) return;
        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK instance) {
                handleEvent(msg, instance);
                instance.flush();
            }
        });
        tdMsgList.add(msg);
    }

    private static void handleEvent(TDPluginMessage msg, ThinkingAnalyticsSDK instance) {
        boolean needTrack = false;
        if (msg.from == TDPluginMessage.TD_FROM_ASM) {
            if (instance.mConfig.mEnableAutoPush) {
                needTrack = true;
            }
        } else if (msg.from >= 2) {
            if (TextUtils.isEmpty(msg.appId) || TextUtils.equals(instance.mConfig.mToken, msg.appId)) {
                needTrack = true;
            }
        }
        if (needTrack) {
            if (msg.type == TDConstants.DataType.TRACK) {
                if (msg.trackDebugType == 0) {
                    instance.autoTrack(msg.eventName, msg.properties);
                } else if (msg.trackDebugType == 2) {
                    instance.trackWithDebugOnly(msg.eventName, msg.properties);
                }
            } else if (msg.type == TDConstants.DataType.USER_SET) {
                instance.user_set(msg.properties);
            }
        }
    }

    public static void clearPluginEvent(ThinkingAnalyticsSDK instance) {
        for (TDPluginMessage tdPluginMessage : tdMsgList) {
            handleEvent(tdPluginMessage, instance);
        }
        if (tdMsgList.size() > 0) {
            instance.flush();
        }
    }
}
