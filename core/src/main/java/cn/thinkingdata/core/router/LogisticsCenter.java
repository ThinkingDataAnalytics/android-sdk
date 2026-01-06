/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.router;

import java.util.Map;

/**
 *
 * @author liulongbing
 * @since 2022/8/15
 */
public class LogisticsCenter {

    public static Map<String, RouteMeta> routes;

    public synchronized static void init() {
        routes = TRouterMap.getDefaultRouters();
    }

    public static boolean completion(Postcard postcard) {
        RouteMeta routeMeta;
        routeMeta = routes.get(postcard.getPath());
        if (null == routeMeta) {
            return false;
        }
        postcard.setType(routeMeta.getType());
        postcard.setClassName(routeMeta.getClassName());
        postcard.setNeedCache(routeMeta.isNeedCache());
        return true;
    }
}
