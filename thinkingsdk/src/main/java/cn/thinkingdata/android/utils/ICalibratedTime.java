package cn.thinkingdata.android.utils;

import java.util.Date;

/**
 * 校准的时间.
 */
public interface ICalibratedTime {
    /**
     * 获取校准后的时间
     * @param elapsedRealtime 系统开机时间，单位毫秒
     * @return 校准后的时间
     */
    Date get(long elapsedRealtime);
}
