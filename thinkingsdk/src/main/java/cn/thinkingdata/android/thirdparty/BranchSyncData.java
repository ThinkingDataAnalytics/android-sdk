package cn.thinkingdata.android.thirdparty;

import java.lang.reflect.Method;

import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;

/**
 * Branch版本 5.+
 */
public class BranchSyncData extends AbstractSyncThirdData {

    public BranchSyncData(String distinctId, String accountId) {
        super(distinctId, accountId);
    }

    @Override
    public void syncThirdPartyData() {
        TDLog.d(TAG, "开始同步Branch数据");
        try {
            Class<?> mBranchClazz = Class.forName("io.branch.referral.Branch");
            Method getInstanceMethod = mBranchClazz.getMethod("getInstance");
            Object mBranchObj = getInstanceMethod.invoke(null);
            Method setRequestMetadataMethod = mBranchClazz.getMethod("setRequestMetadata", String.class, String.class);
            setRequestMetadataMethod.invoke(mBranchObj, TDConstants.TA_DISTINCT_ID, distinctId == null ? "" : distinctId);
            setRequestMetadataMethod.invoke(mBranchObj, TDConstants.TA_ACCOUNT_ID, accountId == null ? "" : accountId);
        } catch (Exception e) {
            TDLog.e(TAG, "Branch数据同步异常:" + e.getMessage());
        }
    }
}
