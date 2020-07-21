package cn.thinkingdata.android;

import org.json.JSONObject;

import cn.thinkingdata.android.utils.TDConstants;

public class TDOverWritableEvent extends ThinkingAnalyticsEvent {
    private final String mEventId;
    public TDOverWritableEvent(String eventName, JSONObject properties, String eventId) {
        super(eventName, properties);
        mEventId = eventId;
    }

    @Override
    String getExtraField() {
        return "#event_id";
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
