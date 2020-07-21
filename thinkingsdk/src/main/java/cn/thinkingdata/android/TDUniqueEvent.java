package cn.thinkingdata.android;

import android.text.TextUtils;

import org.json.JSONObject;

import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;

public class TDUniqueEvent extends ThinkingAnalyticsEvent {
    private static final String TAG = "ThinkingAnalytics.TDUniqueEvent";

    private String mExtraValue;

    public TDUniqueEvent(String eventName, JSONObject properties) {
        super(eventName, properties);
    }

    public void setFirstCheckId(String firstCheckId) {
        if (TextUtils.isEmpty(firstCheckId)) {
            TDLog.w(TAG, "Invalid firstCheckId. Use device Id");
            return;
        }
        mExtraValue = firstCheckId;
    }

    @Override
    String getExtraField() {
        return "#first_check_id";
    }

    @Override
    String getExtraValue() {
        return mExtraValue;
    }

    @Override
    TDConstants.DataType getDataType() {
        return TDConstants.DataType.TRACK;
    }
}
