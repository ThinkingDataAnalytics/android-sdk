/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * 封装时间类.
 * */
public class TDTime implements ITime {

    private final TimeZone mTimeZone;

    private final Date mDate;

    private boolean enableZoneOffset = true;

    public TDTime(Date date, TimeZone timeZone) {
        mDate = date == null ? new Date() : date;
        mTimeZone = timeZone;
    }

    /**
     * < getTime >.
     *
     * @return {@link String}
     */
    public String getTime() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
            if (null != mTimeZone) {
                dateFormat.setTimeZone(mTimeZone);
            }
            String ret = dateFormat.format(mDate);
            if (!Pattern.compile(TDConstants.TIME_CHECK_PATTERN).matcher(ret).find()) {
                ret = TDUtils.formatTime(mDate, mTimeZone);
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void disableZoneOffset() {
        enableZoneOffset = false;
    }

    /**
     * < getZoneOffset >.
     *
     * @return {@link Double}
     */
    public Double getZoneOffset() {
        if (enableZoneOffset && mTimeZone != null) {
            return TDUtils.getTimezoneOffset(mDate.getTime(), mTimeZone);
        }
        return null;
    }
}
