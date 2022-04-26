package cn.thinkingdata.android.thirdparty;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;

/**
 * 同步AF数据
 * af版本 6.3.2
 */
public class AppsFlyerSyncData extends AbstractSyncThirdData {

    private Map<String, Object> mCustomMap;

    public AppsFlyerSyncData(String distinctId, String accountId, Map<String, Object> mCustomMap) {
        super(distinctId, accountId);
        this.mCustomMap = mCustomMap;
    }

    @Override
    public void syncThirdPartyData() {
        TDLog.d(TAG, "开始同步Appsflyer数据");
        try {
            Map<String, Object> maps = new HashMap<>();
            maps.put(TDConstants.TA_DISTINCT_ID, distinctId == null ? "" : distinctId);
            maps.put(TDConstants.TA_ACCOUNT_ID, accountId == null ? "" : accountId);
            if (null != mCustomMap) {
                for (Map.Entry<String, Object> entry : mCustomMap.entrySet()) {
                    maps.put(entry.getKey(), entry.getValue());
                }
            }
            Class<?> mAppsFlyerClazz = Class.forName("com.appsflyer.AppsFlyerLib");
            Method getInstanceMethod = mAppsFlyerClazz.getMethod("getInstance");
            //拿到AppsFlyerLib实例
            Object afObject = getInstanceMethod.invoke(null);
            Method mSetAdditionalDataMethod = mAppsFlyerClazz.getMethod("setAdditionalData", Map.class);
            mSetAdditionalDataMethod.invoke(afObject, maps);
        } catch (Exception e) {
            TDLog.e(TAG, "AppsFlyer数据同步异常:" + e.getMessage());
        }
    }

}
