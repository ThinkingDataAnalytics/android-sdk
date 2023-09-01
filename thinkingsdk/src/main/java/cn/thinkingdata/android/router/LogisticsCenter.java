/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.router;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.thinkingdata.android.utils.TDLog;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/8/15
 * @since
 */
public class LogisticsCenter {

    private static final String TAG = "ThinkingAnalytics.TRouter";

    private static Context mContext;

    public static Map<String, RouteMeta> routes;
    public static Map<String, RouteMeta> plugins = new HashMap<>();

    public synchronized static void init(Context context) {
        mContext = context;
        //Load the plug-in by module name
        routes = TRouterMap.getDefaultRouters();
//        try {
//            ClassUtils.getFileNameByPackageName(mContext, TRouterMap.ROUTE_ROOT_PACKAGE, new OnLoadPluginCallBack() {
//                @Override
//                public void onPluginLoadSuccess(Set<String> classNames) {
//                    handlePlugin(classNames);
//                }
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private synchronized static void handlePlugin(Set<String> classNames) {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                Method getRouterMethod = clazz.getDeclaredMethod("getRouterMap");
                Map<String, String> map = (Map<String, String>) getRouterMethod.invoke(null);
                if (null != map) {
                    for (String key : map.keySet()) {
                        String value = map.get(key);
                        if (!TextUtils.isEmpty(value)) {
                            JSONObject json = new JSONObject(value);
                            String name = json.optString("name");
                            int type = json.optInt("type");
                            boolean needCache = json.optBoolean("needCache");
                            RouteMeta routeMeta = RouteMeta.build(RouteType.parse(type), key, name,needCache);
                            plugins.put(key, routeMeta);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean completion(Postcard postcard) {
        RouteMeta routeMeta;
        routeMeta = plugins.get(postcard.getPath());
        if (null == routeMeta) {
            routeMeta = routes.get(postcard.getPath());
        }
        if (null == routeMeta) {
            TDLog.e(TAG, "not find plugin：" + postcard.getPath());
            return false;
        }
        postcard.setType(routeMeta.getType());
        postcard.setClassName(routeMeta.getClassName());
        postcard.setNeedCache(routeMeta.isNeedCache());
        return true;
    }

    public interface OnLoadPluginCallBack {
        void onPluginLoadSuccess(Set<String> classNames);
    }

}
