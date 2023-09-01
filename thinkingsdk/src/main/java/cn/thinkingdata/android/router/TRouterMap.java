/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.router;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.android.utils.TDLog;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/8/15
 * @since
 */
public class TRouterMap {

    private static final String TAG = "ThinkingAnalytics.TRouterMap";

    public static final String ROUTE_ROOT_PACKAGE = "cn.thinkingdata.module.routes";
    public static final String DOT = ".";

    public static final String SUFFIX_NAME = "ModuleRouter";

    private static final String[] modules = new String[]{"ThirdParty"};

    /**
     * Load the plug-in by module name
     *
     * @return
     */
    public static Map<String, RouteMeta> getDefaultRouters() {
        Map<String, RouteMeta> routes = new HashMap<>();
        for (String module : modules) {
            try {
                Class<?> clazz = Class.forName(ROUTE_ROOT_PACKAGE + DOT + module + SUFFIX_NAME);
                Method getRouterMethod = clazz.getDeclaredMethod("getRouterMap");
                Map<String, String> map = (Map<String, String>) getRouterMethod.invoke(null);
                if (null != map) {
                    for (String key : map.keySet()) {
                        String value = map.get(key);
                        if (null != value && !TextUtils.isEmpty(value)) {
                            JSONObject json = new JSONObject(value);
                            String name = json.optString("name");
                            int type = json.optInt("type");
                            boolean needCache = json.optBoolean("needCache");
                            RouteMeta routeMeta = RouteMeta.build(RouteType.parse(type), key, name,needCache);
                            routes.put(key, routeMeta);
                        }
                    }
                }
            }catch (ClassNotFoundException e){
                TDLog.d(TAG, "No routing table found");
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        return routes;
    }
}
