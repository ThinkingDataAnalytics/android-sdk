/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics;

import org.json.JSONObject;

/**
 *
 * @author liulongbing
 * @create 2023/7/20
 * @since
 */
public abstract class TDEventModel extends ThinkingAnalyticsEvent{
    TDEventModel(String eventName, JSONObject properties) {
        super(eventName, properties);
    }
}
