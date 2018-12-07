package com.thinking.analyselibrarysv;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelperSV extends SQLiteOpenHelper{
    private static final String TAG = "ThinkingAnalyticsSDKSV";
    private static final String KEY_DATA = "clickdatasv";
    private static final String KEY_CREATED_AT = "creattimesv";
    private static final int VERSION = 1;
    private static final String CREATE_EVENTS_TABLE =
            "CREATE TABLE " + DatabaseManagerSV.Table.EVENTSSV.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_DATA + " STRING NOT NULL, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL);";

    public DatabaseHelperSV(Context context, String name, SQLiteDatabase.CursorFactory factory,
                            int version) {
        super(context, name, factory, version);
    }

    public DatabaseHelperSV(Context context, String name, int version){
        this(context, name, null, version);
    }

    public DatabaseHelperSV(Context context, String name){
        this(context, name, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        TDLogSV.d(TAG, "Creating a new TDDataSV database");
        db.execSQL(CREATE_EVENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TDLogSV.d(TAG, "Upgrading TDDataSV database");

        db.execSQL("DROP TABLE IF EXISTS " + DatabaseManagerSV.Table.EVENTSSV.getName());
        db.execSQL(CREATE_EVENTS_TABLE);
    }
}
