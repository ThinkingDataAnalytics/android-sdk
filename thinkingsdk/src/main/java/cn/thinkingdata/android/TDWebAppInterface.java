package cn.thinkingdata.android;

import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import cn.thinkingdata.android.utils.TDLog;

public class TDWebAppInterface {
    private final static String TAG = "ThinkingAnalytics.TDWebAppInterface";

    private final ThinkingAnalyticsSDK instance;

    /** Instantiate the interface and set the context */
    TDWebAppInterface(ThinkingAnalyticsSDK instance) {
        this.instance = instance;
    }

    @JavascriptInterface
    public void thinkingdata_track(String event) {
        if (TextUtils.isEmpty(event)) {
            return;
        }
        TDLog.d(TAG, event);
        instance.trackFromH5(event);
    }
}
