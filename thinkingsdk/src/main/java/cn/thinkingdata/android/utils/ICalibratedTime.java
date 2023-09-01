/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.utils;

import java.util.Date;

public interface ICalibratedTime {

    /**
     * Get the time after calibration
     *
     * @param elapsedRealtime System Up Time
     * @return 
     */
    Date get(long elapsedRealtime);
}
