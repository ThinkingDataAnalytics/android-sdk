/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import android.text.TextUtils;

import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.core.utils.TDLog;
import org.json.JSONObject;

/**
 *  TDUniqueEvent Used to describe the first event.
 *  The server uses the event name and #first_check_id to determine if the event was first fired.
 *  If the event already exists in the system, the current data is ignored.
 *  By default, the device ID is used as #first_check_id.
 */
public class TDFirstEvent extends TDEventModel {
    private static final String TAG = "ThinkingAnalytics.TDUniqueEvent";

    private String mExtraValue;

    /**
     * Constructor to create the first event object based on the event name and properties.
     *
     * @param eventName event name
     * @param properties event properties，can be null
     */
    public TDFirstEvent(String eventName, JSONObject properties) {
        super(eventName, properties);
    }

    /**
     * Set the custom #first_check_id.
     *
     * @param firstCheckId This parameter is used to detect whether it is reported for the first time.
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
