/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.thirdparty;

import java.util.Map;

import cn.thinkingdata.android.ThinkingAnalyticsSDK;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/7/25
 * @since
 */
public class TAThirdPartyManager {

    /**
     * 同步三方数据 支持位运算
     *
     * @param types
     */
    public static void enableThirdPartySharing(int types, ThinkingAnalyticsSDK instance, String loginId) {
        if ((types & TDThirdPartyShareType.TD_APPS_FLYER) > 0) {
            enableThirdPartySharing(TDThirdPartyShareType.TD_APPS_FLYER, instance, loginId, null);
        }
        if ((types & TDThirdPartyShareType.TD_IRON_SOURCE) > 0) {
            enableThirdPartySharing(TDThirdPartyShareType.TD_IRON_SOURCE, instance, loginId, null);
        }
        if ((types & TDThirdPartyShareType.TD_ADJUST) > 0) {
            enableThirdPartySharing(TDThirdPartyShareType.TD_ADJUST, instance, loginId, null);
        }
        if ((types & TDThirdPartyShareType.TD_BRANCH) > 0) {
            enableThirdPartySharing(TDThirdPartyShareType.TD_BRANCH, instance, loginId, null);
        }
        if ((types & TDThirdPartyShareType.TD_TOP_ON) > 0) {
            enableThirdPartySharing(TDThirdPartyShareType.TD_TOP_ON, instance, loginId, null);
        }
        if ((types & TDThirdPartyShareType.TD_TRACKING) > 0) {
            enableThirdPartySharing(TDThirdPartyShareType.TD_TRACKING, instance, loginId, null);
        }
        if ((types & TDThirdPartyShareType.TD_TRAD_PLUS) > 0) {
            enableThirdPartySharing(TDThirdPartyShareType.TD_TRAD_PLUS, instance, loginId, null);
        }
    }

    /**
     * 同步三方数据 单个 支持设置额外参数
     *
     * @param type
     * @param mCustomMap
     */
    public static void enableThirdPartySharing(int type, ThinkingAnalyticsSDK instance, String loginId, Map<String, Object> mCustomMap) {
        ISyncThirdPartyData syncData = TAThirdPartyFactory.create(type, instance, loginId, mCustomMap);
        if (null != syncData) {
            syncData.syncThirdPartyData();
        }
    }
}
