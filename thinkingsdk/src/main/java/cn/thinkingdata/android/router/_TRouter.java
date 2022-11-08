/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.router;

import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.android.router.plugin.IPlugin;
import cn.thinkingdata.android.router.plugin.MethodCall;
import cn.thinkingdata.android.router.provider.IProvider;
import cn.thinkingdata.android.utils.TDLog;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/8/15
 * @since
 */
final class _TRouter {
    private static final String TAG = "ThinkingAnalytics.TRouter";
    private volatile static _TRouter instance = null;
    private volatile static boolean hasInit = false;
    private static Context mContext;

    private Map<String, Object> objectMap = new HashMap<>();

    private _TRouter() {
    }

    protected static synchronized boolean init(Context context) {
        mContext = context;
        //自动加载模块插件
        LogisticsCenter.init(mContext);
        TDLog.i(TAG, "TRouter init success!");
        hasInit = true;
        return true;
    }

    protected static _TRouter getInstance() {
        if (!hasInit) {
            throw new InitException("TRouterCore::Init::Invoke init(context) first!");
        } else {
            if (instance == null) {
                synchronized (_TRouter.class) {
                    if (instance == null) {
                        instance = new _TRouter();
                    }
                }
            }
        }
        return instance;
    }

    protected Postcard build(String path) {
        if (TextUtils.isEmpty(path)) {
            TDLog.e(TAG, "TRouter build Parameter is invalid!");
            return new Postcard("");
        }
        return new Postcard(path);
    }

    protected Object navigation(final Context context, final Postcard postcard) {
        boolean isComplete = LogisticsCenter.completion(postcard);
        if (!isComplete) return null;
        switch (postcard.getType()) {
            case PROVIDER:
                try {
                    if (postcard.isNeedCache()) {
                        if (null != objectMap.get(postcard.getClassName())) {
                            //是否存在缓存 如果有缓存 直接返回
                            return (IProvider) objectMap.get(postcard.getClassName());
                        }
                    }
                    Class<?> providerMeta = Class.forName(postcard.getClassName());
                    IProvider provider = (IProvider) providerMeta.getConstructor().newInstance();
                    if (postcard.isNeedCache()) {
                        objectMap.put(postcard.getClassName(), provider);
                    }
                    return provider;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case PLUGIN:
                MethodCall call = new MethodCall();
                call.method = postcard.getAction();
                call.arguments = postcard.arguments;
                try {
                    if (postcard.isNeedCache()) {
                        if (null != objectMap.get(postcard.getClassName())) {
                            //是否存在缓存 如果有缓存 直接拿到
                            IPlugin cachePlugin = (IPlugin) objectMap.get(postcard.getClassName());
                            if (null != cachePlugin) {
                                cachePlugin.onMethodCall(call);
                                return null;
                            }
                        }
                    }
                    Class<?> pluginClass = Class.forName(postcard.getClassName());
                    IPlugin plugin = (IPlugin) pluginClass.getConstructor().newInstance();
                    if (postcard.isNeedCache()) {
                        objectMap.put(postcard.getClassName(), plugin);
                    }
                    plugin.onMethodCall(call);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        return null;
    }

}
