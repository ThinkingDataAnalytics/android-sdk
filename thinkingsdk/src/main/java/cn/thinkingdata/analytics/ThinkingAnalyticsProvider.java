/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.text.TextUtils;
import android.util.Pair;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.analytics.data.SystemInformation;
import cn.thinkingdata.analytics.utils.ITime;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.analytics.utils.TDTimeCalibrated;
import cn.thinkingdata.core.router.TRouterMap;
import cn.thinkingdata.core.router.provider.IAnalyticsProvider;
import cn.thinkingdata.core.utils.TimeUtil;
import cn.thinkingdata.ta_apt.TRoute;

/**
 * @author liulongbing
 * @since 2024/1/12
 */
@TRoute(path = TRouterMap.ANALYTIC_PROVIDER_ROUTE_PATH)
public class ThinkingAnalyticsProvider implements IAnalyticsProvider {

    @Override
    public String getLoginId(final String name) {
        ThinkingAnalyticsSDK instance = TDAnalyticsAPI.getInstance(name);
        if (null == instance) {
            instance = ThinkingAnalyticsSDK.getInstanceByAppId(name);
        }
        if (null != instance) {
            return instance.getLoginId(false);
        }
        return "";
    }

    @Override
    public String getDistinctId(String name) {
        ThinkingAnalyticsSDK instance = TDAnalyticsAPI.getInstance(name);
        if (null == instance) {
            instance = ThinkingAnalyticsSDK.getInstanceByAppId(name);
        }
        if (null != instance) {
            return instance.getDistinctId();
        }
        return "";
    }

    @Override
    public Map<String, Object> getAnalyticsProperties(String name) {
        Map<String, Object> maps = new HashMap<>();
        ThinkingAnalyticsSDK instance = TDAnalyticsAPI.getInstance(name);
        if (null == instance) {
            instance = ThinkingAnalyticsSDK.getInstanceByAppId(name);
        }
        if (null != instance) {
            maps.put(TDConstants.KEY_ZONE_OFFSET, instance.mCalibratedTimeManager.getTime().getZoneOffset());
            Map<String, Object> deviceInfo = SystemInformation.getInstance(instance.mConfig.mContext).getDeviceInfo();
            maps.put(TDConstants.KEY_INSTALL_TIME, deviceInfo.get(TDConstants.KEY_INSTALL_TIME));
            maps.put(TDConstants.KEY_LIB, deviceInfo.get(TDConstants.KEY_LIB));
            maps.put(TDConstants.KEY_LIB_VERSION, deviceInfo.get(TDConstants.KEY_LIB_VERSION));
        }
        return maps;
    }

    @Override
    public Pair<Long, Boolean> getCurrentTimeStamp() {
        ThinkingAnalyticsSDK instance = TDAnalyticsAPI.getInstance("");
        if (null == instance) {
            instance = ThinkingAnalyticsSDK.getInstanceByAppId("");
        }
        if (null != instance) {
            ITime iTime = instance.mCalibratedTimeManager.getTime();
            if (iTime instanceof TDTimeCalibrated) {
                return new Pair<>((( TDTimeCalibrated ) iTime).getDate().getTime(), true);
            }
        }
        return new Pair<>(new Date().getTime(), false);
    }
}
