package cn.thinkingdata.android.thirdparty;

import java.lang.reflect.Method;

import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;

/**
 * Adjust
 * adjust版本 4.28.9
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
            Method addSessionParameterMethod = mAdjustClazz.getMethod("addSessionCallbackParameter", String.class, String.class);
            addSessionParameterMethod.invoke(null, TDConstants.TA_DISTINCT_ID, distinctId == null ? "" : distinctId);
            addSessionParameterMethod.invoke(null, TDConstants.TA_ACCOUNT_ID, accountId == null ? "" : accountId);
        } catch (Exception e) {
            TDLog.e(TAG, "Adjust数据同步异常:" + e.getMessage());
        }
    }
}
