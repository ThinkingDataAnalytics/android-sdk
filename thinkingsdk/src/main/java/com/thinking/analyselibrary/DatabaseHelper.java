package com.thinking.analyselibrary;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.thinking.analyselibrary.utils.TDLog;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ThinkingAnalyticsSDK";
    private static final String KEY_DATA = "clickdata";
    private static final String KEY_CREATED_AT = "creattime";
    private static final int VERSION = 1;
    private static final String CREATE_EVENTS_TABLE =
            "CREATE TABLE " + DatabaseManager.Table.EVENTS.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_DATA + " STRING NOT NULL, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL);";

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                    int version) {
        super(context, name, factory, version);
    }

    public DatabaseHelper(Context context, String name, int version){
        this(context, name, null, version);
    }

    public DatabaseHelper(Context context, String name){
        this(context, name, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        TDLog.d(TAG, "Creating a new TDData database");
        db.execSQL(CREATE_EVENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TDLog.d(TAG, "Upgrading TDData database");

        db.execSQL("DROP TABLE IF EXISTS " + DatabaseManager.Table.EVENTS.getName());
        db.execSQL(CREATE_EVENTS_TABLE);
    }
}
