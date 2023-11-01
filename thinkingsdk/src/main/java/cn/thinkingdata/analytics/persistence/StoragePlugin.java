/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.analytics.persistence;
/**
 * @author liulongbing
 * @since 2022/9/7
 */
public interface StoragePlugin {

    <T> void save(LocalStorageType type, T t);

    <T> T get(LocalStorageType type);

}
