package cn.thinkingdata.android;

import android.os.SystemClock;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

class EventTimer {
    EventTimer(TimeUnit timeUnit) {
        this.startTime = SystemClock.elapsedRealtime();
        this.timeUnit = timeUnit;
        this.eventAccumulatedDuration = 0;
    }

    String duration() {
        long duration = SystemClock.elapsedRealtime() - startTime + eventAccumulatedDuration;
        try {
            if (duration < 0 || duration > 24 * 60 * 60 * 1000) {
                return String.valueOf(0);
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
            return durationFloat < 0 ? String.valueOf(0) : String.format(Locale.CHINA, "%.3f", durationFloat);
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

    private final TimeUnit timeUnit;
    private long startTime;
    private long eventAccumulatedDuration;
}
