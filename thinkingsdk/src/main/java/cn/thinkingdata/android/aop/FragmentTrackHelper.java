
package cn.thinkingdata.android.aop;

import android.os.Bundle;
import android.view.View;

import cn.thinkingdata.android.ThinkingDataRuntimeBridge;

public class FragmentTrackHelper {

    private static final String TAG = "ThinkingAnalytics";

    /**
     * 插件 Hook 处理 Fragment 的 onViewCreated 生命周期
     *
     * @param object   Fragment
     * @param rootView View
     * @param bundle   Bundle
     */
    public static void onFragmentViewCreated(Object object, View rootView, Bundle bundle) {
        ThinkingDataRuntimeBridge.onFragmentCreateView(object, rootView);
    }

    /**
     * 插件 Hook 处理 Fragment 的 onResume 生命周期
     *
     * @param object Fragment
     */
    public static void trackFragmentResume(Object object) {
        ThinkingDataRuntimeBridge.onFragmentOnResume(object);
    }

    /**
     * 插件 Hook 处理 Fragment 的 onPause 生命周期
     *
     * @param object Fragment
     */
    public static void trackFragmentPause(Object object) {
        //Log.i(TAG, "hook-trackFragmentPause");
    }

    /**
     * 插件 Hook 处理 Fragment 的 setUserVisibleHint 回调
     *
     * @param object          Fragment
     * @param isVisibleToUser 是否可见
     */
    public static void trackFragmentSetUserVisibleHint(Object object, boolean isVisibleToUser) {
        ThinkingDataRuntimeBridge.onFragmentSetUserVisibleHint(object, isVisibleToUser);
    }

    /**
     * 插件 Hook 处理 Fragment 的 onHiddenChanged 回调
     *
     * @param object Fragment
     * @param hidden Fragment 是否隐藏
     */
    public static void trackOnHiddenChanged(Object object, boolean hidden) {
        ThinkingDataRuntimeBridge.onFragmentHiddenChanged(object, hidden);
    }

}
