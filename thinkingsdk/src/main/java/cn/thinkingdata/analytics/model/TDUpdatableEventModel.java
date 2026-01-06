/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics.model;
import org.json.JSONObject;
import cn.thinkingdata.analytics.TDUpdatableEvent;
/**
 *
 * @author liulongbing
 * @since 2023/7/20
 */
public class TDUpdatableEventModel extends TDUpdatableEvent {
    public TDUpdatableEventModel(String eventName, JSONObject properties, String eventId) {
        super(eventName, properties, eventId);
    }
}
