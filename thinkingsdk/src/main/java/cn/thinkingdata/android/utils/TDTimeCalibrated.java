package cn.thinkingdata.android.utils;

import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TDTimeCalibrated implements ITime {
    private long mSystemElapsedRealtime;
    private TimeZone mTimeZone;
    private ICalibratedTime mCalibratedTime;

    private Date mDate;

    public TDTimeCalibrated(ICalibratedTime calibratedTime, TimeZone timeZone, long systemElapsedRealtime) {
        init(calibratedTime, timeZone, systemElapsedRealtime);
    }

    public TDTimeCalibrated(ICalibratedTime calibratedTime, TimeZone timeZone) {
        init(calibratedTime, timeZone, SystemClock.elapsedRealtime());
    }

    private void init(ICalibratedTime calibratedTime, TimeZone timeZone, long systemElapsedRealtime) {
        mCalibratedTime = calibratedTime;
        mTimeZone = timeZone;
        mSystemElapsedRealtime = systemElapsedRealtime;
    }

    private synchronized Date getDate() {
        if (null == mDate) {
            mDate = mCalibratedTime.get(mSystemElapsedRealtime);
        }
        return mDate;
    }

    @Override
    public String getTime() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
            dateFormat.setTimeZone(mTimeZone);
            return dateFormat.format(getDate());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Double getZoneOffset() {
        return TDUtils.getTimezoneOffset(getDate().getTime(), mTimeZone);
    }
}
