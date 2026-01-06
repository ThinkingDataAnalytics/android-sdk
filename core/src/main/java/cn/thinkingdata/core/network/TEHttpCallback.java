package cn.thinkingdata.core.network;

import android.os.Handler;
import android.os.Looper;

public abstract class TEHttpCallback {

    static Handler sMainHandler = new Handler(Looper.getMainLooper());

    public boolean callBackOnMainThread = false;

    void onError(int code, final String msg) {
        if (callBackOnMainThread) {
            sMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    onFailure(code, msg);
                }
            });
        } else {
            onFailure(code, msg);
        }
    }

    void onResponse(final TDNetResponse data) {
        if (callBackOnMainThread) {
            sMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    onSuccess(data);
                }
            });
        } else {
            onSuccess(data);
        }
    }

    public abstract void onFailure(int errorCode, String errorMsg);

    public abstract void onSuccess(TDNetResponse data);

}
