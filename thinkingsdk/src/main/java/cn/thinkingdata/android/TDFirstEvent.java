package cn.thinkingdata.android;

import android.text.TextUtils;

import org.json.JSONObject;

import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;

/**
 *  TDUniqueEvent 用于描述首次事件。
 *
 *  服务端会根据事件名和 #first_check_id 来判断是否是首次触发该事件。
 *  如果系统中已经有该事件，则忽略当前数据。
 *
 *  默认情况下，会使用设备 ID 作为 #first_check_id.
 */
public class TDFirstEvent extends ThinkingAnalyticsEvent {
    private static final String TAG = "ThinkingAnalytics.TDUniqueEvent";

    private String mExtraValue;

    /**
     * 构造函数，根据事件名和属性创建首次事件对象
     *
     * @param eventName 事件名
     * @param properties 事件属性，可为 null
     */
    public TDFirstEvent(String eventName, JSONObject properties) {
        super(eventName, properties);
    }

    /**
     * 设置自定义的 #first_check_id
     *
     * @param firstCheckId 字符串，用于检测是否首次上报.
     */
    public void setFirstCheckId(String firstCheckId) {
        if (TextUtils.isEmpty(firstCheckId)) {
            TDLog.w(TAG, "Invalid firstCheckId. Use device Id");
            return;
        }
        mExtraValue = firstCheckId;
    }

    @Override
    String getExtraField() {
        return TDConstants.KEY_FIRST_CHECK_ID;
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
