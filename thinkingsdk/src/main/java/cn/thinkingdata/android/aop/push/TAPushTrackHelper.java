/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.aop.push;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Method;

import cn.thinkingdata.android.utils.TDLog;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/5/30
 * @since
 */
public class TAPushTrackHelper {

    private static final String TAG = "ThinkingAnalytics";

    public static void trackJPushClickNotification(String extras,
                                                   String title,
                                                   String content,
                                                   String appPushChannel) {
        TDLog.i(TAG, "extras:" + extras);
        TDLog.i(TAG, "title:" + title);
        TDLog.i(TAG, "content:" + content);
        TDLog.i(TAG, "appPushChannel:" + appPushChannel);
        String sfData = getTAData(extras);
        trackNotificationClickedEvent(sfData, title, content, "JPush", appPushChannel);
    }

    public static void onNewIntent(Object activity, Intent intent) {
        //if (!isTrackPushEnabled()) return;
        try {
            if (activity instanceof Activity) {
                TAPushProcess.getInstance().onNotificationClick((Activity) activity, intent);
            }
        } catch (Exception e) {
            TDLog.e(TAG, e.getMessage());
        }
    }

    public static void trackUmengClickNotification(Object msg) {
        if (msg == null) {
            return;
        }
        //if (!isTrackPushEnabled()) return;
        try {
            Method getRawMessage = msg.getClass().getDeclaredMethod("getRaw");
            JSONObject raw = (JSONObject) getRawMessage.invoke(msg);
            if (raw == null) {
                return;
            }
            JSONObject body = raw.optJSONObject("body");
            if (body != null) {
                String extra = raw.optString("extra");
                String title = body.optString("title");
                String content = body.optString("text");
                String taData = getTAData(extra);
                trackNotificationClickedEvent(taData,title,content,"UMeng",null);
                TDLog.i(TAG, String.format("UMengClick is called, title is %s, content is %s," +
                        " extras is %s", title, content, extra));
            }
        } catch (Exception e) {
            TDLog.i(TAG, e.getMessage());
        }
    }

    public static void trackUMengActivityNotification(Intent intent) {
        if (intent == null) {
            return;
        }
        //if (!isTrackPushEnabled()) return;

        try {
            String intentBody = intent.getStringExtra("body");
            if (!TextUtils.isEmpty(intentBody)) {
                JSONObject raw = new JSONObject(intentBody);
                JSONObject body = raw.optJSONObject("body");
                if (body != null) {
                    String extra = raw.optString("extra");
                    String title = body.optString("title");
                    String content = body.optString("text");
                    String messageSource = intent.getStringExtra("message_source");
                    String taData = getTAData(extra);
                    trackNotificationClickedEvent(taData,title,content,"UMeng",messageSource);
                    TDLog.i(TAG, String.format("onUMengActivityMessage is called, title is %s, content is %s," +
                            " extras is %s" + "source is %s", title, content, extra, messageSource));
                }
            }
        } catch (Exception e) {
            TDLog.e(TAG, e.getMessage());
        }
    }

    public static void trackGeTuiNotification(Object gtNotificationMessage) {
        if (gtNotificationMessage == null) {
            return;
        }
        //if (!isTrackPushEnabled()) return;
        try {
            Method getMessageIdMethod = gtNotificationMessage.getClass().getMethod("getMessageId");
            String msgId = (String) getMessageIdMethod.invoke(gtNotificationMessage);
            Method getTitleMethod = gtNotificationMessage.getClass().getMethod("getTitle");
            String title = (String) getTitleMethod.invoke(gtNotificationMessage);
            Method getContentMethod = gtNotificationMessage.getClass().getMethod("getContent");
            String content = (String) getContentMethod.invoke(gtNotificationMessage);
            if (!TextUtils.isEmpty(msgId) &&
                    !TextUtils.isEmpty(title) &&
                    !TextUtils.isEmpty(content)) {
                TAPushProcess.getInstance().trackGTDelayed(msgId, title, content);
            }
        } catch (Exception e) {
            TDLog.e(TAG, e.getMessage());
        }
    }

