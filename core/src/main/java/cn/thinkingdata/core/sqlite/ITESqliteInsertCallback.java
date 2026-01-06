/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.sqlite;

/**
 *
 * @author liulongbing
 * @since 2023/5/5
 */
public interface ITESqliteInsertCallback {

    void onInsertCallback(long count);

}
