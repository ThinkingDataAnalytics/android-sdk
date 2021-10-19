package cn.thinkingdata.android;

import org.json.JSONException;
import org.json.JSONObject;

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
    private JSONObject presetProperties;
    public TDPresetProperties(JSONObject presetProperties)
    {
        this.presetProperties = presetProperties;
        this.bundle_id = presetProperties.optString(TDConstants.KEY_BUNDLE_ID);
        this.carrier = presetProperties.optString(TDConstants.KEY_CARRIER);
        this.device_id = presetProperties.optString(TDConstants.KEY_DEVICE_ID);
        this.device_model = presetProperties.optString(TDConstants.KEY_DEVICE_MODEL);
        this.manufacture = presetProperties.optString(TDConstants.KEY_MANUFACTURER);
        this.network_type = presetProperties.optString(TDConstants.KEY_NETWORK_TYPE);
        this.os = presetProperties.optString(TDConstants.KEY_OS);
        this.os_version = presetProperties.optString(TDConstants.KEY_OS_VERSION);
        this.screen_height = presetProperties.optInt(TDConstants.KEY_SCREEN_HEIGHT);
        this.screen_width = presetProperties.optInt(TDConstants.KEY_SCREEN_WIDTH);
        this.system_language = presetProperties.optString(TDConstants.KEY_SYSTEM_LANGUAGE);
        this.zone_offset = presetProperties.optDouble(TDConstants.KEY_ZONE_OFFSET);
    }

    /**
     * @return 生成事件预制属性，不支持把事件预制属性设置为用户预制属性
     */
    public JSONObject toEventPresetProperties()
    {
        return this.presetProperties;
    }

    public TDPresetProperties(){}
}