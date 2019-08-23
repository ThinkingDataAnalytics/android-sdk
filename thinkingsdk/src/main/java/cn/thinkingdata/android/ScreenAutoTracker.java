package cn.thinkingdata.android;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 为页面浏览事件增加页面 URL 信息，以及其他自定义属性.
 *
 * 实现了此接口的 Activity 的 ta_app_view 事件将会携带 url 信息和自定义属性
 */
public interface ScreenAutoTracker {
    /**
     * 返回值作为当前页面的Url信息
     * 作为该页面的URL以及下个页面的Referrer
     * @return String 预置属性 #url 的值
     */
    String getScreenUrl();

    /**
     * 返回值为增加的自定义属性
     * @return JSONObject 页面浏览事件自定义属性
     * @throws JSONException JSONException
     */
    JSONObject getTrackProperties() throws JSONException;
}
