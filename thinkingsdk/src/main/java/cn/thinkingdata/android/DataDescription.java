package cn.thinkingdata.android;

import org.json.JSONException;
import org.json.JSONObject;

import cn.thinkingdata.android.utils.ITime;
import cn.thinkingdata.android.utils.TDConstants;

/**
 * TD 数据类
 */
class DataDescription {
    private static final boolean SAVE_TO_DATABASE = true;

    String eventName; // 事件名称，如果有

    // 数据时间, #time 字段
    private final ITime mTime;
    // 数据类型
    final TDConstants.DataType mType;

    private String mDistinctId;
    private String mAccountId;

    private final JSONObject mProperties; // 属性

    boolean saveData = SAVE_TO_DATABASE;

    final String mToken;

    DataDescription(ThinkingAnalyticsSDK instance, TDConstants.DataType type, JSONObject properties, ITime time) {
        mType = type;
        mProperties = properties;
        mTime = time;
        mToken = instance.getToken();
        mDistinctId = instance.getDistinctId();
        mAccountId = instance.getLoginId();
    }

    void setNoCache() {
        this.saveData = false;
    }

    /**
     * 获取数据，可能会阻塞，不要在主线程中调用
     * @return 待上报数据
     */
    public JSONObject get() {
        JSONObject finalData = new JSONObject();

        try {
            finalData.put(TDConstants.KEY_TYPE, mType.getType());
            // 有可能会阻塞
            finalData.put(TDConstants.KEY_TIME, mTime.getTime());
            finalData.put(TDConstants.KEY_DISTINCT_ID, mDistinctId);
            if (null != mAccountId) {
                finalData.put(TDConstants.KEY_ACCOUNT_ID, mAccountId);
            }

            if (mType == TDConstants.DataType.TRACK) {
                finalData.put(TDConstants.KEY_EVENT_NAME, eventName);
                Double zoneOffset = mTime.getZoneOffset();
                if (null != zoneOffset) {
                    mProperties.put(TDConstants.KEY_ZONE_OFFSET, zoneOffset);
                }
            }

            finalData.put(TDConstants.KEY_PROPERTIES, mProperties);

        } catch (JSONException e) {

            e.printStackTrace();
        }

        return finalData;
    }
}
