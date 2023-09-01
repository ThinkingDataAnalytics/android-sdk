/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import cn.thinkingdata.android.utils.TDConstants;
import org.json.JSONObject;


/**
 * Overridden event that corresponds to the ta_overwrite operation.
 * Create the TDOverWritableEvent object to override the previous event data.
 * Passing eventId specifies the event that needs to be overridden.
 */
public class TDOverWritableEvent extends ThinkingAnalyticsEvent {
    private final String mEventId;

    public TDOverWritableEvent(String eventName, JSONObject properties, String eventId) {
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
        return TDConstants.DataType.TRACK_OVERWRITE;
    }
}
