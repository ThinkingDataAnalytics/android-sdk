package cn.thinkingdata.android;

import org.json.JSONObject;

import cn.thinkingdata.android.utils.TDConstants;

/**
 * 可被重写的事件，对应 ta_overwrite 操作。
 *
 * 创建 TDOverWritableEvent 对象以重写之前的事件数据。传入 eventId 指定需要被重写的事件。
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
