package com.thinking.analyselibrary;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

interface IThinkingAnalyticsAPI {
    /**
     * 上传单个事件，只包含预置属性和已设置的公共属性
     * @param eventName 事件名称
     */
    void track(String eventName);

    /**
     * 上传事件及其相关属性
     * @param eventName 事件名称
     * @param properties 事件属性
     */
    void track(String eventName, JSONObject properties);

    /**
     * 上传事件，可以设定事件触发时间。 SDK 1.1.5 版本开始支持
     * @param eventName 事件名称
     * @param properties 事件属性
     * @param time 事件触发时间
     */
    void track(String eventName, JSONObject properties, Date time);

    /**
     * 记录事件时长，调用此方法开始计时，目标事件上传时停止计时，并在事件属性中加入#duration属性，单位为秒
     * @param eventName 目标事件名称
     */
    void timeEvent(String eventName);

    /**
     * 设置账号ID，每次设置会覆盖之前的值。不会上传登录事件
     * @param loginId 账号 ID
     */
    void login(String loginId);

    /**
     * 清空账号ID, 不会上传用户登出事件
     */
    void logout();

    /**
     * 设置访客ID，替换掉默认的 UUID 访客ID
     * @param identify
     */
    void identify(String identify);

    /**
     * 设置用户属性，如果属性已存在，则用新值替换原值
     * @param property 用户属性
     */
    void user_set(JSONObject property);

    /**
     * 设置单次用户属性，如果属性已经存在，则忽略新的属性值
     * @param property 用户属性
     */
    void user_setOnce(JSONObject property);

    /**
     * 对数值类型用户属性进行累加操作
     * @param property 用户属性，JSONObject
     */
    void user_add(JSONObject property);

    /**
     * 对数值类型用户属性进行累加操作，只设置一个属性
     * @param propertyName 属性名称
     * @param propertyValue 属性值，可为负数
     */
    void user_add(String propertyName, Number propertyValue);

    /**
     * 删除用户属性，但会保留已上传的事件数据。该操作不可逆，需要慎重使用
     */
    void user_delete();

    /**
     * 设置公共事件属性，之后上传的每个事件都会包含公共事件属性。公共事件属性会被保存，无需每次设置
     * @param superProperties 公共事件属性
     */
    void setSuperProperties(JSONObject superProperties);

    /**
     * 清除一条公共事件属性
     * @param superPropertyName 要清除的公共事件属性key
     */
    void unsetSuperProperty(String superPropertyName);

    /**
     * 清除所有公共事件属性
     */
    void clearSuperProperties();

    /**
     * 获得已设置的公共事件属性
     * @return JSONObejct 已设置的公共事件属性
     */
    JSONObject getSuperProperties();

    /**
     * 设置上传的网络条件，默认情况下，SDK 将会网络条件为在 3G、4G 及 Wifi 时上传数据
     * @param type 上传数据的网络类型
     */
    void setNetworkType(ThinkingAnalyticsSDK.ThinkingdataNetworkType type);

    /**
     * 开启自动采集事件功能
     * @param eventTypeList 枚举 AutoTrackEventType 的列表，表示需要开启的自动采集事件类型
     */
    void enableAutoTrack(List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList);

    /**
     * 开启自动采集 Fragment 浏览事件
     */
    void trackFragmentAppViewScreen();

    /**
     * 手动触发页面浏览事件上传
     * @param activity
     */
    void trackViewScreen(Activity activity);

    /**
     * 手动触发页面浏览事件上传
     * @param fragment
     */
    void trackViewScreen(android.app.Fragment fragment);

    /**
     * 手动触发页面浏览事件上传
     * @param fragment
     */
    void trackViewScreen(android.support.v4.app.Fragment fragment);

    /**
     * 自定义控件ID，如果不设置，默认使用 android:id
     * @param view 控件
     * @param viewID 控件ID
     */
    void setViewID(View view, String viewID);

    /**
     * 自定义 Dialog 控件ID，如果不设置，默认使用 android:id
     * @param view 控件
     * @param viewID Dialog 控件ID
     */
    void setViewID(android.app.Dialog view, String viewID);

    /**
     * 自定义 Dialog 控件ID，如果不设置，默认使用 android:id
     * @param view 控件
     * @param viewID Dialog 控件ID
     */
    void setViewID(android.support.v7.app.AlertDialog view, String viewID);

    /**
     * 自定义控件点击事件的属性
     * @param view 需要设置自定义属性的控件
     * @param properties 控件自定义属性
     */
    void setViewProperties(View view, JSONObject properties);

    /**
     * 忽略指定页面的自动采集事件，包括页面浏览和控件点击事件
     * @param activity 指定页面
     */
    void ignoreAutoTrackActivity(Class<?> activity);

    /**
     * 忽略多个页面的自动采集事件，包括页面浏览和控件点击事件
     * @param activitiesList 指定页面列表
     */
    void ignoreAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 忽略指定类型的控件点击事件
     * @param viewType 控件类型，如Dialog、CheckBox等
     */
    void ignoreViewType(Class viewType);

    /**
     * 忽略指定元素的点击事件
     * @param view 指定元素
     */
    void ignoreView(View view);

    void setJsBridge(WebView webView);
}
