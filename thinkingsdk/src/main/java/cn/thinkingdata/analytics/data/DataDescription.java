/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.data;

import cn.thinkingdata.analytics.TDPresetProperties;
import cn.thinkingdata.analytics.ThinkingAnalyticsSDK;
import cn.thinkingdata.analytics.utils.ITime;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.analytics.utils.TDTime;
import cn.thinkingdata.analytics.utils.TDTimeCalibrated;
import cn.thinkingdata.analytics.utils.TDTimeConstant;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * TD data class.
 */
public class DataDescription {
    private static final boolean SAVE_TO_DATABASE = true;

    public String eventName;

    // Data time, #time field
    private final ITime mTime;
    // data type
    final TDConstants.DataType mType;

    private String mDistinctId;
    private String mAccountId;

    private final JSONObject mProperties;

    private Map<String, String> mExtraFields;

    public void setExtraFields(Map<String, String> extraFields) {
        mExtraFields = extraFields;
    }

    public boolean saveData = SAVE_TO_DATABASE;

    boolean mIsSaveOnly = false;

    final String mToken;

    public DataDescription(ThinkingAnalyticsSDK instance, TDConstants.DataType type, JSONObject properties, ITime time, String distinctId, String accountId, boolean isSaveOnly) {
        mType = type;
        mProperties = properties;
        mTime = time;
        mToken = instance.getToken();
        mDistinctId = distinctId;
        mAccountId = accountId;
        mIsSaveOnly = isSaveOnly;
    }

    public void setNoCache() {
        this.saveData = false;
    }

    /**
     * Get data, may block, do not call in the main thread.
     *
     * @return Data to be Reported
     */
    public JSONObject get() {
        JSONObject finalData = new JSONObject();

        try {
            finalData.put(TDConstants.KEY_TYPE, mType.getType());
            //  It may be blocked
            finalData.put(TDConstants.KEY_TIME, mTime.getTime());
            finalData.put(TDConstants.KEY_DISTINCT_ID, mDistinctId);
            if (null != mAccountId) {
                finalData.put(TDConstants.KEY_ACCOUNT_ID, mAccountId);
            }

            if (null != mExtraFields) {
                for (Map.Entry<String, String> entry : mExtraFields.entrySet()) {
                    finalData.put(entry.getKey(), entry.getValue());
                }
            }

            if (mType.isTrack()) {
                finalData.put(TDConstants.KEY_EVENT_NAME, eventName);
                Double zoneOffset = mTime.getZoneOffset();
                if (null != zoneOffset) {
                    mProperties.put(TDConstants.KEY_ZONE_OFFSET, zoneOffset);
                }
            }
//            if (mType == TDConstants.DataType.TRACK || mType == TDConstants.DataType.TRACK_UPDATE || mType == TDConstants.DataType.TRACK_OVERWRITE) {
//                int type = getCalibratedType();
//                if (type > 0) {
//                    mProperties.put(TDConstants.KEY_CALIBRATION_TYPE, getCalibratedType());
//                }
//            }
            finalData.put(TDConstants.KEY_PROPERTIES, mProperties);

        } catch (JSONException e) {

            e.printStackTrace();
        }

        return finalData;
    }

    /**
     * Gets whether the time is calibrated
     *
     * @return
     */
    private int getCalibratedType() {
        if (TDPresetProperties.disableList.contains(TDConstants.KEY_CALIBRATION_TYPE)) {
            return TDConstants.CALIBRATION_TYPE_CLOSE;
        }
        int type = TDConstants.CALIBRATION_TYPE_NONE;
        if (mTime instanceof TDTimeCalibrated) {
            type = TDConstants.CALIBRATION_TYPE_SUCCESS;
        } else if (mTime instanceof TDTime) {
            if ((( TDTime ) mTime).mCalibrationDisuse) {
                type = TDConstants.CALIBRATION_TYPE_DISUSE;
            }
        } else if (mTime instanceof TDTimeConstant) {
            return -1;
        }
        return type;
    }
}
