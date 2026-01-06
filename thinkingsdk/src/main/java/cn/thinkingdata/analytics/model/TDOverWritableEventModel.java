/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics.model;
import org.json.JSONObject;
import cn.thinkingdata.analytics.TDOverWritableEvent;
/**
 *
 * @author liulongbing
 * @since 2023/7/20
 */
public class TDOverWritableEventModel extends TDOverWritableEvent {
    public TDOverWritableEventModel(String eventName, JSONObject properties, String eventId) {
        super(eventName, properties, eventId);
    }
}
