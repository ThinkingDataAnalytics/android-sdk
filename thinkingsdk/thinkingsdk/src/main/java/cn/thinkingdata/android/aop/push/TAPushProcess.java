/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.aop.push;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.android.utils.TDLog;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/5/31
 * @since
 */
public class TAPushProcess {

    private static final String TAG = "ThinkingAnalytics.process";

    private static TAPushProcess INSTANCE;

    private final static int GT_PUSH_MSG = 100;

    private final Map<String, TANotificationInfo> mGeTuiMap;

    private final Handler mPushHandler;

    private TAPushProcess() {
        mGeTuiMap = new HashMap<>();
        HandlerThread thread = new HandlerThread("TA.PushThread");
        thread.start();
        mPushHandler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == GT_PUSH_MSG) {
                    try {
                        String msgId = (String) msg.obj;
                        if (!TextUtils.isEmpty(msgId) && mGeTuiMap.containsKey(msgId)) {
                            TANotificationInfo push = mGeTuiMap.get(msgId);
                            mGeTuiMap.remove(msgId);
                            if (push != null) {
                                TAPushTrackHelper.trackGeTuiNotificationClicked(push.title,push.content,null,push.time);
                            }
                        }
                    } catch (Exception e) {
                        TDLog.e(TAG,e.getMessage());
                    }
                }
            }
        };
    }

    public static synchronized TAPushProcess getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TAPushProcess();
        }
        return INSTANCE;
    }

    public void onNotificationClick(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            //只有 Activity 打开时，才尝试出发极光的推送
            if (context instanceof Activity) {
                TAPushTrackHelper.trackJPushOpenActivity(intent);
            }
        } catch (Exception e) {
            TDLog.e(TAG, e.getMessage());
        }
    }

    void trackGTDelayed(String messageId, String title, String content) {
        try {
            Message message = Message.obtain();
            message.what = GT_PUSH_MSG;
            message.obj = messageId;
            mGeTuiMap.put(messageId, new TANotificationInfo(title, content, System.currentTimeMillis()));
            mPushHandler.sendMessageDelayed(message, 200);
        } catch (Exception e) {
            TDLog.e(TAG, e.getMessage());
        }
    }

    void trackGeTuiReceiveMessageData(String extraData, String msgId) {
        try {
            if (mPushHandler.hasMessages(GT_PUSH_MSG) && mGeTuiMap.containsKey(msgId)) {
                mPushHandler.removeMessages(GT_PUSH_MSG);
                TANotificationInfo push = mGeTuiMap.get(msgId);
                if (push != null) {
                    TAPushTrackHelper.trackGeTuiNotificationClicked(push.title, push.content, extraData, push.time);
                }
                mGeTuiMap.remove(msgId);
                TDLog.i(TAG, " onGeTuiReceiveMessage:msg id : " + msgId);
            }
        } catch (Exception e) {
            TDLog.e(TAG, e.getMessage());
        }
    }
}
