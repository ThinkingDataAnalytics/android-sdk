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
public class TAThirdPartyFactory {

    public static ISyncThirdPartyData create(int type, ThinkingAnalyticsSDK mInstance, String loginId, Map<String, Object> mCustomMap) {
        ISyncThirdPartyData syncData = null;
        switch (type) {
            case TDThirdPartyShareType.TD_APPS_FLYER:
                syncData = new AppsFlyerSyncData(mInstance.getDistinctId(), loginId, mCustomMap);
                break;
            case TDThirdPartyShareType.TD_IRON_SOURCE:
                syncData = new IronSourceSyncData(mInstance);
                break;
            case TDThirdPartyShareType.TD_ADJUST:
                syncData = new AdjustSyncData(mInstance.getDistinctId(), loginId);
                break;
            case TDThirdPartyShareType.TD_BRANCH:
                syncData = new BranchSyncData(mInstance.getDistinctId(), loginId);
                break;
            case TDThirdPartyShareType.TD_TOP_ON:
                syncData = new TopOnSyncData(mInstance.getDistinctId(), mCustomMap);
                break;
            case TDThirdPartyShareType.TD_TRACKING:
                syncData = new TrackingSyncData(mInstance.getDistinctId());
                break;
            case TDThirdPartyShareType.TD_TRAD_PLUS:
                syncData = new TradPlusSyncData(mInstance.getDistinctId());
                break;
            default:
                break;
        }
        return syncData;
    }
}
