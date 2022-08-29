package cn.thinkingdata.android.plugin.config;

import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import cn.thinkingdata.android.plugin.entity.ThinkingAnalyticsMethodCell;

/**
 * fragment自动埋点配置
 */
public class ThinkingFragmentHookConfig {

    public static String THINKING_FRAGMENT_TRACK_HELPER_API = "cn/thinkingdata/android/aop/FragmentTrackHelper";

    public final static HashMap<String, ThinkingAnalyticsMethodCell> TA_FRAGMENT_METHODS = new HashMap<>();

    static {
        TA_FRAGMENT_METHODS.put("onResume()V", new ThinkingAnalyticsMethodCell(
                "onResume",
                "()V",
                "",// parent省略，均为 android/app/Fragment 或 android/support/v4/app/Fragment
                "trackFragmentResume",
                "(Ljava/lang/Object;)V",
                0, 1,
                Collections.singletonList(Opcodes.ALOAD)));
        TA_FRAGMENT_METHODS.put("setUserVisibleHint(Z)V", new ThinkingAnalyticsMethodCell(
                "setUserVisibleHint",
                "(Z)V",
                "",// parent省略，均为 android/app/Fragment 或 android/support/v4/app/Fragment
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
                "",// parent省略，均为 android/app/Fragment 或 android/support/v4/app/Fragment
                "trackFragmentPause",
                "(Ljava/lang/Object;)V",
                0, 1,
                Collections.singletonList(Opcodes.ALOAD)));
    }
}
