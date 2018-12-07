package com.thinking.analyselibrarysv;

import org.json.JSONException;
import org.json.JSONObject;

public interface ScreenAutoTrackerSV {
    /**
     * 返回当前页面的Url
     * 用作下个页面的referrer
     * @return String
     */
    String getScreenUrl();

    /**
     * 返回自定义属性集合
     * 我们内置了一个属性:$screen_name,代表当前页面名称, 默认情况下,该属性会采集当前Activity的CanonicalName,即:
     * activity.getClass().getCanonicalName(), 如果想自定义页面名称, 可以在Map里put该key进行覆盖。
     * 注意:screen_name的前面必须要要加"$"符号
     *
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getTrackProperties() throws JSONException;
}
