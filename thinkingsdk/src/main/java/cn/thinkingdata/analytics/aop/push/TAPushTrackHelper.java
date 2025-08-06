/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.aop.push;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * push message helper for asm
 *
 * @author liulongbing
 * @since 2022/5/30
 */
public class TAPushTrackHelper {

    /**
     * JPushMessageReceiver
     *
     * @param extras         extras
     * @param title          notification title
     * @param content        notification content
     * @param appPushChannel push channel
     */
    public static void trackJPushClickNotification(String extras,
                                                   String title,
                                                   String content,
                                                   String appPushChannel) {
        TAPushUtils.handleExtraReceiverData(extras);
    }

    public static void onJPushTokenRegister(String token) {
        if (!TextUtils.isEmpty(token)) {
            try {
                JSONObject json = new JSONObject();
                json.put("jiguang_id", token);
                TAPushUtils.handlePushToken(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void onFcmTokenRegister(String token){
        if (!TextUtils.isEmpty(token)) {
            try {
                JSONObject json = new JSONObject();
                json.put("fcm_token", token);
                TAPushUtils.handlePushToken(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * onCreate
     *
     * @param activity activity
     * @param intent   intent
     */
    public static void onNewIntent(Object activity, Intent intent) {
        try {
            if (activity instanceof Activity) {
                TAPushUtils.handleStartIntent(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * onNewIntent
     *
     * @param activity activity
     */
    public static void onCreateIntent(Object activity) {
        try {
            if (activity instanceof Activity) {
                Intent intent = (( Activity ) activity).getIntent();
                TAPushUtils.handleStartIntent(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * UmengNotificationClickHandler
     *
     * @param msg msg
     */
    public static void trackUmengClickNotification(Object msg) {
        if (null == msg) return;
        try {
            Method getRawMessage = msg.getClass().getDeclaredMethod("getRaw");
            JSONObject raw = ( JSONObject ) getRawMessage.invoke(msg);
            if (raw == null) {
                return;
            }
            JSONObject body = raw.optJSONObject("body");
            if (body != null) {
                String extra = raw.optString("extra");
                TAPushUtils.handleExtraReceiverData(extra);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * UmengNotifyClick  UmengNotifyClickActivity onMessage(UMessage)
     *
     * @param obj UMessage
     */
    public static void trackUMengCallBackNotification(Object obj) {
        if (null == obj) return;
        try {
            Class<?> uMessageClass = obj.getClass();
            Method getRawMethod = uMessageClass.getMethod("getRaw");
            Object jsonObj = getRawMethod.invoke(obj);
            if (jsonObj instanceof JSONObject) {
                JSONObject json = ( JSONObject ) jsonObj;
                TAPushUtils.handleExtraReceiverData(json.optString("extra"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * UmengNotifyClickActivity onMessage(intent)
     *
     * @param intent intent
     */
    public static void trackUMengActivityNotification(Intent intent) {
        if (null == intent) return;
        try {
            String body = intent.getStringExtra("body");
            if (!TextUtils.isEmpty(body)) {
                JSONObject raw = new JSONObject(body);
                TAPushUtils.handleExtraReceiverData(raw.optString("extra"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * onNotificationMessageClicked
     *
     * @param gtNotificationMessage NotificationMessage
     */
    public static void trackGeTuiNotification(Object gtNotificationMessage) {
        if (null == gtNotificationMessage) return;
        try {
            Method getMessageIdMethod = gtNotificationMessage.getClass().getMethod("getMessageId");
            String msgId = ( String ) getMessageIdMethod.invoke(gtNotificationMessage);
            if (!TextUtils.isEmpty(msgId)) {
                TAPushUtils.gtMsgList.add(msgId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * onReceiveMessageData
     *
     * @param gtTransmitMessage TransmitMessage
     */
    public static void trackGeTuiReceiveMessageData(Object gtTransmitMessage) {
        if (null == gtTransmitMessage) return;
        try {
            Method getPayloadMethod = gtTransmitMessage.getClass().getMethod("getPayload");
            byte[] bytes = ( byte[] ) getPayloadMethod.invoke(gtTransmitMessage);
            Method getMessageIdMethod = gtTransmitMessage.getClass().getMethod("getMessageId");
            String msgId = ( String ) getMessageIdMethod.invoke(gtTransmitMessage);

            if (bytes != null && !TextUtils.isEmpty(msgId)) {
                String taData = new String(bytes);
                TAPushUtils.handleGtPushEvent(taData, msgId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackMeizuNotification(String extras,
                                              String title,
                                              String content,
                                              String appPushServiceName) {
        //Meizu push data is obtained in intent
    }

    /**
     * BroadcastReceiver
     * cn.jpush.android.intent.NOTIFICATION_OPENED
     *
     * @param receiver BroadcastReceiver
     * @param context  context
     * @param intent   intent
     */
    public static void trackBroadcastReceiverNotification(BroadcastReceiver receiver, Context context, Intent intent) {
        if (null == intent) return;
        String action = intent.getAction();
        if ("cn.jpush.android.intent.NOTIFICATION_OPENED".equals(action)) {
            String ext = intent.getStringExtra("cn.jpush.android.EXTRA");
            TAPushUtils.handleExtraReceiverData(ext);
        }
    }

}
