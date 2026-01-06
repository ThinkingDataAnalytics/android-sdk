package cn.thinkingdata.core.router.provider;

import android.util.Pair;

import java.util.Map;

public interface IAnalyticsProvider extends IProvider {

    String getLoginId(String name);

    String getDistinctId(String name);

    Map<String, Object> getAnalyticsProperties(String name);

    Pair<Long,Boolean> getCurrentTimeStamp();

}
