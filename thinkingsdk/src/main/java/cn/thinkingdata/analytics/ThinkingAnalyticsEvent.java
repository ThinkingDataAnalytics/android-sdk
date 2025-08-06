/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics;

import org.json.JSONObject;

import java.util.Date;
import java.util.TimeZone;

import cn.thinkingdata.analytics.utils.TDConstants;

/**
 * ThinkingAnalyticsEvent.
 * */
public abstract class ThinkingAnalyticsEvent {
    private final String mEventName;
    private final JSONObject mProperties;
    private Date mEventTime;
    private TimeZone mTimeZone;

    ThinkingAnalyticsEvent(String eventName, JSONObject properties) {
        mEventName = eventName;
        mProperties = properties;
    }

    public void setEventTime(Date time) {
        mEventTime = time;
    }

    public void setEventTime(Date time, TimeZone timeZone) {
        mEventTime = time;
        mTimeZone = timeZone;
    }

    abstract String getExtraField();

    abstract String getExtraValue();

    abstract TDConstants.DataType getDataType();

    String getEventName() {
        return mEventName;
    }

    JSONObject getProperties() {
        return mProperties;
    }

    Date getEventTime() {
        return mEventTime;
    }

    TimeZone getTimeZone() {
        return mTimeZone;
    }
}
