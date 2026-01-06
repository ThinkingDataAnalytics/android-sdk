/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.utils;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * time manager
 *
 * @author liulongbing
 * @since 2022/11/30
 */
public class TimeUtil {

    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    private static Map<String, ThreadLocal<SimpleDateFormat>> formatMaps = new HashMap<>();

    public static String formatDate(Date date, String patten, TimeZone timeZone) {
        if (TextUtils.isEmpty(patten)) {
            patten = TIME_PATTERN;
        }
        String formatString = "";
        SimpleDateFormat simpleDateFormat = getDateFormat(patten, timeZone);
        if (null == simpleDateFormat) {
            return formatString;
        }
        try {
            formatString = simpleDateFormat.format(date);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return formatString;
    }


    private synchronized static SimpleDateFormat getDateFormat(final String patten, final TimeZone timeZone) {
        String suffix = "";
        if (null != timeZone) {
            suffix = timeZone.getID();
        }
        String key = patten + "_" + suffix;
        ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = formatMaps.get(key);
        if (null == dateFormatThreadLocal) {
            dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
                @Override
                public SimpleDateFormat initialValue() {
                    SimpleDateFormat simpleDateFormat = null;
                    try {
                        simpleDateFormat = new SimpleDateFormat(patten, Locale.CHINA);
                        if (null != timeZone) {
                            simpleDateFormat.setTimeZone(timeZone);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return simpleDateFormat;
                }
            };
            if (null != dateFormatThreadLocal.get()) {
                formatMaps.put(key, dateFormatThreadLocal);
            }
        }
        return dateFormatThreadLocal.get();
    }

    public static Date getFormatDate(String dateStr, String patten, double timeZoneOffset) {
        if (TextUtils.isEmpty(patten)) {
            return new Date(0);
        }
        TimeZone timeZone = getTimeZone(timeZoneOffset);
        SimpleDateFormat simpleDateFormat = getDateFormat(patten, timeZone);
        if (null == simpleDateFormat) return new Date(0);
        try {
            return simpleDateFormat.parse(dateStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Date(0);
    }

    public static TimeZone getTimeZone(double timeZoneOffset) {
        int hour = ( int ) timeZoneOffset;
        int minute = ( int ) ((timeZoneOffset - hour) * 60);
        if (hour >= 0) {
            return TimeZone.getTimeZone(String.format(Locale.ROOT, "GMT+%02d:%02d", hour, minute));
        } else {
            return TimeZone.getTimeZone(String.format(Locale.ROOT, "GMT%02d:%02d", hour, minute));
        }
    }


}
