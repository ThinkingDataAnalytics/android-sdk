/*
 * Copyright (C) 2025 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

/**
 * @author liulongbing
 * @since 2025/3/11
 */
public class CustomTimestampFormatter {

    private static final int[] DAYS_IN_MONTH_NORMAL = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final int[] DAYS_IN_MONTH_LEAP = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    public static String formatTimestamp(long timestamp, double timeZoneOffsetHours) {
        // 计算时区偏移的毫秒数
        long offsetMillis = ( long ) timeZoneOffsetHours * 60 * 60 * 1000;
        // 加上时区偏移后的时间戳
        long adjustedTimestamp = timestamp + offsetMillis;

        // 提取秒数、分钟数、小时数、天数等信息
        long totalSeconds = adjustedTimestamp / 1000;
        long millis = adjustedTimestamp % 1000;
        long seconds = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes = totalMinutes % 60;
        long totalHours = totalMinutes / 60;
        long hours = totalHours % 24;
        long totalDays = totalHours / 24;

        // 从 1970 年 1 月 1 日开始计算年、月、日
        int year = 1970;
        while (true) {
            int daysInYear = isLeapYear(year) ? 366 : 365;
            if (totalDays < daysInYear) {
                break;
            }
            totalDays -= daysInYear;
            year++;
        }

        int month = 0;
        int[] daysInMonth = isLeapYear(year) ? DAYS_IN_MONTH_LEAP : DAYS_IN_MONTH_NORMAL;
        while (totalDays >= daysInMonth[month]) {
            totalDays -= daysInMonth[month];
            month++;
        }
        month++; // 月份从 1 开始
        int day = ( int ) (totalDays + 1);
        // 字符串拼接
        StringBuilder sb = new StringBuilder();
        sb.append(year);
        sb.append("-");
        if (month < 10) {
            sb.append("0");
        }
        sb.append(month);
        sb.append("-");
        if (day < 10) {
            sb.append("0");
        }
        sb.append(day);
        sb.append(" ");
        if (hours < 10) {
            sb.append("0");
        }
        sb.append(( int ) hours);
        sb.append(":");
        if (minutes < 10) {
            sb.append("0");
        }
        sb.append(( int ) minutes);
        sb.append(":");
        if (seconds < 10) {
            sb.append("0");
        }
        sb.append(( int ) seconds);
        sb.append(".");
        if (millis < 10) {
            sb.append("00");
        } else if (millis < 100) {
            sb.append("0");
        }
        sb.append(( int ) millis);
        return sb.toString();
    }


    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

}
