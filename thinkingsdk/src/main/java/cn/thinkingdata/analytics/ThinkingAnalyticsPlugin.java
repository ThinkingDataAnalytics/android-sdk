/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.analytics;
import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;
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
            case "updatePushToken":
                String token = call.argument("token");
                String user_language = call.argument("user_language");
                double local_zone = call.argument("local_zone");
                final String appId = call.argument("appId");
                final JSONObject json = new JSONObject();
                try {
                    json.put("token", token);
                    json.put("user_language", user_language);
                    json.put("local_zone", local_zone);
                    ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
                        @Override
                        public void process(ThinkingAnalyticsSDK instance) {
                            if (TextUtils.equals(instance.getToken(), appId)) {
                                instance.user_set(json);
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "uploadPushClick":
                final String appId1 = call.argument("appId");
                JSONObject ops = call.argument("ops_properties");
                final JSONObject json1 = new JSONObject();
                try {
                    json1.put("#ops_receipt_properties", ops);
                    ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
                        @Override
                        public void process(ThinkingAnalyticsSDK instance) {
                            if (TextUtils.equals(instance.getToken(), appId1)) {
                                instance.autoTrack("ops_push_click", json1);
                                instance.flush();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
