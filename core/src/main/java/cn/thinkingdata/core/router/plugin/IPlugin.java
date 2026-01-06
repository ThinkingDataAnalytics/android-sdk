/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.router.plugin;

/**
 *
 * @author liulongbing
 * @since 2022/8/15
 */
public interface IPlugin {
    void onMethodCall(MethodCall call);
}
