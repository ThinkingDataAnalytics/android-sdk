/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.sqlite;
import android.database.Cursor;

/**
 *
 * @author liulongbing
 * @since 2023/5/5
 */
public interface ITESqliteQueryCallback {

    void onQuerySuccess(Cursor cursor);

    void onQueryFail();

}
