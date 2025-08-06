/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import org.json.JSONObject;

import cn.thinkingdata.analytics.utils.TDConstants;


/**
 * Events that can be updated. Corresponds to the track_update operation.
 * In some scenarios, attributes in the event table need to be updated. You can create a TDUpdatableEvent and pass in an eventId identifying this data point.
 * Upon receiving such a request, the server uses the current attribute to override the previous attribute of the same name in the corresponding data of the eventId.
 */
public class TDUpdatableEvent extends TDEventModel {
    private final String mEventId;

    public TDUpdatableEvent(String eventName, JSONObject properties, String eventId) {
        super(eventName, properties);
        mEventId = eventId;
    }

    @Override
    String getExtraField() {
        return TDConstants.KEY_EVENT_ID;
    }

    @Override
    String getExtraValue() {
        return mEventId;
    }

    @Override
    TDConstants.DataType getDataType() {
        return TDConstants.DataType.TRACK_UPDATE;
    }
}
