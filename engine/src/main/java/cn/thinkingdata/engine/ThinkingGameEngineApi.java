/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.engine;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import cn.thinkingdata.android.TDConfig;
import cn.thinkingdata.android.TDFirstEvent;
import cn.thinkingdata.android.TDOverWritableEvent;
import cn.thinkingdata.android.TDUpdatableEvent;
import cn.thinkingdata.android.ThinkingAnalyticsSDK;
import cn.thinkingdata.android.encrypt.TDSecreteKey;
import cn.thinkingdata.android.thirdparty.TDThirdPartyShareType;

/**
 * 游戏引擎处理类
 *
 * @author liulongbing
 * @create 2022/7/29
 * @since
 */
public class ThinkingGameEngineApi {

    private static Map<String, ThinkingAnalyticsSDK> sInstances = new HashMap<>();
    private static List<String> sAppIds = new ArrayList<>();

    /**
     * 设置lib 和version
     *
     * @param libName
     * @param libVersion
     */
    public void setCustomerLibInfo(String libName, String libVersion) {
        ThinkingAnalyticsSDK.setCustomerLibInfo(libName, libVersion);
    }

    /**
     * 是否开启日志
     *
     * @param enableLog
     */
    public void enableTrackLog(boolean enableLog) {
        ThinkingAnalyticsSDK.enableTrackLog(enableLog);
    }

    /**
     * 时间校准
     *
     * @param timeStampMillis
     */
    public void calibrateTime(double timeStampMillis) {
        ThinkingAnalyticsSDK.calibrateTime((long) timeStampMillis);
    }

    /**
     * ntp时间校准
     *
     * @param ntp_server
     */
    public void calibrateTimeWithNtp(String ntp_server) {
        ThinkingAnalyticsSDK.calibrateTimeWithNtp(ntp_server);
    }

