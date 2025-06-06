/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import cn.thinkingdata.analytics.TDConfig;
import cn.thinkingdata.analytics.tasks.TrackTaskManager;
import cn.thinkingdata.analytics.utils.CommonUtil;
import cn.thinkingdata.analytics.utils.TDDebugException;
import cn.thinkingdata.analytics.ThinkingAnalyticsSDK;
import cn.thinkingdata.analytics.utils.ITime;
import cn.thinkingdata.analytics.utils.PropertyUtils;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.analytics.utils.TDUtils;
import cn.thinkingdata.core.utils.TDLog;

/**
 * User attribute operation
 *
 * @author liulongbing
 * @since 2022/9/20
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
        try {
            if (null == propertyValue) {
                TDLog.d(TAG, "user_add value must be Number");
            } else {
                JSONObject properties = new JSONObject();
                properties.put(propertyName, propertyValue);
                user_add(properties,null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
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

    public void userOperation(final TDConstants.DataType type, final JSONObject properties, Date date) {
        if (instance.isTrackDisabled())  return;
        final ITime time = date == null ? instance.mCalibratedTimeManager.getTime() : instance.mCalibratedTimeManager.getTime(date, null);
        final String accountId = instance.getLoginId();
        final String distinctId = instance.getDistinctId();
        final boolean isSaveOnly = instance.isStatusTrackSaveOnly();
        final JSONObject cloneJson = CommonUtil.cloneJsonObject(properties);
        TrackTaskManager.getInstance().addTask(new Runnable() {
            @Override
            public void run() {
                if (!PropertyUtils.checkProperty(cloneJson)) {
                    TDLog.w(TAG, "The data contains invalid key or value: " + cloneJson.toString());
                }
                try {
                    JSONObject finalProperties = new JSONObject();
                    if (cloneJson != null) {
                        TDUtils.mergeJSONObject(cloneJson, finalProperties, mConfig.getDefaultTimeZone());
                    }
                    instance.trackInternal(new DataDescription(instance, type, finalProperties, time, distinctId, accountId, isSaveOnly));
                } catch (Exception e) {
                    TDLog.w(TAG, e.getMessage());
                }
            }
        });
    }
}
