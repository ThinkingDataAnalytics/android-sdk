package com.thinking.analyselibrary;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.thinking.analyselibrary.utils.TDLog;

public class WebAppInterface {
    private final static String TAG = "ThinkingAnalyticsSDK.Web";

    Context mContext;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public void thinkingdata_track(String event) {
        if (TextUtils.isEmpty(event)) {
            return;
        }
        TDLog.d(TAG, event);
        ThinkingAnalyticsSDK.sharedInstance(mContext).trackFromH5(event);

    }
}