    public void sharedInstance(Context context, String config) {
        try {
            JSONObject json = new JSONObject(config);
            String appId = json.optString("appId");
            String serverUrl = json.optString("serverUrl");
            if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(serverUrl)) return;
            TDConfig tdConfig = TDConfig.getInstance(context, appId, serverUrl);
            if (json.has("timeZone")) {
                String timeZoneId = json.optString("timeZone");
                tdConfig.setDefaultTimeZone(TimeZone.getTimeZone(timeZoneId));
            }
            if (json.has("mode")) {
                int mode = json.optInt("mode");
                tdConfig.setMode(TDConfig.ModeEnum.values()[mode]);
            }
            if (json.has("trackOldData")) {
                boolean trackOldData = json.optBoolean("trackOldData");
                tdConfig.setTrackOldData(trackOldData);
            }
            if (json.has("enableEncrypt")) {
                boolean enableEncrypt = json.optBoolean("enableEncrypt");
                tdConfig.enableEncrypt(enableEncrypt);
            }

            if (json.has("secretKey")) {
                JSONObject secretKey = json.optJSONObject("secretKey");
                if (null != secretKey) {
                    TDSecreteKey key = new TDSecreteKey();
                    key.publicKey = secretKey.optString("publicKey");
                    key.version = secretKey.optInt("version");
                    key.symmetricEncryption = secretKey.optString("symmetricEncryption");
                    key.asymmetricEncryption = secretKey.optString("asymmetricEncryption");
                    tdConfig.setSecretKey(key);
                }
            }

            ThinkingAnalyticsSDK instance = ThinkingAnalyticsSDK.sharedInstance(tdConfig);
            if (!sInstances.containsKey(appId)) {
                sInstances.put(appId, instance);
            }
            if (!sAppIds.contains(appId)) {
                sAppIds.add(appId);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 拿到当前的实例
     *
     * @param appId
     * @return
     */
    protected ThinkingAnalyticsSDK getCurrentInstance(String appId) {
        String currentAppId = appId;
        if (TextUtils.isEmpty(appId) && sAppIds.size() > 0) {
            currentAppId = sAppIds.get(0);
        }
        return sInstances.get(currentAppId);
    }

    /**
     * 统一安全检查
     *
     * @param obj
     * @return
     */
    protected ThinkingAnalyticsSDK safeCheck(String obj) {
        try {
            JSONObject json = new JSONObject(obj);
            String appId = json.optString("appId");
            if (TextUtils.isEmpty(appId) && sAppIds.size() > 0) {
                appId = sAppIds.get(0);
            }
            if (TextUtils.isEmpty(appId)) return null;
            ThinkingAnalyticsSDK ta = getCurrentInstance(appId);
            return ta;
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 发送日志
     *
     * @param obj
     */
    public void track(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("eventName")) {
                String eventName = json.optString("eventName");
                JSONObject properties = null;
                if (json.has("properties")) {
                    properties = json.optJSONObject("properties");
                }
                Date date = null;
                if (json.has("time")) {
                    double time = json.getDouble("time");
                    date = new Date((long) time);
                }
                TimeZone timeZone = null;
                if (json.has("timeZone")) {
                    String zone = json.optString("timeZone");
                    timeZone = TimeZone.getTimeZone(zone);
                }
                ta.track(eventName, properties, date, timeZone);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 首次事件
     *
     * @param obj
     */
    public void trackEvent(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("eventName")) {
                String eventName = json.optString("eventName");
                JSONObject properties = null;
                if (json.has("properties")) {
                    properties = json.optJSONObject("properties");
                }
                String eventId = null;
                if (json.has("eventId")) {
                    eventId = json.optString("eventId");
                }
                Date date = null;
                if (json.has("time")) {
                    double time = json.getDouble("time");
                    date = new Date((long) time);
                }
                TimeZone timeZone = null;
                if (json.has("timeZone")) {
                    String zone = json.optString("timeZone");
                    timeZone = TimeZone.getTimeZone(zone);
                }
                int eventType = json.optInt("type");
                if (eventType == 0) {
                    TDFirstEvent firstEvent = new TDFirstEvent(eventName, properties);
                    firstEvent.setFirstCheckId(eventId);
                    firstEvent.setEventTime(date, timeZone);
                    ta.track(firstEvent);
                } else if (eventType == 1) {
                    TDUpdatableEvent updatableEvent = new TDUpdatableEvent(eventName, properties, eventId);
                    updatableEvent.setEventTime(date, timeZone);
                    ta.track(updatableEvent);
                } else if (eventType == 2) {
                    TDOverWritableEvent overWritableEvent = new TDOverWritableEvent(eventName, properties, eventId);
                    overWritableEvent.setEventTime(date, timeZone);
                    ta.track(overWritableEvent);
                }
            }
        } catch (Exception e) {

        }
    }

    /**
     * 事件计时
     *
     * @param obj
     */
    public void timeEvent(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("eventName")) {
                String eventName = json.optString("eventName");
                ta.timeEvent(eventName);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 登录
     *
     * @param obj
     */
    public void login(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("loginId")) {
                String loginId = json.optString("loginId");
                ta.login(loginId);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 登出
     */
    public void logout(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            ta.logout();
        } catch (Exception e) {

        }
    }

    /**
     * 设置访客ID
     */
    public void identify(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("distinctId")) {
                String distinctId = json.optString("distinctId");
                ta.identify(distinctId);
            }
        } catch (Exception e) {

        }
    }


    /**
     * 用户属性
     *
     * @param obj
     */
    public void userSet(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("properties")) {
                JSONObject properties = json.optJSONObject("properties");
                ta.user_set(properties);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 撤销用户属性
     *
     * @param obj
     */
    public void userUnset(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("properties")) {
                JSONArray properties = json.optJSONArray("properties");
                if (properties == null) return;
                String[] pros = new String[]{};
                for (int i = 0; i < properties.length(); i++) {
                    pros[i] = properties.getString(i);
                }
                ta.user_unset(pros);
            }
        } catch (Exception e) {

        }
    }

    /**
     * user_setOnce
     *
     * @param obj
     */
    public void userSetOnce(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("properties")) {
                JSONObject properties = json.optJSONObject("properties");
                ta.user_setOnce(properties);
            }
        } catch (Exception e) {

        }
    }

    /**
     * user_add
     *
     * @param obj
     */
    public void userAdd(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("properties")) {
                JSONObject properties = json.optJSONObject("properties");
                ta.user_add(properties);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 用户属性删除
     *
     * @param obj
     */
    public void userDel(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            ta.user_delete();
        } catch (Exception e) {

        }
    }

    /**
     * user_append
     *
     * @param obj
     */
    public void userAppend(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("properties")) {
                JSONObject properties = json.optJSONObject("properties");
                ta.user_append(properties);
            }
        } catch (Exception e) {

        }
    }

    /**
     * user_uniqAppend
     *
     * @param obj
     */
    public void userUniqAppend(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("properties")) {
                JSONObject properties = json.optJSONObject("properties");
                ta.user_uniqAppend(properties);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 设置公共属性
     *
     * @param obj
     */
    public void setSuperProperties(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("properties")) {
                JSONObject properties = json.optJSONObject("properties");
                ta.setSuperProperties(properties);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 删除公共属性某一个属性
     *
     * @param obj
     */
    public void unsetSuperProperty(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("property")) {
                String property = json.optString("property");
                ta.unsetSuperProperty(property);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 清除公共属性
     *
     * @param obj
     */
    public void clearSuperProperties(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            ta.clearSuperProperties();
        } catch (Exception e) {

        }
    }

    /**
     * flush
     *
     * @param obj
     */
    public void flush(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            ta.flush();
        } catch (Exception e) {

        }
    }

    /**
     * 开启自动采集
     *
     * @param obj
     */
    public void enableAutoTrack(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("autoTrack")) {
                JSONArray autoTrack = json.optJSONArray("autoTrack");
                if (autoTrack == null) return;
                List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
                for (int i = 0; i < autoTrack.length(); i++) {
                    if (TextUtils.equals(autoTrack.optString(i), "appStart")) {
                        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
                    } else if (TextUtils.equals(autoTrack.optString(i), "appEnd")) {
                        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
                    } else if (TextUtils.equals(autoTrack.optString(i), "appCrash")) {
                        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
                    } else if (TextUtils.equals(autoTrack.optString(i), "appInstall")) {
                        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
                    }
                }
                ta.enableAutoTrack(eventTypeList);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 三方数据同步
     */
    public void enableThirdPartySharing(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("types")) {
                JSONArray types = json.optJSONArray("types");
                if (types == null) return;
                int thirdTypes = 0;
                for (int i = 0; i < types.length(); i++) {
                    switch (types.optString(i)) {
                        case "AppsFlyer":
                            thirdTypes = thirdTypes | TDThirdPartyShareType.TD_APPS_FLYER;
                            break;
                        case "IronSource":
                            thirdTypes = thirdTypes | TDThirdPartyShareType.TD_IRON_SOURCE;
                            break;
                        case "Adjust":
                            thirdTypes = thirdTypes | TDThirdPartyShareType.TD_ADJUST;
                            break;
                        case "Branch":
                            thirdTypes = thirdTypes | TDThirdPartyShareType.TD_BRANCH;
                            break;
                        case "TopOn":
                            thirdTypes = thirdTypes | TDThirdPartyShareType.TD_TOP_ON;
                            break;
                        case "Tracking":
                            thirdTypes = thirdTypes | TDThirdPartyShareType.TD_TRACKING;
                            break;
                        case "TradPlus":
                            thirdTypes = thirdTypes | TDThirdPartyShareType.TD_TRAD_PLUS;
                            break;
                    }
                }
                ta.enableThirdPartySharing(thirdTypes);
            } else if (json.has("type")) {
                String type = json.optString("type");
                int thirdType = 0;
                switch (type) {
                    case "AppsFlyer":
                        thirdType = TDThirdPartyShareType.TD_APPS_FLYER;
                        break;
                    case "IronSource":
                        thirdType = TDThirdPartyShareType.TD_IRON_SOURCE;
                        break;
                    case "Adjust":
                        thirdType = TDThirdPartyShareType.TD_ADJUST;
                        break;
                    case "Branch":
                        thirdType = TDThirdPartyShareType.TD_BRANCH;
                        break;
                    case "TopOn":
                        thirdType = TDThirdPartyShareType.TD_TOP_ON;
                        break;
                    case "Tracking":
                        thirdType = TDThirdPartyShareType.TD_TRACKING;
                        break;
                    case "TradPlus":
                        thirdType = TDThirdPartyShareType.TD_TRAD_PLUS;
                        break;
                }
                Map<String, Object> maps = new HashMap<>();
                JSONObject params = json.optJSONObject("params");
                if (null != params) {
                    for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                        String key = it.next();
                        maps.put(key, params.opt(key));
                    }
                }
                ta.enableThirdPartySharing(thirdType, maps);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 设置数据上报状态
     *
     * @param obj
     */
    public void setTrackStatus(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("status")) {
                String status = json.optString("status");
                if (TextUtils.equals(status, "pause")) {
                    ta.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.PAUSE);
                } else if (TextUtils.equals(status, "stop")) {
                    ta.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.STOP);
                } else if (TextUtils.equals(status, "saveOnly")) {
                    ta.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.SAVE_ONLY);
                } else if (TextUtils.equals(status, "normal")) {
                    ta.setTrackStatus(ThinkingAnalyticsSDK.TATrackStatus.NORMAL);
                }
            }
        } catch (Exception e) {

        }
    }

    /**
     * 获取预置属性
     *
     * @param obj
     */
    public String getPresetProperties(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return null;
            return ta.getPresetProperties().toEventPresetProperties().toString();
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 获取公共属性
     *
     * @param obj
     * @return
     */
    public String getSuperProperties(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return null;
            return ta.getSuperProperties().toString();
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 获取访客ID
     *
     * @param obj
     * @return
     */
    public String getDistinctId(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return null;
            return ta.getDistinctId();
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 获取设备ID
     *
     * @param obj
     * @return
     */
    public String getDeviceId(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return null;
            return ta.getDeviceId();
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 设置自动采集事件公共属性
     *
     * @return
     */
    public void setAutoTrackProperties(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("autoTrack")) {
                JSONArray types = json.optJSONArray("autoTrack");
                JSONObject properties = json.optJSONObject("properties");
                if (null == types) return;
                List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypes = new ArrayList<>();
                for (int i = 0; i < types.length(); i++) {
                    if (TextUtils.equals(types.getString(i), "appStart")) {
                        eventTypes.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
                    } else if (TextUtils.equals(types.getString(i), "appEnd")) {
                        eventTypes.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
                    } else if (TextUtils.equals(types.getString(i), "appInstall")) {
                        eventTypes.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
                    } else if (TextUtils.equals(types.getString(i), "appCrash")) {
                        eventTypes.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
                    }
                }
                if (eventTypes.size() > 0) {
                    ta.setAutoTrackProperties(eventTypes, properties);
                }
            }
        } catch (Exception e) {

        }
    }

    /**
     * 创建轻实例
     * @param obj
     * @return
     */
    public String createLightInstance(String obj){
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return "";
            String token =  UUID.randomUUID().toString();
            ThinkingAnalyticsSDK instance = ta.createLightInstance();
            sInstances.put(token,instance);
            sAppIds.add(token);
            return token;
        } catch (Exception e) {

        }
        return "";
    }


}
