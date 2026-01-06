/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.router;

/**
 *
 * @author liulongbing
 * @since 2022/8/15
 */
public class RouteMeta {

    private String path;
    private RouteType type;
    private String className;
    private boolean needCache;

    public RouteMeta() {
    }

    public RouteMeta(RouteType type, String path, String className,boolean needCache) {
        this.type = type;
        this.path = path;
        this.className = className;
        this.needCache = needCache;
    }

    public static RouteMeta build(RouteType type, String path, String className,boolean needCache) {
        return new RouteMeta(type, path, className,needCache);
    }

    public void setType(RouteType type) {
        this.type = type;
    }

    public RouteType getType() {
        return type;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPath() {
        return path;
    }

    public String getClassName() {
        return className;
    }

    public void setNeedCache(boolean needCache) {
        this.needCache = needCache;
    }

    public boolean isNeedCache() {
        return needCache;
    }

}
