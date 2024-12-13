/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.analytics.utils.plugin;

import org.json.JSONObject;

import cn.thinkingdata.analytics.utils.TDConstants;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2024/1/30
 * @since
 */
public class TDPluginMessage {

    public static final int TD_FROM_ASM = 1;
    public static final int TD_FROM_PUSH = 2;

    public TDConstants.DataType type;
    public String eventName;
    public JSONObject properties;
    public String appId;
    public int from;
    public int trackDebugType = 0;

}
