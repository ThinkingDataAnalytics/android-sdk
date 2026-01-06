/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.router;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.core.router.plugin.IPlugin;
import cn.thinkingdata.core.router.plugin.MethodCall;
import cn.thinkingdata.core.router.provider.IProvider;
import cn.thinkingdata.core.utils.TDLog;

/**
 *
 * @author liulongbing
 * @since 2022/8/15
 */
public final class TRouter {

    private static final String TAG = "ThinkingAnalytics.TRouter";
    private volatile static TRouter instance = null;

    private final Map<String, Object> objectMap = new HashMap<>();

    private TRouter() {
        LogisticsCenter.init();
    }

    public static TRouter getInstance() {
            if (instance == null) {
                synchronized (TRouter.class) {
                    if (instance == null) {
                        instance = new TRouter();
                    }
                }
            }
            return instance;
    }

    public Postcard build(String path) {
        if (TextUtils.isEmpty(path)) {
            TDLog.e(TAG, "TRouter build Parameter is invalid!");
            return new Postcard("");
        }
        return new Postcard(path);
    }


    public Object navigation(Postcard postcard) {
        boolean isComplete = LogisticsCenter.completion(postcard);
        if (!isComplete) return null;
        switch (postcard.getType()) {
            case PROVIDER:
                try {
                    if (postcard.isNeedCache()) {
                        if (null != objectMap.get(postcard.getClassName())) {
                            //Whether there is a cache If there is a cache directly returned
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
                            // If there is a cache, get it directly
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
