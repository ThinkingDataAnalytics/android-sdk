package cn.thinkingdata.android;

import android.text.TextUtils;

import org.json.JSONObject;

import cn.thinkingdata.android.utils.TDConstants;

public class TDUpdatableEvent extends ThinkingAnalyticsEvent {
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
