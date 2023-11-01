/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.data;

import java.util.concurrent.TimeUnit;

import cn.thinkingdata.analytics.utils.TDUtils;

public class EventTimer {

    static final long MAX_DURATION = 24 * 60 * 60 * 1000;

    public EventTimer(TimeUnit timeUnit, long systemUpdateTime) {
        //this.startTime = SystemClock.elapsedRealtime();
        this.startTime = systemUpdateTime;
        this.timeUnit = timeUnit;
        this.eventAccumulatedDuration = 0;
    }

    public String duration(long systemUpdateTime) {
        // long duration = SystemClock.elapsedRealtime() - startTime + eventAccumulatedDuration;
        long duration = systemUpdateTime - startTime + eventAccumulatedDuration;
        return durationFormat(duration);
    }

    public String backgroundDuration() {
        return durationFormat(backgroundDuration);
    }

    String durationFormat(long duration) {
        try {
            if (duration < 0) {
                return String.valueOf(0);
            }
            if (duration > MAX_DURATION) {
                return durationFormat(MAX_DURATION);
            }
            float durationFloat;
            if (timeUnit == TimeUnit.MILLISECONDS) {
                durationFloat = duration;
            } else if (timeUnit == TimeUnit.SECONDS) {
                durationFloat = duration / 1000.0f;
            } else if (timeUnit == TimeUnit.MINUTES) {
                durationFloat = duration / 1000.0f / 60.0f;
            } else if (timeUnit == TimeUnit.HOURS) {
                durationFloat = duration / 1000.0f / 60.0f / 60.0f;
            } else {
                durationFloat = duration;
            }
//            return durationFloat < 0 ? String.valueOf(0)
//                    : String.format(Locale.CHINA, "%.3f", durationFloat);
            //String.format has poor performance
            return durationFloat < 0 ? String.valueOf(0)
                    : String.valueOf(TDUtils.formatNumberWithSpace(durationFloat, 3));
        } catch (Exception e) {
            e.printStackTrace();
            return String.valueOf(0);
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEventAccumulatedDuration() {
        return eventAccumulatedDuration;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEventAccumulatedDuration(long eventAccumulatedDuration) {
        this.eventAccumulatedDuration = eventAccumulatedDuration;
    }

    public void setBackgroundDuration(long backgroundDuration) {
        this.backgroundDuration = backgroundDuration;
    }

    public long getBackgroundDuration() {
        return backgroundDuration;
    }

    private final TimeUnit timeUnit;
    private long startTime;
    private long eventAccumulatedDuration;
    private long backgroundDuration;
}
