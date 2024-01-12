/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cn.thinkingdata.analytics.aop.push.TAPushUtils;
import cn.thinkingdata.core.router.plugin.IPlugin;
import cn.thinkingdata.core.router.plugin.MethodCall;
import cn.thinkingdata.ta_apt.TRoute;

/**
 * thinking analytics receiver module
 *
 * @author liulongbing
 * @since 2023/6/21
 */
@TRoute(path = "/thinkingdata/analytic")
public class ThinkingAnalyticsPlugin implements IPlugin {
    @Override
    public void onMethodCall(MethodCall call) {
        switch (call.method) {
            case "uploadPushToken":
                try {
                    JSONObject tokenJson = call.argument("tokenJson");
                    TAPushUtils.handlePushToken(tokenJson);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "uploadPushClick":
                try {
                    String ops = call.argument("ops_properties");
                    TAPushUtils.trackPushClickEvent(ops);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
