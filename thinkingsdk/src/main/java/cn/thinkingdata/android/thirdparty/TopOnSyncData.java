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
 * 同步TopOn数据.
 */
public class TopOnSyncData extends AbstractSyncThirdData {

    private final Map<String, Object> mCustomMap;

    public TopOnSyncData(String distinctId, Map<String, Object> mCustomMap) {
        super(distinctId);
        this.mCustomMap = mCustomMap;
    }

    @Override
    public void syncThirdPartyData() {
        try {
            Class<?> mATSDKClazz = Class.forName("com.anythink.core.api.ATSDK");
            Method initCustomMapMethod = mATSDKClazz.getMethod("initCustomMap", Map.class);
            Map<String, Object> params = new HashMap<>();
            Class<?> mATCustomRuleKeysClazz = Class.forName("com.anythink.core.api.ATCustomRuleKeys");
            Field mUserIdField = mATCustomRuleKeysClazz.getField("USER_ID");
            String userIdStr = (String) mUserIdField.get(null);
            params.put(userIdStr, distinctId == null ? "" : distinctId);
            //拼接外界的参数
            if (null != mCustomMap) {
                for (Map.Entry<String, Object> entry : mCustomMap.entrySet()) {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
            initCustomMapMethod.invoke(null, params);
        } catch (Exception e) {
            TDLog.e(TAG, "TopOn数据同步异常:" + e.getMessage());
        }
    }

}
