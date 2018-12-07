package com.thinking.analyselibrarysv;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class TDContentProviderSV extends ContentProvider {
    private final static int SUCCESS = 1;
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private DatabaseHelperSV dbHelper;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            uriMatcher.addURI(context.getApplicationContext().getPackageName() + ".TDContentProviderSV", "eventssv", SUCCESS);
            dbHelper = new DatabaseHelperSV(context, context.getApplicationContext().getPackageName() + "sv");
        }
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int id = 0;
        try {
            if (uriMatcher.match(uri) == SUCCESS) {
                SQLiteDatabase database = dbHelper.getWritableDatabase();
                id = database.delete(DatabaseManagerSV.Table.EVENTSSV.getName(), selection, selectionArgs);
            }
            else
            {
                throw new UnsupportedOperationException("Not yet implemented");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri u = null;
        try {
            if (uriMatcher.match(uri) == SUCCESS) {
                SQLiteDatabase database = dbHelper.getWritableDatabase();
                long d = database.insert(DatabaseManagerSV.Table.EVENTSSV.getName(), "_id", values);
                u = ContentUris.withAppendedId(uri, d);
            }
            else
            {
                throw new UnsupportedOperationException("Not yet implemented");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return u;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        try {
            if (uriMatcher.match(uri) == SUCCESS) {
                SQLiteDatabase database = dbHelper.getReadableDatabase();
                cursor = database.query(DatabaseManagerSV.Table.EVENTSSV.getName(), projection, selection, selectionArgs, null, null, sortOrder);
            }
            else
            {
                throw new UnsupportedOperationException("Not yet implemented");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
