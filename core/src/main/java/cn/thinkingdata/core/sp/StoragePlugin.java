/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.core.sp;
/**
 * @author liulongbing
 * @since 2022/9/7
 */
public interface StoragePlugin {

    <T> void save(int type, T t);

    @Deprecated
    <T> T get(int type);

    <T> T get(int type, T t);
}
