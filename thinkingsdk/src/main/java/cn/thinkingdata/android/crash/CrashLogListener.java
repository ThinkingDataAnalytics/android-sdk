/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.crash;

import java.io.File;

/**
 * < CrashLogListener >.
 *
 * @author bugliee
 * @create 2022/3/9
 * @since 1.0.0
 */
public interface CrashLogListener {

    /**
     * check whether there is a crash log file locally.
     *
     * @param logFile crashLog
     */
    void onFile(File logFile);
}
