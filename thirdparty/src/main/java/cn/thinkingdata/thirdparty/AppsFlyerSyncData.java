/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.thirdparty;

import cn.thinkingdata.android.utils.TDLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * < AppsFlyerSyncData 同步AF数据 AF版本6.3.2>.
 *
 * @author thinker
 * @create 2022/01/01
 * @since 1.0.0
 */
public class AppsFlyerSyncData extends AbstractSyncThirdData {

    private final Map<String, Object> mCustomMap;

    public AppsFlyerSyncData(String distinctId, String accountId, Map<String, Object> mCustomMap) {
        super(distinctId, accountId);
        this.mCustomMap = mCustomMap;
    }

    @Override
    public void syncThirdPartyData() {
        TDLog.d(TAG, "开始同步Appsflyer数据");
        Map<String, Object> maps = new HashMap<>();
        maps.put(TAThirdConstants.TA_DISTINCT_ID, distinctId == null ? "" : distinctId);
        maps.put(TAThirdConstants.TA_ACCOUNT_ID, accountId == null ? "" : accountId);
        if (null != mCustomMap) {
            for (Map.Entry<String, Object> entry : mCustomMap.entrySet()) {
                maps.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            Class<?> mAppsFlyerClazz = Class.forName("com.appsflyer.AppsFlyerLib");
            Method getInstanceMethod = mAppsFlyerClazz.getMethod("getInstance");
            //拿到AppsFlyerLib实例
            Object afObject = getInstanceMethod.invoke(null);
            Method mSetAdditionalDataMethod
                    = mAppsFlyerClazz.getMethod("setAdditionalData", Map.class);
            mSetAdditionalDataMethod.invoke(afObject, maps);
            TDLog.d(TAG, "AppsFlyer数据同步成功");
        } catch (NoSuchMethodException e) {
            TDLog.e(TAG, "AppsFlyer数据同步异常:" + e.getMessage());
            syncThirdPartyData5(maps);
        } catch (Exception e) {
            TDLog.e(TAG, "AppsFlyer数据同步异常:" + e.getMessage());
        }
    }

    /**
     * 适配5.x版本
     * @param maps
     */
    private void syncThirdPartyData5(Map<String, Object> maps) {
        TDLog.d(TAG, "重新开始同步Appsflyer数据");
        try {
            Class<?> mAppsFlyerClazz = Class.forName("com.appsflyer.AppsFlyerLib");
            Method getInstanceMethod = mAppsFlyerClazz.getMethod("getInstance");
            //拿到AppsFlyerLib实例
            Object afObject = getInstanceMethod.invoke(null);
            Method mSetAdditionalDataMethod
                    = mAppsFlyerClazz.getMethod("setAdditionalData", HashMap.class);
            mSetAdditionalDataMethod.invoke(afObject, (HashMap<String, Object>) maps);
            TDLog.d(TAG, "同步Appsflyer数据成功");
        } catch (Exception e) {
            TDLog.e(TAG, "AppsFlyer数据同步异常:" + e.getMessage());
        }
    }

}
