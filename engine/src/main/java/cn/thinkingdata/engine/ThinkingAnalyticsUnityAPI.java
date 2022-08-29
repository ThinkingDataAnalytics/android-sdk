/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.engine;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.thinkingdata.android.ThinkingAnalyticsSDK;

/**
 * <  >.
 * unity 扩展方法
 *
 * @author liulongbing
 * @create 2022/8/1
 * @since
 */
public class ThinkingAnalyticsUnityAPI extends ThinkingGameEngineApi {

    /**
     * 设置网络状态
     *
     * @param obj
     */
    public void setNetworkType(String obj) {
        try {
            ThinkingAnalyticsSDK ta = safeCheck(obj);
            if (ta == null) return;
            JSONObject json = new JSONObject(obj);
            if (json.has("network_type")) {
                int network_type = json.optInt("network_type");
                ThinkingAnalyticsSDK.ThinkingdataNetworkType networkType;
                switch (network_type) {
                    case 0:
                        networkType = ThinkingAnalyticsSDK.ThinkingdataNetworkType.NETWORKTYPE_DEFAULT;
                        break;
                    case 1:
                        networkType = ThinkingAnalyticsSDK.ThinkingdataNetworkType.NETWORKTYPE_WIFI;
                        break;
                    case 2:
                        networkType = ThinkingAnalyticsSDK.ThinkingdataNetworkType.NETWORKTYPE_ALL;
                        break;
                    default:
                        return;
                }
                ta.setNetworkType(networkType);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 设置动态公共属性
     *
     * @param listener
     */
    public void setDynamicSuperPropertiesTrackerListener(String appId, DynamicSuperPropertiesTrackerListener listener) {
        ThinkingAnalyticsSDK ta = getCurrentInstance(appId);
        if (null == ta) return;
        ta.setDynamicSuperPropertiesTracker(new ThinkingAnalyticsSDK.DynamicSuperPropertiesTracker() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                try {
                    return new JSONObject(listener.getDynamicSuperPropertiesString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return new JSONObject();
            }
        });
    }

    /**
     * @param obj
     * @param listener
     */
    public void enableAutoTrack(String obj, AutoTrackEventTrackerListener listener) {
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
                ta.enableAutoTrack(eventTypeList, new ThinkingAnalyticsSDK.AutoTrackEventListener() {
                    @Override
                    public JSONObject eventCallback(ThinkingAnalyticsSDK.AutoTrackEventType autoTrackEventType, JSONObject jsonObject) {
                        int mType = 0;
                        if (autoTrackEventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_START) {
                            mType = 1;
                        } else if (autoTrackEventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL) {
                            mType = 1 << 5;
                        } else if (autoTrackEventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_END) {
                            mType = 1 << 1;
                        } else if (autoTrackEventType == ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH) {
                            mType = 1 << 4;
                        }
                        try {
                            return new JSONObject(listener.eventCallback(mType, jsonObject.toString()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return new JSONObject();
                    }
                });
            }
        } catch (Exception e) {

        }
    }

    public interface DynamicSuperPropertiesTrackerListener {
        /**
         * 获取动态公共属性.
         *
         * @return 动态公共属性 String
         */
        String getDynamicSuperPropertiesString();
    }

    /**
     * 自动采集事件回调接口[unity使用]
     */
    public interface AutoTrackEventTrackerListener {
        /**
         * 回调事件名称和当前属性并获取动态属性
         *
         * @return 动态属性 String
         */
        String eventCallback(int type, String properties);
    }


}
