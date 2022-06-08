/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.thirdparty;

import cn.thinkingdata.android.utils.TDLog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * TradPlus版本 7.4.10.1
 */
public class TradPlusSyncData extends AbstractSyncThirdData {

    public TradPlusSyncData(String distinctId) {
        super(distinctId);
    }

    @Override
    public void syncThirdPartyData() {
        TDLog.d(TAG, "开始同步TradPlus数据");
        try {
            Class<?> mSegmentClazz = Class.forName("com.tradplus.ads.mobileads.util.SegmentUtils");
            Method initCustomMapMethod = mSegmentClazz.getMethod("initCustomMap", Map.class);
            Map<String, String> params = new HashMap<>();
            Class<?> mAppKeyManagerClazz = Class.forName("com.tradplus.ads.mobileads.util.AppKeyManager");
            Field mUserIdField = mAppKeyManagerClazz.getField("CUSTOM_USERID");
            String userIdStr = (String) mUserIdField.get(null);
            params.put(userIdStr, distinctId == null ? "" : distinctId);
            initCustomMapMethod.invoke(null, params);
        } catch (Exception e) {
            TDLog.e(TAG, "TradPlus数据同步异常:" + e.getMessage());
        }
    }

}
