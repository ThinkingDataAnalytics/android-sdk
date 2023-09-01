/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.router.plugin;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/8/15
 * @since
 */
public interface IPlugin {
    void onMethodCall(MethodCall call);
}
