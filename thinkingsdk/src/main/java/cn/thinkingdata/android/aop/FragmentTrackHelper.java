/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.android.aop;

import android.os.Bundle;
import android.view.View;
import cn.thinkingdata.android.ThinkingDataRuntimeBridge;

/**
 * Send the data by invoking the method through asm.
 */
public class FragmentTrackHelper {

    private static final String TAG = "ThinkingAnalytics";

    public static void onFragmentViewCreated(Object object, View rootView, Bundle bundle) {
        ThinkingDataRuntimeBridge.onFragmentCreateView(object, rootView);
    }

    public static void trackFragmentResume(Object object) {
        ThinkingDataRuntimeBridge.onFragmentOnResume(object);
    }

    public static void trackFragmentPause(Object object) {
    }

    public static void trackFragmentSetUserVisibleHint(Object object, boolean isVisibleToUser) {
        ThinkingDataRuntimeBridge.onFragmentSetUserVisibleHint(object, isVisibleToUser);
    }

    public static void trackOnHiddenChanged(Object object, boolean hidden) {
        ThinkingDataRuntimeBridge.onFragmentHiddenChanged(object, hidden);
    }
}
