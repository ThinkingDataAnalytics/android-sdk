/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.utils;

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
 * @create 2022/11/30
 * @since
 */
public class TimeUtil {

    private static Map<String, ThreadLocal<SimpleDateFormat>> formatMaps = new HashMap<>();

    public static String formatDate(Date date, String patten, TimeZone timeZone) {
        if (TextUtils.isEmpty(patten)) {
            patten = TDConstants.TIME_PATTERN;
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
                protected SimpleDateFormat initialValue() {
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

}
