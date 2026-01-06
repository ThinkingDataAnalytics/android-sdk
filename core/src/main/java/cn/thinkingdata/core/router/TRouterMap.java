/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.router;

import android.text.TextUtils;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.thinkingdata.core.utils.TDLog;

/**
 * @author liulongbing
 * @since 2022/8/15
 */
public class TRouterMap {

    private static final String TAG = "ThinkingAnalytics.TRouterMap";

    public static final String PUSH_ROUTE_PATH = "/thinkingdata/tpush";
    public static final String ANALYTIC_ROUTE_PATH = "/thinkingdata/analytic";
    public static final String ANALYTIC_PROVIDER_ROUTE_PATH = "/thinkingdata/provider/analytic";
    public static final String PRESET_TEMPLATE_ROUTE_PATH = "/thingkingdata/preset/template";
    public static final String SENSITIVE_PROPERTIES_ROUTE_PATH = "/thingkingdata/sensitive/properties";
    public static final String RCC_PLUGIN_ROUTE_PATH = "/thingkingdata/plugin/rcc";
    public static final String STRATEGY_PLUGIN_ROUTE_PATH = "/thingkingdata/plugin/strategy";
    public static final String INSTALL_REFERRER_PATH = "/thingkingdata/install/referrer";

    public static final String ROUTE_ROOT_PACKAGE = "cn.thinkingdata.module.routes";
    public static final String DOT = ".";

    public static final String SUFFIX_NAME = "ModuleRouter";

    private static final String[] modules = new String[]{"ThirdParty", "Analytic", "PresetTemplate",
            "SensitiveProperties", "RemoteConfig", "Strategy", "InstallReferrer"};

    /**
     * Load the plug-in by module name
     *
     * @return Route Path
     */
    public static Map<String, RouteMeta> getDefaultRouters() {
        Map<String, RouteMeta> routes = new HashMap<>();
        for (String module : modules) {
            try {
                Class<?> clazz = Class.forName(ROUTE_ROOT_PACKAGE + DOT + module + SUFFIX_NAME);
                Method getRouterMethod = clazz.getDeclaredMethod("getRouterMap");
                Map<String, String> map = ( Map<String, String> ) getRouterMethod.invoke(null);
                if (null != map) {
                    for (String key : map.keySet()) {
                        String value = map.get(key);
                        if (null != value && !TextUtils.isEmpty(value)) {
                            JSONObject json = new JSONObject(value);
                            String name = json.optString("name");
                            int type = json.optInt("type");
                            boolean needCache = json.optBoolean("needCache");
                            RouteMeta routeMeta = RouteMeta.build(RouteType.parse(type), key, name, needCache);
                            routes.put(key, routeMeta);
                        }
                    }
                }
            } catch (ClassNotFoundException ignore) {

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return routes;
    }
}
