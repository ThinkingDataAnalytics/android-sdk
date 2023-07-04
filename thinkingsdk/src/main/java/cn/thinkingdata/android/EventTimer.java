/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android;

import android.os.SystemClock;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import cn.thinkingdata.android.utils.TDUtils;

class EventTimer {

    static final long MAX_DURATION = 24 * 60 * 60 * 1000;

    EventTimer(TimeUnit timeUnit) {
        this.startTime = SystemClock.elapsedRealtime();
        this.timeUnit = timeUnit;
        this.eventAccumulatedDuration = 0;
    }

    String duration() {
        long duration = SystemClock.elapsedRealtime() - startTime + eventAccumulatedDuration;
        return durationFormat(duration);
    }

    String backgroundDuration() {
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

    long getStartTime() {
        return startTime;
    }

    long getEventAccumulatedDuration() {
        return eventAccumulatedDuration;
    }

    void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    void setEventAccumulatedDuration(long eventAccumulatedDuration) {
        this.eventAccumulatedDuration = eventAccumulatedDuration;
    }

    void setBackgroundDuration(long backgroundDuration) {
        this.backgroundDuration = backgroundDuration;
    }

    long getBackgroundDuration() {
        return backgroundDuration;
    }

    private final TimeUnit timeUnit;
    private long startTime;
    private long eventAccumulatedDuration;
    private long backgroundDuration;
}
