/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.aop.push;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import cn.thinkingdata.android.ThinkingAnalyticsSDK;
import cn.thinkingdata.android.ThinkingDataRuntimeBridge;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/5/31
 * @since
 */
public class TAPushUtils {

    private static final String TA_PUSH_CLICK_EVENT = "ops_push_click";
    private static final List<JSONObject> pushList = new ArrayList<>();
    public static List<String> gtMsgList = new ArrayList<>();

    public static void handleStartIntent(Intent intent) {
        // huawei oppo vivo fcm
        if (TAPushUtils.handleBundleExtraData(intent)) return;
        // meizu
        if (TAPushUtils.handleIntentExtraData(intent)) return;
        //jpush
        if (handleJPushIntentData(intent)) return;
        // xiaomi
        TAPushUtils.handleMiPushData(intent);
    }

    /**
     * handle mi push data
     *
     * @param intent
     */
    public static void handleMiPushData(Intent intent) {
        if (null == intent) return;
        try {
            Object miPushObj = intent.getSerializableExtra("key_message");
            if (null != miPushObj) {
                Class<?> miPushMessageClazz = miPushObj.getClass();
                Method getExtrasMethod = miPushMessageClazz.getMethod("getExtra");
                Object miObj = getExtrasMethod.invoke(miPushObj);
                if (miObj instanceof Map) {
                    Map<String, String> miMsg = ( Map<String, String> ) miObj;
                    String te_extras = ( String ) miMsg.get("te_extras");
                    trackPushClickEvent(te_extras);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * push data in extras
     *
     * @param intent
     */
    public static boolean handleBundleExtraData(Intent intent) {
        if (null == intent) return false;
        try {
            Bundle bundle = intent.getExtras();
            if (null != bundle) {
                String te_extras = bundle.getString("te_extras");
                return trackPushClickEvent(te_extras);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * push data in intent
     *
     * @param intent
     */
    public static boolean handleIntentExtraData(Intent intent) {
        if (null == intent) return false;
        try {
            String te_extras = intent.getStringExtra("te_extras");
            return trackPushClickEvent(te_extras);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean handleJPushIntentData(Intent intent) {
        if (null == intent) return false;
        try {
            String pushData = null;
            if (intent.getData() != null) {
                pushData = intent.getData().toString();
            }
            if (TextUtils.isEmpty(pushData) && intent.getExtras() != null) {
                pushData = intent.getExtras().getString("JMessageExtra");
            }
            if (null != pushData && !TextUtils.isEmpty(pushData)) {
                JSONObject jsonObject = new JSONObject(pushData);
                Object extras = jsonObject.opt("n_extras");
                JSONObject json = null;
                if (extras instanceof String) {
                    json = new JSONObject(( String ) extras);
                } else if (extras instanceof JSONObject) {
                    json = ( JSONObject ) extras;
                }
                if (null != json) {
                    return trackPushClickEvent(json.optString("te_extras"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void handleExtraReceiverData(String extras) {
        try {
            if (TextUtils.isEmpty(extras)) return;
            JSONObject json = new JSONObject(extras);
            trackPushClickEvent(json.optString("te_extras"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean trackPushClickEvent(String te_extras) {
        boolean trackSuccess = false;
        try {
            if (TextUtils.isEmpty(te_extras)) return false;
            JSONObject json = new JSONObject(te_extras);
            Object obj = json.opt("#ops_receipt_properties");
            JSONObject ops = null;
            if (obj instanceof String) {
                ops = new JSONObject(( String ) obj);
            } else if (obj instanceof JSONObject) {
                ops = ( JSONObject ) obj;
            }
            if (null != ops) {
                final JSONObject properties = new JSONObject();
                properties.put("#ops_receipt_properties", ops);
                trackSuccess = true;
                final boolean[] flags = new boolean[1];
                ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
                    @Override
                    public void process(ThinkingAnalyticsSDK instance) {
                        flags[0] = true;
                        ThinkingDataRuntimeBridge.onAppPushClickEvent(instance,TA_PUSH_CLICK_EVENT,properties);
                    }
                });
                if (!flags[0]) {
                    pushList.add(properties);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return trackSuccess;
    }

    /**
     * clear cache event
     *
     * @param instance
     */
    public static void clearPushEvent(ThinkingAnalyticsSDK instance) {
        if (null == instance) return;
        for (JSONObject jsonObject : pushList) {
            instance.track(TA_PUSH_CLICK_EVENT, jsonObject);
        }
        pushList.clear();
    }

    public static void handleGtPushEvent(String payload, String msgId) {
        if (gtMsgList.remove(msgId)) {
            handleExtraReceiverData(payload);
        }
    }
}
