/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;

/**
 * Local persistent enumeration
 *
 * @author liulongbing
 * @since 2022/9/7
 */
public class LocalStorageType {
    public static final int ENABLE = 0;

    public static final int FLUSH_SIZE = 1;

    public static final int FLUSH_INTERVAL = 2;
    public static final int IDENTIFY = 3;
    public static final int LAST_INSTALL = 4;
    public static final int LOGIN_ID = 5;
    public static final int OPT_OUT = 6;
    public static final int PAUSE_POST = 7;

    public static final int RANDOM_ID = 9;
    public static final int SUPER_PROPERTIES = 10;
    public static final int SESSION_ID =11;
    public static final int ENCRYPT_SUPPORT_GCM = 12;
    /** Current analytics session id as a decimal string. Distinct from {@link #SESSION_ID}. */
    public static final int ANALYTICS_SESSION_ID = 13;
    /** Epoch millis of the last event for the analytics session. */
    public static final int ANALYTICS_SESSION_LAST_EVENT_TIME = 14;
}
