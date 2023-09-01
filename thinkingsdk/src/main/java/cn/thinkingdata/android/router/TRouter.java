/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.router;
import android.content.Context;
import cn.thinkingdata.android.utils.TDLog;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/8/15
 * @since
 */
public final class TRouter {

    private static final String TAG = "ThinkingAnalytics.TRouter";

    private volatile static TRouter instance = null;
    private volatile static boolean hasInit = false;

    private TRouter() {

    }

    public static void init(Context context) {
        if (!hasInit) {
            TDLog.i(TAG, "TRouter init start.");
            hasInit = _TRouter.init(context.getApplicationContext());
            TDLog.i(TAG, "ARouter init over.");
        }
    }

    public static TRouter getInstance() {
        if (!hasInit) {
            throw new InitException("TRouter::Init::Invoke init(context) first!");
        } else {
            if (instance == null) {
                synchronized (TRouter.class) {
                    if (instance == null) {
                        instance = new TRouter();
                    }
                }
            }
            return instance;
        }
    }

    public Postcard build(String path) {
        return _TRouter.getInstance().build(path);
    }


    public Object navigation(Context mContext, Postcard postcard) {
        return _TRouter.getInstance().navigation(mContext, postcard);
    }

}
