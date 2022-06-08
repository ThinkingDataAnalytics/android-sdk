/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.thirdparty;

import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;
import java.lang.reflect.Method;

/**
 * < AbstractSyncThirdData Adjust版本 4.28.9>.
 *
 * @author thinker
 * @create 2022/01/01
 * @since 1.0.0
 */
public class AdjustSyncData extends AbstractSyncThirdData {

    public AdjustSyncData(String distinctId, String accountId) {
        super(distinctId, accountId);
    }

    @Override
    public void syncThirdPartyData() {
        TDLog.d(TAG, "开始同步Adjust数据");
        try {
            Class<?> mAdjustClazz = Class.forName("com.adjust.sdk.Adjust");
            Method addSessionParameterMethod = mAdjustClazz
                    .getMethod("addSessionCallbackParameter", String.class, String.class);
            addSessionParameterMethod
                    .invoke(null, TDConstants.TA_DISTINCT_ID, distinctId == null ? "" : distinctId);
            addSessionParameterMethod
                    .invoke(null, TDConstants.TA_ACCOUNT_ID, accountId == null ? "" : accountId);
        } catch (Exception e) {
            TDLog.e(TAG, "Adjust数据同步异常:" + e.getMessage());
        }
    }
}
