/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.router.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liulongbing
 * @since 2022/8/15
 */
public class MethodCall {

    public String method;
    public Map<String, Object> arguments = new HashMap<>();

    public <T> T argument(String key) {
        return ( T ) arguments.get(key);
    }

    public boolean hasKey(String key) {
        return arguments.containsKey(key);
    }

}
