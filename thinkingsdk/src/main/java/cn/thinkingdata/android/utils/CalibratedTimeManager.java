/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.utils;

import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cn.thinkingdata.android.TDConfig;

/**
 * 时间校准管理
 * @author liulongbing
 * @create 2022/9/19
 * @since
 */
public class CalibratedTimeManager {

    private static ICalibratedTime sCalibratedTime;
    private static final ReentrantReadWriteLock sCalibratedTimeLock = new ReentrantReadWriteLock();

    private final TDConfig mConfig;

    public CalibratedTimeManager(TDConfig mConfig) {
        this.mConfig = mConfig;
    }

    // 获取当前时间的 ITime 实例
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

    // 获取与指定 date 和 timeZone 相关的 ITime 实例
    // 如果 timeZone 为 null, 则不会在事件中上传 #zone_offset 字段.
    public ITime getTime(Date date, TimeZone timeZone) {
        if (null == timeZone) {
            TDTime time = new TDTime(date, mConfig.getDefaultTimeZone());
            time.disableZoneOffset();
            return time;
        }
        return new TDTime(date, timeZone);
    }

    // 获取常量类型的 ITime 实例
    public ITime getTime(String timeString, Double zoneOffset) {
        return new TDTimeConstant(timeString, zoneOffset);
    }

    public static ICalibratedTime getCalibratedTime(){
        return sCalibratedTime;
    }

    /**
     * 校准时间.
     *
     * @param timestamp 当前时间戳
     */
    public static void calibrateTime(long timestamp) {
        setCalibratedTime(new TDCalibratedTime(timestamp));
    }

    /**
     * 使用指定的 NTP Server 校准时间.
     *
     * @param ntpServer NTP Server 列表
     */
    public static void calibrateTimeWithNtp(String... ntpServer) {
        if (null == ntpServer) {
            return;
        }
        setCalibratedTime(new TDCalibratedTimeWithNTP(ntpServer));
    }


    /**
     * 使用自定义的 ICalibratedTime 校准时间.
     *
     * @param calibratedTime ICalibratedTime 实例
     */
    private static void setCalibratedTime(ICalibratedTime calibratedTime) {
        sCalibratedTimeLock.writeLock().lock();
        sCalibratedTime = calibratedTime;
        sCalibratedTimeLock.writeLock().unlock();
    }

}
