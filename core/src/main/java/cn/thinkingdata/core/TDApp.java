/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import cn.thinkingdata.core.router.TRouter;
import cn.thinkingdata.core.router.TRouterMap;
import cn.thinkingdata.core.utils.TDCommonUtil;

/**
 * @author liulongbing
 * @since 2024/5/17
 */
public class TDApp {

    public static void init(Context context) {
        if (context == null) return;
        List<TDSettings> settings = TDCommonUtil.getConfigFromAssets(context);
        init(context, settings);
    }

    public static void init(Context context, TDSettings setting) {
        if (context == null || setting == null) return;
        List<TDSettings> lists = new ArrayList<>();
        lists.add(setting);
        init(context, lists);
    }

    public static void init(Context context, List<TDSettings> settings) {
        if (context == null || settings == null) return;
        try {
            //初始化采集SDK
            TRouter.getInstance().build(TRouterMap.ANALYTIC_ROUTE_PATH)
                    .withAction("init")
                    .withObject("context", context)
                    .withObject("settings", settings)
                    .navigation();
            //初始化RCC SDK
            TRouter.getInstance().build(TRouterMap.RCC_PLUGIN_ROUTE_PATH)
                    .withAction("init")
                    .withObject("context", context)
                    .withObject("settings", settings)
                    .navigation();
            //初始化触发式 SDK
            TRouter.getInstance().build(TRouterMap.STRATEGY_PLUGIN_ROUTE_PATH)
                    .withAction("init")
                    .withObject("context", context)
                    .withObject("settings", settings)
                    .navigation();
        } catch (Exception ignore) {
        }
    }

}
