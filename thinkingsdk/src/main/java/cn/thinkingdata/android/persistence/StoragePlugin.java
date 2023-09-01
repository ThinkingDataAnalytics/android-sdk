/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.persistence;
/**
 * @author liulongbing
 * @create 2022/9/7
 * @since
 */
public interface StoragePlugin {

    <T> void save(LocalStorageType type, T t);

    <T> T get(LocalStorageType type);

}
