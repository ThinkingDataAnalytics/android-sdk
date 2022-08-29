/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.thirdparty;

import cn.thinkingdata.android.ThinkingAnalyticsSDK;
import cn.thinkingdata.android.utils.TDLog;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import org.json.JSONObject;

/**
 * IronSource
 * ironSource版本 7.1.14
 */
public class IronSourceSyncData extends AbstractSyncThirdData {

    private final ThinkingAnalyticsSDK mThinkingSdk;

    public IronSourceSyncData(ThinkingAnalyticsSDK mThinkingSdk) {
        this.mThinkingSdk = mThinkingSdk;
    }

    @Override
    public void syncThirdPartyData() {
        TDLog.d(TAG, "开始同步IronSource数据");
        if (checkHasAddListener()) {
            TDLog.e(TAG, "IronSource数据已同步，无需重复调用");
            return;
        }
        try {
            Class<?> mIronSourceClazz = Class.forName("com.ironsource.mediationsdk.IronSource");
            Class<?> mImpressionDataListenerClazz = Class.forName(
                    "com.ironsource.mediationsdk.impressionData.ImpressionDataListener");
            Method addImpressionDataListenerMethod = mIronSourceClazz.getMethod(
                    "addImpressionDataListener", mImpressionDataListenerClazz);
            Object mDataHandlerObj = Proxy.newProxyInstance(mIronSourceClazz.getClassLoader(), new Class[]{mImpressionDataListenerClazz}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("onImpressionSuccess".equals(method.getName())
                            && args != null && args.length == 1) {
                        try {
                            Object mImpressionData = args[0];
                            Class<?> mImpressionDataClazz = Class.forName(
                                    "com.ironsource.mediationsdk.impressionData.ImpressionData");
                            Method mAllDataMethod = mImpressionDataClazz.getMethod("getAllData");
                            Object mJsonData = mAllDataMethod.invoke(mImpressionData);
                            if (mJsonData instanceof JSONObject) {
                                JSONObject json = (JSONObject) mJsonData;
                                if (mThinkingSdk != null) {
                                    mThinkingSdk.track(TAThirdConstants.IRON_SOURCE_EVENT_NAME, json);
                                }
                            }
                        } catch (Exception e) {
                            //ignored
                        }
                    }
                    return 0;
                }
            });
            addImpressionDataListenerMethod.invoke(null, mDataHandlerObj);
            TDLog.e(TAG, "IronSource数据同步成功");
        } catch (Exception e) {
            TDLog.e(TAG, "IronSource数据同步异常:" + e.getMessage());
        }
    }

    /**
     * 检查是否已经添加过监听.
     *
     * @return boolean
     */
    private boolean checkHasAddListener() {
        try {
            Class<?> mDataHolderClazz = Class.forName(
                    "com.ironsource.mediationsdk.IronsourceObjectPublisherDataHolder");
            Method getInstanceMethod = mDataHolderClazz.getMethod("getInstance");
            Object mDataHolderObj = getInstanceMethod.invoke(null);
            Method getImpressionListenerMethod
                    = mDataHolderClazz.getMethod("getImpressionDataListeners");
            Object mListenerObj = getImpressionListenerMethod.invoke(mDataHolderObj);
            if (mListenerObj instanceof HashSet) {
                HashSet<?> mListeners = (HashSet<?>) mListenerObj;
                for (Object mListener : mListeners) {
                    //如果发现监听里面存在代理 就说明已经添加
                    if (mListener.getClass().getName().startsWith("$Proxy")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }
}
