/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cn.thinkingdata.analytics.TDConfig;
import cn.thinkingdata.core.receiver.TDAnalyticsObservable;

/**
 * Time calibration management
 *
 * @author liulongbing
 * @since 2022/9/19
 */
public class CalibratedTimeManager {

    private static final String TAG = "ThinkingAnalytics.NTP";

    private static final int DEFAULT_TIME_OUT = 3000;
    private final String[] ntpServer = new String[]{"time.apple.com"};

    private static ICalibratedTime sCalibratedTime;
    private static final ReentrantReadWriteLock sCalibratedTimeLock = new ReentrantReadWriteLock();

    private final TDConfig mConfig;

    public CalibratedTimeManager(TDConfig mConfig) {
        this.mConfig = mConfig;
//        if (!TDPresetProperties.disableList.contains(TDConstants.KEY_CALIBRATION_TYPE)) {
//            Thread mCalibrationThread = new Thread(new Runnable() {
//                final TDNTPClient ntpClient = new TDNTPClient();
//
//                @Override
//                public void run() {
//                    for (String s : ntpServer) {
//                        if (ntpClient.requestTime(s, DEFAULT_TIME_OUT)) {
//                            TDLog.i(TAG, "NTP offset from " + s + " is: " + ntpClient.getOffset());
//                            long startTime = System.currentTimeMillis() + ntpClient.getOffset();
//                            calibrateTime(startTime);
//                            break;
//                        }
//                    }
//                }
//            });
//            mCalibrationThread.start();
//        }
    }

    // Gets an ITime instance of the current time
    public ITime getTime() {
        ITime result;
        sCalibratedTimeLock.readLock().lock();
        if (null != sCalibratedTime) {
            result = new TDTimeCalibrated(sCalibratedTime, mConfig.getDefaultTimeZone());
        } else {
            result = new TDTime(new Date(), mConfig.getDefaultTimeZone());
        }
        sCalibratedTimeLock.readLock().unlock();
        return result;
    }

    public double getSyncTimeZoneOffset() {
        Double result;
        sCalibratedTimeLock.readLock().lock();
        if (null != sCalibratedTime) {
            result = new TDTimeCalibrated(sCalibratedTime, mConfig.getDefaultTimeZone()).getSyncZoneOffset();
        } else {
            result = new TDTime(new Date(), mConfig.getDefaultTimeZone()).getZoneOffset();
        }
        sCalibratedTimeLock.readLock().unlock();
        return result;
    }

    // Gets the ITime instance associated with the specified date and timeZone
    //  If timeZone is null, the #zone_offset field will not be uploaded in the event.
    public ITime getTime(Date date, TimeZone timeZone) {
        if (null == timeZone) {
            TDTime time = new TDTime(date, mConfig.getDefaultTimeZone());
            time.disableZoneOffset();
            return time;
        }
        TDTime t = new TDTime(date, timeZone);
        t.setCalibrationDisuse(true);
        return t;
    }

    // Gets an ITime instance of a constant type
    public ITime getTime(String timeString, Double zoneOffset) {
        return new TDTimeConstant(timeString, zoneOffset);
    }

    public static ICalibratedTime getCalibratedTime() {
        return sCalibratedTime;
    }

    /**
     * time calibration
     *
     * @param timestamp current timestamp
     */
    public static void calibrateTime(long timestamp) {
        setCalibratedTime(new TDCalibratedTime(timestamp));
        TDAnalyticsObservable.getInstance().onTimeCalibrated();
    }

    /**
     * Use the specified NTP Server calibration time.
     *
     * @param ntpServer NTP Server list
     */
    public static void calibrateTimeWithNtp(String... ntpServer) {
        if (null == ntpServer) {
            return;
        }
        setCalibratedTime(new TDCalibratedTimeWithNTP(ntpServer));
    }


    /**
     * Use the custom ICalibratedTime calibration time.
     *
     * @param calibratedTime ICalibratedTime instance
     */
    private static void setCalibratedTime(ICalibratedTime calibratedTime) {
        sCalibratedTimeLock.writeLock().lock();
//        if (sCalibratedTime == null) {
//            sCalibratedTime = calibratedTime;
//        }
        sCalibratedTime = calibratedTime;
        sCalibratedTimeLock.writeLock().unlock();
    }

}
