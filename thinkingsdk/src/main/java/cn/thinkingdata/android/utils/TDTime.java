package cn.thinkingdata.android.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TDTime implements ITime {

    private final TimeZone mTimeZone;

    private final Date mDate;

    private boolean enableZoneOffset = true;

    public TDTime(Date date, TimeZone timeZone) {
        mDate = date == null ? new Date() : date;
        mTimeZone = timeZone;
    }

    public String getTime() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
            if(null != mTimeZone) dateFormat.setTimeZone(mTimeZone);
            return dateFormat.format(mDate);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void disableZoneOffset() {
        enableZoneOffset = false;
    }

    public Double getZoneOffset() {
        if (enableZoneOffset && mTimeZone != null) {
            return TDUtils.getTimezoneOffset(mDate.getTime(), mTimeZone);
        }
        return null;
    }
}
