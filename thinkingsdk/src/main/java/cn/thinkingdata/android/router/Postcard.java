/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.router;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/8/15
 * @since
 */
public class Postcard extends RouteMeta{

    private String path;
    private String action;
    public Map<String, Object> arguments = new HashMap<>();

    public Postcard(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public Postcard withInt(String key, int value) {
        arguments.put(key, value);
        return this;
    }

    public Postcard withDouble(String key, double value) {
        arguments.put(key, value);
        return this;
    }

    public Postcard withLong(String key, long value) {
        arguments.put(key, value);
        return this;
    }

    public Postcard withString(String key, String value) {
        arguments.put(key, value);
        return this;
    }

    public Postcard withCharSequence(String key, CharSequence value) {
        arguments.put(key, value);
        return this;
    }

    public Postcard withFloat(String key, float value) {
        arguments.put(key, value);
        return this;
    }

    public Postcard withBoolean(String key, boolean value) {
        arguments.put(key, value);
        return this;
    }

    public Postcard withObject(String key, Object obj) {
        arguments.put(key, obj);
        return this;
    }

    public String getAction() {
        return action;
    }

    public Postcard withAction(String action) {
        this.action = action;
        return this;
    }

    /**
     * Navigation to the route with path in postcard.
     * No param, will be use application context.
     *
     * @return
     */
    public Object navigation() {
        return navigation(null);
    }

    /**
     * Navigation to the route with path in postcard.
     *
     * @param context
     * @return
     */
    public Object navigation(Context context) {
        return TRouter.getInstance().navigation(context, this);
    }

}
