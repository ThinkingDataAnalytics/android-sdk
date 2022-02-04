package cn.thinkingdata.android.plugin.config;

import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import cn.thinkingdata.android.plugin.entity.ThinkingAnalyticsMethodCell;
import cn.thinkingdata.android.plugin.utils.ThinkingAnalyticsUtil;
import cn.thinkingdata.android.plugin.utils.ThinkingVersionUtils;

/**
 * fragment自动埋点配置
 */
public class ThinkingFragmentHookConfig {

    private static final String FRAGMENT_MIN_SDK = "2.7.6";

    public static String THINKING_FRAGMENT_TRACK_HELPER_API = "cn/thinkingdata/android/aop/ThinkingFragmentTrackHelper";

    public final static HashMap<String, ThinkingAnalyticsMethodCell> TA_FRAGMENT_METHODS = new HashMap<>();

    /**
     * 初始化fragment方法
     */
    public static void initFragmentMethods() {
        if (ThinkingAnalyticsUtil.compareVersion(FRAGMENT_MIN_SDK, ThinkingVersionUtils.thinkingSDKVersion) > 0) {
            THINKING_FRAGMENT_TRACK_HELPER_API = "cn/thinkingdata/android/aop/FragmentTrackHelper";
            TA_FRAGMENT_METHODS.put("onResume()V", new ThinkingAnalyticsMethodCell(
                    "onResume",
                    "()V",
                    "",
                    "trackFragmentResume",
                    "(Ljava/lang/Object;)V",
                    0, 1,
                    Collections.singletonList(Opcodes.ALOAD)));
            TA_FRAGMENT_METHODS.put("setUserVisibleHint(Z)V", new ThinkingAnalyticsMethodCell(
                    "setUserVisibleHint",
                    "(Z)V",
                    "",
                    "trackFragmentSetUserVisibleHint",
                    "(Ljava/lang/Object;Z)V",
                    0, 2,
                    Arrays.asList(Opcodes.ALOAD, Opcodes.ILOAD)));
            TA_FRAGMENT_METHODS.put("onHiddenChanged(Z)V", new ThinkingAnalyticsMethodCell(
                    "onHiddenChanged",
                    "(Z)V",
                    "",
                    "trackOnHiddenChanged",
                    "(Ljava/lang/Object;Z)V",
                    0, 2,
                    Arrays.asList(Opcodes.ALOAD, Opcodes.ILOAD)));
            TA_FRAGMENT_METHODS.put("onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V", new ThinkingAnalyticsMethodCell(
                    "onViewCreated",
                    "(Landroid/view/View;Landroid/os/Bundle;)V",
                    "",
                    "onFragmentViewCreated",
                    "(Ljava/lang/Object;Landroid/view/View;Landroid/os/Bundle;)V",
                    0, 3,
                    Arrays.asList(Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ALOAD)));
            TA_FRAGMENT_METHODS.put("onPause()V", new ThinkingAnalyticsMethodCell(
                    "onPause",
                    "()V",
                    "",
                    "trackFragmentPause",
                    "(Ljava/lang/Object;)V",
                    0, 1,
                    Collections.singletonList(Opcodes.ALOAD)));
        } else {
            TA_FRAGMENT_METHODS.put("onResume()V", new ThinkingAnalyticsMethodCell(
                    "onResume",
                    "()V",
                    "",
                    "onFragmentResume",
                    "(Ljava/lang/Object;)V",
                    0, 1,
                    Collections.singletonList(Opcodes.ALOAD)));
            TA_FRAGMENT_METHODS.put("setUserVisibleHint(Z)V", new ThinkingAnalyticsMethodCell(
                    "setUserVisibleHint",
                    "(Z)V",
                    "",
                    "onFragmentSetUserVisibleHint",
                    "(Ljava/lang/Object;Z)V",
                    0, 2,
                    Arrays.asList(Opcodes.ALOAD, Opcodes.ILOAD)));
            TA_FRAGMENT_METHODS.put("onHiddenChanged(Z)V", new ThinkingAnalyticsMethodCell(
                    "onHiddenChanged",
                    "(Z)V",
                    "",
                    "onFragmentHiddenChanged",
                    "(Ljava/lang/Object;Z)V",
                    0, 2,
                    Arrays.asList(Opcodes.ALOAD, Opcodes.ILOAD)));
            TA_FRAGMENT_METHODS.put("onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V", new ThinkingAnalyticsMethodCell(
                    "onViewCreated",
                    "(Landroid/view/View;Landroid/os/Bundle;)V",
                    "",
                    "onFragmentViewCreated",
                    "(Ljava/lang/Object;Landroid/view/View;Landroid/os/Bundle;)V",
                    0, 3,
                    Arrays.asList(Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ALOAD)));
            TA_FRAGMENT_METHODS.put("onPause()V", new ThinkingAnalyticsMethodCell(
                    "onPause",
                    "()V",
                    "",
                    "onFragmentPause",
                    "(Ljava/lang/Object;)V",
                    0, 1,
                    Collections.singletonList(Opcodes.ALOAD)));
        }
    }
}
