package cn.thinkingdata.android;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
     * 上传事件，可以设定事件触发时间。 SDK 1.1.5 版本开始支持. 该方法在 v2.2.0+ 被废弃
     *
     * time 将按照设备默认时区被序列化为指定格式的字符串上报，但是 #zone_offset 将不会被设置
     *
     * @param eventName 事件名称
     * @param properties 事件属性
     * @param time 事件触发时间
     */
    @Deprecated
    void track(String eventName, JSONObject properties, Date time);

    /**
     * 上传事件，并设定事件触发时间。SDK v2.2.0 开始支持.
     * @param eventName 事件名称
     * @param properties 事件属性
     * @param time 事件触发时间
     * @param timeZone 事件时区
     */
    void track(String eventName, JSONObject properties, Date time, final TimeZone timeZone);

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
     * 设置访客 ID，替换掉默认的 UUID 访客 ID
     * @param identify 字符串类型的访客 ID.
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
     * 对 List 类型的用户属性进行追加操作
     * @param property 用户属性，JSONObject
     */
    void user_append(JSONObject property);

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
     * 重置用户属性
     * @param properties
     */
    void user_unset(String... properties);

    /**
     * 设置公共事件属性，之后上传的每个事件都会包含公共事件属性。公共事件属性会被保存，无需每次设置
     * @param superProperties 公共事件属性
     */
    void setSuperProperties(JSONObject superProperties);

    /**
     * 设置动态公共属性。之后上传的每个事件都会包含公共事件属性
     * @param dynamicSuperPropertiesTracker 动态公共属性接口
     */
    void setDynamicSuperPropertiesTracker(ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker dynamicSuperPropertiesTracker);

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
     * 获取访客 ID: 上报数据中的 #distinct_id 值
     * @return 访客 ID
     */
    String getDistinctId();

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
     * 开启采集安装事件. added for unity3D.
     */
    void trackAppInstall();

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
     * @param fragment 需要采集的 Fragment 实例.
     */
    void trackViewScreen(android.app.Fragment fragment);

    /**
     * 手动触发页面浏览事件上传.
     * @param fragment 需要采集的 fragment 实例. 支持 support 库和 androidx 库的 Fragment.
     */
    void trackViewScreen(Object fragment);

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
     * 清空缓存队列. 当调用此函数时，会将目前缓存队列中的数据尝试上报. 如果上报成功会删除本地缓存数据.
     */
    void flush();

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

    /**
     * 支持 H5 与原生 APP SDK 打通. 在 WebView 初始化时调用此函数.
     * @param webView 传入 WebView 实例
     */
    void setJsBridge(WebView webView);

    /**
     * 腾讯 X5 WebView 与原生 APP SDK 打通.
     * @param x5WebView WebView 实例
     */
    void setJsBridgeForX5WebView(Object x5WebView);

    /**
     * 获取设备ID
     * @return 设备ID
     */
    String getDeviceId();

    /**
     * 打开/关闭 实例功能. 当关闭 SDK 功能时，之前的缓存数据会保留，并继续上报; 但是不会追踪之后的数据和改动.
     * @param enabled true 打开上报; false 关闭上报
     */
    void enableTracking(boolean enabled);

    /**
     * 停止上报此用户数据，调用此接口之后，会删除本地缓存数据和之前设置; 后续的上报和设置都无效. 并且发送 user_del (不会重试)
     */
    void optOutTrackingAndDeleteUser();

    /**
     * 停止上报此用户的数据. 调用此接口之后，会删除本地缓存数据和之前设置; 后续的上报和设置都无效.
     */
    void optOutTracking();

    /**
     * 开启此实例的上报.
     */
    void optInTracking();

    /**
     * 创建轻量级的 SDK 实例. 轻量级的 SDK 实例不支持缓存本地账号ID，访客ID，公共属性等.
     * @return SDK 实例
     */
    IThinkingAnalyticsAPI createLightInstance();
}
