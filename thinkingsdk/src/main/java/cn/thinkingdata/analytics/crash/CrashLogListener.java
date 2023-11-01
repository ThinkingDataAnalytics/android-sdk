/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.crash;

import java.io.File;

/**
 * CrashLogListener
 *
 * @author bugliee
 * @since 2022/3/9
 */
public interface CrashLogListener {

    /**
     * check whether there is a crash log file locally.
     *
     * @param logFile crashLog
     */
    void onFile(File logFile);
}
