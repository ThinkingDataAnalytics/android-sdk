package cn.thinkingdata.android.utils;

import android.os.SystemClock;

import java.util.Date;

public final class TDCalibratedTime implements ICalibratedTime {
    private final long startTime;
    private final long mSystemElapsedRealtime;

    public TDCalibratedTime(long startTime) {
        this.startTime = startTime;
        mSystemElapsedRealtime = SystemClock.elapsedRealtime();
    }

    @Override
    public Date get(long systemElapsedRealtime) {
        return new Date(systemElapsedRealtime - this.mSystemElapsedRealtime + startTime);
    }
}
