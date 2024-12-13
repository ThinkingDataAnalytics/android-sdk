/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics.aop.push;

import org.json.JSONObject;

import cn.thinkingdata.analytics.utils.TDConstants;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2023/10/16
 * @since
 */
public class PushEventItem {
    public TDConstants.DataType type;
    public JSONObject properties;
    public String appId;
    public int from; //1 asm plugin 2 TDPush SDK
}
