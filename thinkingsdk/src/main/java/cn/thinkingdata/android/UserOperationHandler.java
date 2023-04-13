/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import cn.thinkingdata.android.utils.ITime;
import cn.thinkingdata.android.utils.PropertyUtils;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;
import cn.thinkingdata.android.utils.TDUtils;

/**
 * User attribute operation
 *
 * @author liulongbing
 * @create 2022/9/20
 * @since
 */
public class UserOperationHandler {

    static final String TAG = "ThinkingAnalytics.UserOperation";
    private final ThinkingAnalyticsSDK instance;
    private final TDConfig mConfig;

    public UserOperationHandler(ThinkingAnalyticsSDK instance, TDConfig mConfig) {
        this.instance = instance;
        this.mConfig = mConfig;
    }

    public void user_add(String propertyName, Number propertyValue){
        if (instance.hasDisabled()) {
            return;
        }
        try {
            if (null == propertyValue) {
                TDLog.d(TAG, "user_add value must be Number");
                if (mConfig.shouldThrowException()) {
                    throw new TDDebugException("Invalid property values for user add.");
                }
            } else {
                JSONObject properties = new JSONObject();
                properties.put(propertyName, propertyValue);
                user_add(properties,null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            if (mConfig.shouldThrowException()) {
                throw new TDDebugException(e);
            }
        }
    }

    public void user_add(JSONObject properties, Date date) {
        instance.user_operations(TDConstants.DataType.USER_ADD, properties, date);
    }

    public void user_append(JSONObject properties, Date date) {
        instance.user_operations(TDConstants.DataType.USER_APPEND, properties, date);
    }

    public void user_uniqAppend(JSONObject properties, Date date) {
        instance.user_operations(TDConstants.DataType.USER_UNIQ_APPEND, properties, date);
    }

    public void user_setOnce(JSONObject properties, Date date) {
        instance.user_operations(TDConstants.DataType.USER_SET_ONCE, properties, date);
    }

    public void user_set(JSONObject properties, Date date) {
        instance.user_operations(TDConstants.DataType.USER_SET, properties, date);
    }

    public void user_delete(Date date) {
        instance.user_operations(TDConstants.DataType.USER_DEL, null, date);
    }

    public void user_unset(String... properties) {
        if (instance.hasDisabled()) {
            return;
        }
        if (properties == null) {
            return;
        }
        JSONObject props = new JSONObject();
        for (String s : properties) {
            try {
                props.put(s, 0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (props.length() > 0) {
            user_unset(props, null);
        }
    }

    public void user_unset(JSONObject properties, Date date) {
        instance.user_operations(TDConstants.DataType.USER_UNSET, properties, date);
    }

    public void userOperation(TDConstants.DataType type, JSONObject properties, Date date) {
        if (instance.hasDisabled()) {
            return;
        }
        if (!PropertyUtils.checkProperty(properties)) {
            TDLog.w(TAG, "The data contains invalid key or value: " + properties.toString());
            if (mConfig.shouldThrowException()) {
                throw new TDDebugException("Invalid properties. Please refer to SDK debug log for detail reasons.");
            }
        }
        try {
            ITime time = date == null ? instance.mCalibratedTimeManager.getTime() : instance.mCalibratedTimeManager.getTime(date, null);
            JSONObject finalProperties = new JSONObject();
            if (properties != null) {
                TDUtils.mergeJSONObject(properties, finalProperties, mConfig.getDefaultTimeZone());
            }
            instance.trackInternal(new DataDescription(instance, type, finalProperties, time));
        } catch (Exception e) {
            TDLog.w(TAG, e.getMessage());
        }
    }
}