    public static void trackGeTuiReceiveMessageData(Object gtTransmitMessage) {
        if (gtTransmitMessage == null) {
            return;
        }
        //if (!isTrackPushEnabled()) return;
        try {
            Method getPayloadMethod = gtTransmitMessage.getClass().getMethod("getPayload");
            byte[] bytes = (byte[]) getPayloadMethod.invoke(gtTransmitMessage);
            Method getMessageIdMethod = gtTransmitMessage.getClass().getMethod("getMessageId");
            String msgId = (String) getMessageIdMethod.invoke(gtTransmitMessage);

            if (bytes != null && !TextUtils.isEmpty(msgId)) {
                String taData = new String(bytes);
                TAPushProcess.getInstance().trackGeTuiReceiveMessageData(taData, msgId);
            }
        } catch (Exception e) {
            TDLog.e(TAG, e.getMessage());
        }
    }

    public static void trackMeizuNotification(String extras,
                                              String title,
                                              String content,
                                              String appPushServiceName) {
        //if (!isTrackPushEnabled()) return;

        TDLog.i(TAG, String.format("meizu is called, title is %s, content is %s," +
                " extras is %s, appPushChannel is %s, appPushServiceName is %s", title, content, extras, "Meizu", appPushServiceName));
        try {
            String nExtras = extras;
            try {
                JSONObject extrasJson = null;
                try {
                    extrasJson = new JSONObject(extras);
                } catch (Exception e) {
                }
                //极光的魅族厂商通道
                if (extrasJson != null && extrasJson.has("JMessageExtra")) {
                    JSONObject jMessageJson = extrasJson.optJSONObject("JMessageExtra");
                    if (jMessageJson != null) {
                        JSONObject contentJson = jMessageJson.optJSONObject("m_content");
                        if (contentJson != null) {
                            nExtras = contentJson.optString("n_extras");
                        }
                    }
                    appPushServiceName = "JPush";
                }
            } catch (Exception e) {
            }
            String taData = getTAData(nExtras);
            trackNotificationClickedEvent(taData,title,content,appPushServiceName,"Meizu");
        } catch (Exception e) {
        }
    }

    public static void trackGeTuiNotificationClicked(String title, String content, String data, long time) {
        trackNotificationClickedEvent(data, title, content, "GeTui", null, time);
        TDLog.i(TAG, String.format("GEITUI is called, title is %s, content is %s," +
                " extras is %s", title, content, data));
    }

    /**
     * 极光推送走厂商通道打开 Activity 时，调用方法
     *
     * @param intent Activity 的 intent
     */
    public static void trackJPushOpenActivity(Intent intent) {
        if (intent == null) {
            return;
        }

        //if (!isTrackPushEnabled()) return;

        String data = null;
        //获取华为平台附带的 jpush 信息
        if (intent.getData() != null) {
            data = intent.getData().toString();
        }

        //获取除华为平台附带的 jpush 信息
        if (TextUtils.isEmpty(data) && intent.getExtras() != null) {
            data = intent.getExtras().getString("JMessageExtra");
        }
        TDLog.i(TAG, "Intent data :" + data);
        if (TextUtils.isEmpty(data)) return;
        try {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(data);
            } catch (Exception e) {

            }
            if (jsonObject != null) {
                String title = jsonObject.optString("n_title");
                String content = jsonObject.optString("n_content");
                String extras = jsonObject.optString("n_extras");
                int sdk = jsonObject.optInt("rom_type");
                String channel = TAPushUtils.getJPushSource(sdk);
                TDLog.i(TAG, String.format("JPush is called, title is %s, content is %s," +
                        " extras is %s, appPushChannel is %s", title, content, extras, channel));
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content) || TextUtils.isEmpty(channel)) {
                    return;
                }
                String taData = getTAData(extras);
                trackNotificationClickedEvent(taData,title,content,"JPush",channel);
            }
        } catch (Exception e) {
            TDLog.e(TAG, e.getMessage());
        }
    }

    private static String getTAData(String extras) {
//        String sfData = null;
//        try {
//            JSONObject sfDataJson = new JSONObject(extras);
//            sfData = sfDataJson.optString("sf_data");
//        } catch (Exception e) {
//        }
//        return sfData;
        return extras;
    }

    public static void trackNotificationClickedEvent(String sfData,
                                                     String title,
                                                     String content,
                                                     String appPushServiceName,
                                                     String appPushChannel) {
        trackNotificationClickedEvent(sfData, title, content, appPushServiceName, appPushChannel, 0L);
    }

    private static void trackNotificationClickedEvent(String sfData,
                                                      String title,
                                                      String content,
                                                      String appPushServiceName,
                                                      String appPushChannel,
                                                      long time) {

    }

}
