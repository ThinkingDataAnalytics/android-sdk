/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics.model;

import org.json.JSONObject;

import cn.thinkingdata.analytics.TDFirstEvent;
/**
 *
 * @author liulongbing
 * @since 2023/7/20
 */
public class TDFirstEventModel extends TDFirstEvent {
    public TDFirstEventModel(String eventName, JSONObject properties) {
        super(eventName, properties);
    }
}
