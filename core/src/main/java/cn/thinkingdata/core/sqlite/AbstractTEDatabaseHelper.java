/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.sqlite;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 * @author liulongbing
 * @since 2023/5/5
 */
public abstract class AbstractTEDatabaseHelper extends SQLiteOpenHelper {

    private final ExecutorService mPool = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    public AbstractTEDatabaseHelper(Context context, String name, int version) {
        super(context, name, null, version);
    }

    public void insertAsync(final String table, final ContentValues values, final ITESqliteInsertCallback callback) {
        mPool.execute(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                long count = db.insert(table, null, values);
                if (null != callback) {
                    callback.onInsertCallback(count);
                }
            }
        });
    }

    public void deleteAsync(final String table, final String whereClause, final String[] whereArgs, final ITESqliteDeleteCallback callback) {
        mPool.execute(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                int count = db.delete(table, whereClause, whereArgs);
                if (null != callback) {
                    callback.onDeleteCallback(count);
                }
            }
        });
    }

    public void updateAsync(final String table, final ContentValues values, final String whereClause, final String[] whereArgs, final ITESqliteUpdateCallback callback) {
        mPool.execute(new Runnable() {
            @Override
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                int count = db.update(table, values, whereClause, whereArgs);
                if (null != callback) {
                    callback.onUpdateCallback(count);
                }
            }
        });
    }

    public void rawQueryAsync(final String sql, final String[] selectionArgs, final ITESqliteQueryCallback callback) {
        mPool.execute(new Runnable() {
            @Override
            public void run() {
                Cursor c = null;
                try {
                    SQLiteDatabase db = getReadableDatabase();
                    c = db.rawQuery(sql, selectionArgs);
                    if (null != callback) {
                        if (c != null) {
                            callback.onQuerySuccess(c);
                        } else {
                            callback.onQueryFail();
                        }
                    }
                } catch (Exception e) {
                    if (null != callback) {
                        callback.onQueryFail();
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        });
    }

}
