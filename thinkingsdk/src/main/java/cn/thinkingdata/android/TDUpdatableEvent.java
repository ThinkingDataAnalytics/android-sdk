package cn.thinkingdata.android;

import org.json.JSONObject;

import cn.thinkingdata.android.utils.TDConstants;

/**
 * 可被更新的事件。对应 track_update 操作。
 *
 * 某些场景下，需要对事件表中的属性进行更新。可以创建 TDUpdatableEvent 并传入标识此条数据点 eventId.
 * 在收到此类请求后，服务端会使用当前的属性，覆盖之前该 eventId 对应数据中同名的属性。
 */
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
