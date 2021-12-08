package cn.thinkingdata.android;

import android.content.Context;
import android.content.res.Resources;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.thinkingdata.android.utils.TDConstants;

public class TDPresetProperties {
    /**
     * 应用包名(当进程名和包名不一致时，返回进程名)
     */
    public String bundle_id;
    /**
     *手机SIM卡运营商信息，双卡双待时，默认获取主卡运营商信息
     */
    public String carrier;
    /**
     * 设备ID（设备的AndroidId）
     */
    public String device_id;
    /**
     * 设备型号
     */
    public String device_model;
    /**
     * 厂商信息
     */
    public String manufacture;
    /**
     * 网络类型
     */
    public String network_type;
    /**
     * 系统类型
     */
    public String os;
    /**
     * 系统版本号
     */
    public String os_version;
    /**
     * 屏幕高度
     */
    public int    screen_height;
    /**
     * 屏幕宽度
     */
    public int    screen_width;
    /**
     * 手机系统语言
     */
    public String system_language;
    /**
     * 时区偏移值
     * */
    public double zone_offset;
    /**
     * 应用版本号
     */
    public String app_version;
    /**
     * 安装时间
     * */
    public String install_time;
    /**
     * 是否为模拟器
     * */
    public boolean is_simulator;
    /**
     * ram使用情况
     * */
    public String ram;
    /**
     * disk使用情况
     * */
    public String disk;
    /**
     * fps
     * */
    public int fps;

    /**
     * 预置属性过滤列表
     */
    static final List<String> disableList = new ArrayList<>();

    private JSONObject presetProperties;
    public TDPresetProperties(JSONObject presetProperties)
    {
        this.presetProperties = presetProperties;
        if (!disableList.contains(TDConstants.KEY_BUNDLE_ID)){
            this.bundle_id = presetProperties.optString(TDConstants.KEY_BUNDLE_ID);
        }
        if (!disableList.contains(TDConstants.KEY_CARRIER)) {
            this.carrier = presetProperties.optString(TDConstants.KEY_CARRIER);
        }
        if (!disableList.contains(TDConstants.KEY_DEVICE_ID)) {
            this.device_id = presetProperties.optString(TDConstants.KEY_DEVICE_ID);
        }
        if (!disableList.contains(TDConstants.KEY_DEVICE_MODEL)) {
            this.device_model = presetProperties.optString(TDConstants.KEY_DEVICE_MODEL);
        }
        if (!disableList.contains(TDConstants.KEY_MANUFACTURER)) {
            this.manufacture = presetProperties.optString(TDConstants.KEY_MANUFACTURER);
        }
        if (!disableList.contains(TDConstants.KEY_NETWORK_TYPE)) {
            this.network_type = presetProperties.optString(TDConstants.KEY_NETWORK_TYPE);
        }
        if (!disableList.contains(TDConstants.KEY_OS)) {
            this.os = presetProperties.optString(TDConstants.KEY_OS);
        }
        if (!disableList.contains(TDConstants.KEY_OS_VERSION)) {
            this.os_version = presetProperties.optString(TDConstants.KEY_OS_VERSION);
        }
        if (!disableList.contains(TDConstants.KEY_SCREEN_HEIGHT)) {
            this.screen_height = presetProperties.optInt(TDConstants.KEY_SCREEN_HEIGHT);
        }
        if (!disableList.contains(TDConstants.KEY_SCREEN_WIDTH)) {
            this.screen_width = presetProperties.optInt(TDConstants.KEY_SCREEN_WIDTH);
        }
        if (!disableList.contains(TDConstants.KEY_SYSTEM_LANGUAGE)) {
            this.system_language = presetProperties.optString(TDConstants.KEY_SYSTEM_LANGUAGE);
        }
        if (!disableList.contains(TDConstants.KEY_ZONE_OFFSET)) {
            this.zone_offset = presetProperties.optDouble(TDConstants.KEY_ZONE_OFFSET);
        }
        if (!disableList.contains(TDConstants.KEY_APP_VERSION)) {
            this.app_version = presetProperties.optString(TDConstants.KEY_APP_VERSION);
        }
        if (!disableList.contains(TDConstants.KEY_INSTALL_TIME)) {
            this.install_time = presetProperties.optString(TDConstants.KEY_INSTALL_TIME);
        }
        if (!disableList.contains(TDConstants.KEY_SIMULATOR)) {
            this.is_simulator = presetProperties.optBoolean(TDConstants.KEY_SIMULATOR);
        }
        if (!disableList.contains(TDConstants.KEY_RAM)) {
            this.ram = presetProperties.optString(TDConstants.KEY_RAM);
        }
        if (!disableList.contains(TDConstants.KEY_DISK)) {
            this.disk = presetProperties.optString(TDConstants.KEY_DISK);
        }
        if (!disableList.contains(TDConstants.KEY_FPS)) {
            this.fps = presetProperties.optInt(TDConstants.KEY_FPS);
        }
    }

    /**
     * @return 生成事件预制属性，不支持把事件预制属性设置为用户预制属性
     */
    public JSONObject toEventPresetProperties()
    {
        return this.presetProperties;
    }

    public TDPresetProperties(){}

    /**
     * 初始化静态属性配置
     */
    static void initDisableList(Context context) {
        synchronized (disableList) {
            if (disableList.isEmpty()) {
                Resources resources = context.getResources();
                String[] array = resources.getStringArray(R.array.TDDisPresetProperties);
                disableList.addAll(Arrays.asList(array));
            }
        }
    }
}