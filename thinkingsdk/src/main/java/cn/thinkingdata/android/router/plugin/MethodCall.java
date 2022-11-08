/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.router.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/8/15
 * @since
 */
public class MethodCall {

    public String method;
    public Map<String, Object> arguments = new HashMap<>();

    public <T> T argument(String key) {
        return (T) arguments.get(key);
    }
    
}
