package cn.thinkingdata.android.thirdparty;

import java.lang.reflect.Method;

import cn.thinkingdata.android.utils.TDLog;

/**
 * 热云 1.8.2
 */
public class TrackingSyncData extends AbstractSyncThirdData{

    public TrackingSyncData(String distinctId) {
        super(distinctId);
    }

    @Override
    public void syncThirdPartyData() {
        TDLog.d(TAG, "开始同步热云数据");
        try {
            Class<?> mTrackingClazz = Class.forName("com.reyun.tracking.sdk.Tracking");
            Method setRegisterWithAccountIDMethod = mTrackingClazz.getMethod("setRegisterWithAccountID",String.class);
            setRegisterWithAccountIDMethod.invoke(null,distinctId == null ? "" : distinctId);
        } catch (Exception e) {
            TDLog.e(TAG, "Tracking数据同步异常:" + e.getMessage());
        }
    }

}