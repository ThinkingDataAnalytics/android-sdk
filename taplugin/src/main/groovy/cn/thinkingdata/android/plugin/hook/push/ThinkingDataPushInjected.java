/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.plugin.hook.push;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cn.thinkingdata.android.plugin.utils.LoggerUtil;
import cn.thinkingdata.android.plugin.utils.ThinkingAnalyticsUtil;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/5/30
 * @since
 */
public class ThinkingDataPushInjected {

    public static final String PUSH_TRACK_API = "cn/thinkingdata/android/aop/push/TAPushTrackHelper";


    public static void handlePushEvent(MethodVisitor methodVisitor, String superName, String nameDesc) {
        if ("cn/jpush/android/service/JPushMessageReceiver".equals(superName)
                && "onNotifyMessageOpened(Landroid/content/Context;Lcn/jpush/android/api/NotificationMessage;)V".equals(nameDesc)) {
            handleJPushEvent(methodVisitor);
        } else if ("onNewIntent(Landroid/content/Intent;)V".equals(nameDesc)) {
            handleNewIntent(methodVisitor, superName);
        } else if ("com/umeng/message/UmengNotificationClickHandler".equals(superName)) {
            handleUmengClickHandler(methodVisitor, nameDesc);
        } else if ("com/umeng/message/UmengNotifyClickActivity".equals(superName)
                && "onMessage(Landroid/content/Intent;)V".equals(nameDesc)) {
            handleUmengClickActivity(methodVisitor);
        } else if ("com/igexin/sdk/GTIntentService".equals(superName)) {
            handleGTPushEvent(methodVisitor, nameDesc);
        } else if ("onNotificationClicked(Landroid/content/Context;Lcom/meizu/cloud/pushsdk/handler/MzPushMessage;)V".equals(nameDesc)
                && "com/meizu/cloud/pushsdk/MzPushMessageReceiver".equals(superName)) {
            handleMeizuPushEvent(methodVisitor);
        }
    }

    private static void handleJPushEvent(MethodVisitor methodVisitor) {
        try {
            Label l1 = new Label();
            // 参数空判断
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitJumpInsn(Opcodes.IFNULL, l1);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitJumpInsn(Opcodes.IFNULL, l1);
            // 读取参数
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "cn/jpush/android/api/NotificationMessage", "notificationExtras", "Ljava/lang/String;");
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "cn/jpush/android/api/NotificationMessage", "notificationTitle", "Ljava/lang/String;");
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, "cn/jpush/android/api/NotificationMessage", "notificationContent", "Ljava/lang/String;");
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, PUSH_TRACK_API, "trackJPushClickNotification", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
            methodVisitor.visitLabel(l1);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void handleNewIntent(MethodVisitor methodVisitor, String superName) {
        try {
            if (ThinkingAnalyticsUtil.isActivityClass(superName)) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, PUSH_TRACK_API, "onNewIntent", "(Ljava/lang/Object;Landroid/content/Intent;)V", false);
            }
        } catch (Throwable throwable) {
            LoggerUtil.error("Can not load class for [onNewIntent] hook");
        }
    }

    private static void handleUmengClickHandler(MethodVisitor methodVisitor, String nameDesc) {
        try {
            if ("openActivity(Landroid/content/Context;Lcom/umeng/message/entity/UMessage;)V".equals(nameDesc)
                    || "dealWithCustomAction(Landroid/content/Context;Lcom/umeng/message/entity/UMessage;)V".equals(nameDesc)
                    || "launchApp(Landroid/content/Context;Lcom/umeng/message/entity/UMessage;)V".equals(nameDesc)
                    || "openUrl(Landroid/content/Context;Lcom/umeng/message/entity/UMessage;)V".equals(nameDesc)) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, PUSH_TRACK_API, "trackUmengClickNotification", "(Ljava/lang/Object;)V", false);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void handleUmengClickActivity(MethodVisitor methodVisitor) {
        try {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, PUSH_TRACK_API, "trackUMengActivityNotification", "(Landroid/content/Intent;)V", false);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void handleGTPushEvent(MethodVisitor methodVisitor, String nameDesc) {
        try {
            if ("onReceiveMessageData(Landroid/content/Context;Lcom/igexin/sdk/message/GTTransmitMessage;)V".equals(nameDesc)) {
                LoggerUtil.info("2");
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, PUSH_TRACK_API, "trackGeTuiReceiveMessageData", "(Ljava/lang/Object;)V", false);
            } else if ("onNotificationMessageClicked(Landroid/content/Context;Lcom/igexin/sdk/message/GTNotificationMessage;)V".equals(nameDesc)) {
                LoggerUtil.info("3");
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, PUSH_TRACK_API, "trackGeTuiNotification", "(Ljava/lang/Object;)V", false);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void handleMeizuPushEvent(MethodVisitor methodVisitor) {
        try {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            Label l1 = new Label();
            // 参数空判断
            methodVisitor.visitJumpInsn(Opcodes.IFNULL, l1);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitJumpInsn(Opcodes.IFNULL, l1);
            // 读取参数
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/meizu/cloud/pushsdk/handler/MzPushMessage", "getSelfDefineContentString", "()Ljava/lang/String;", false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/meizu/cloud/pushsdk/handler/MzPushMessage", "getTitle", "()Ljava/lang/String;", false);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/meizu/cloud/pushsdk/handler/MzPushMessage", "getContent", "()Ljava/lang/String;", false);
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, PUSH_TRACK_API, "trackMeizuNotification", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
            methodVisitor.visitLabel(l1);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void addOnNewIntent(ClassVisitor classVisitor) {
        MethodVisitor mv = classVisitor.visitMethod(Opcodes.ACC_PROTECTED, "onNewIntent", "(Landroid/content/Intent;)V", null, null);

        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "android/app/Activity", "onNewIntent", "(Landroid/content/Intent;)V", false);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, PUSH_TRACK_API, "onNewIntent", "(Ljava/lang/Object;Landroid/content/Intent;)V", false);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

}
