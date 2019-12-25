package cn.thinkingdata.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import cn.thinkingdata.android.utils.TDLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DatabaseAdapter {
    private static final String TAG = "ThinkingAnalytics.DatabaseAdapter";
    public enum Table {
        EVENTS("events");

        Table(String name) {
            mTableName = name;
        }

        public String getName() {
            return mTableName;
        }

        private final String mTableName;
    }

    private static final String KEY_DATA = "clickdata";
    private static final String KEY_CREATED_AT = "creattime";
    private static final String KEY_TOKEN = "token";

    private static final String DATABASE_NAME = "thinkingdata";
    private static final int DB_VERSION = 1;

    private static final String CREATE_EVENTS_TABLE =
            "CREATE TABLE " + Table.EVENTS.getName() + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_DATA + " TEXT NOT NULL, " +
                    KEY_CREATED_AT + " INTEGER NOT NULL, " +
                    KEY_TOKEN + " TEXT NOT NULL DEFAULT '')";

    private static final String EVENTS_TIME_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.EVENTS.getName() +
                    " (" + KEY_CREATED_AT + ");";

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private final File mDatabaseFile;
        private final int mMinimumDatabaseLimit;

        public DatabaseHelper(Context context, String name) {
            super(context, name, null, DB_VERSION);
            mDatabaseFile = context.getDatabasePath(name);
            mMinimumDatabaseLimit = TDContextConfig.getInstance(context).getMinimumDatabaseLimit();
        }

        void deleteDatabase() {
            close();
            mDatabaseFile.delete();
        }

        boolean belowMemThreshold() {
            if (mDatabaseFile.exists()) {
                return Math.max(mDatabaseFile.getUsableSpace(), mMinimumDatabaseLimit) >= mDatabaseFile.length();
            }
            return true;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            TDLog.d(TAG, "Creating a new ThinkingData events database");

            db.execSQL(CREATE_EVENTS_TABLE);
            db.execSQL(EVENTS_TIME_INDEX);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            TDLog.d(TAG, "Upgrading ThinkingData events database");

            db.execSQL("DROP TABLE IF EXISTS " + Table.EVENTS.getName());
            db.execSQL(CREATE_EVENTS_TABLE);
            db.execSQL(EVENTS_TIME_INDEX);
        }
    }

    private static final int DB_UPDATE_ERROR = -1;
    private static final int DB_OUT_OF_MEMORY_ERROR = -2;

    private static final Map<Context, DatabaseAdapter> sInstances = new HashMap<>();
    private final DatabaseHelper mDb;

    static DatabaseAdapter getInstance(Context context) {
        synchronized (sInstances) {
            final Context appContext = context.getApplicationContext();
            DatabaseAdapter ret;
            if (! sInstances.containsKey(appContext)) {
                ret = new DatabaseAdapter(appContext);
                sInstances.put(appContext, ret);
            } else {
                ret = sInstances.get(appContext);
            }
            return ret;
        }
    }

    static boolean dbNotExist(Context context) {
        return !(context.getDatabasePath(DATABASE_NAME).exists() ||
                context.getDatabasePath(context.getPackageName()).exists());
    }

    DatabaseAdapter(Context context) {
        this(context, DATABASE_NAME);
    }

    DatabaseAdapter(Context context, String dbName) {
        mDb = new DatabaseHelper(context, dbName);

        // 迁移数据，删除老数据库
        try {
            File oldDatabase = context.getDatabasePath(context.getPackageName());
            if (oldDatabase.exists()) {
                OldDatabaseHelper oldDatabaseHelper = new OldDatabaseHelper(context, context.getPackageName());
                JSONArray oldEvents = oldDatabaseHelper.getAllEvents();
                for (int i = 0; i < oldEvents.length(); i++) {
                    try {
                        JSONObject event = oldEvents.getJSONObject(i);
                        final ContentValues cv = new ContentValues();
                        cv.put(KEY_DATA, event.getString(KEY_DATA));
                        cv.put(KEY_CREATED_AT, event.getString(KEY_CREATED_AT));

                        TDLog.d(TAG, cv.toString());
                        SQLiteDatabase database = mDb.getWritableDatabase();
                        database.insert(Table.EVENTS.getName(), null, cv);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                oldDatabase.delete();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class OldDatabaseHelper extends SQLiteOpenHelper {
        OldDatabaseHelper(Context context, String dbName) {
            super(context, dbName, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        JSONArray getAllEvents() {
            Cursor c = null;
            final JSONArray events = new JSONArray();
            try {
                final SQLiteDatabase db = getReadableDatabase();
                c = db.rawQuery("SELECT * FROM " + Table.EVENTS + " ORDER BY " + KEY_CREATED_AT, null);
                while (c.moveToNext()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(KEY_CREATED_AT, c.getString(c.getColumnIndex(KEY_CREATED_AT)));
                    jsonObject.put(KEY_DATA, c.getString(c.getColumnIndex(KEY_DATA)));
                    events.put(jsonObject);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close();
                if (c != null) {
                    c.close();
                }
            }
            return events;
        }
    }


    private boolean belowMemThreshold() {
        return mDb.belowMemThreshold();
    }

    private static final String KEY_DATA_SPLIT_SEPARATOR = "#td#";

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j     the JSON to record
     * @param table the table to insert into
     * @param token the token this event belongs to
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j, Table table, String token) {
        // we are aware of the race condition here, but what can we do..?
        if (!this.belowMemThreshold()) {
            TDLog.d(TAG, "There is not enough space left on the device to store td data, oldest data will be deleted");
            String[] eventsData = generateDataString(table, null, 100);
            if (eventsData == null) {
                return DB_OUT_OF_MEMORY_ERROR;
            }
            final String lastId = eventsData[0];
            int count = cleanupEvents(lastId, Table.EVENTS, null);
            if (count <= 0) {
                return DB_OUT_OF_MEMORY_ERROR;
            }
        }

        final String tableName = table.getName();

        int count = DB_UPDATE_ERROR;
        Cursor c = null;

        try {
            final SQLiteDatabase db = mDb.getWritableDatabase();

            final ContentValues cv = new ContentValues();
            cv.put(KEY_DATA, j.toString() + KEY_DATA_SPLIT_SEPARATOR + j.toString().hashCode());
            cv.put(KEY_CREATED_AT, System.currentTimeMillis());
            cv.put(KEY_TOKEN, token);
            db.insert(tableName, null, cv);

            c = db.rawQuery("SELECT COUNT(*) FROM " + tableName + " WHERE token='" + token + "'", null);
            c.moveToFirst();
            count = c.getInt(0);

        } catch (final SQLiteException e) {
            TDLog.e(TAG, "could not add data to table " + tableName + ". Re-initializing database.", e);
            if (c != null) {
                c.close();
            }
            mDb.deleteDatabase();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } finally {

            }
        }
        return count;
    }

    /**
     * Removes events with an _id <= last_id from table
     * @param last_id the last id to delete
     * @param table   the table to remove events from
     * @param token the project token; if null, delete all related events.
     * @return the number of rows in the table
     */
    public int cleanupEvents(String last_id, Table table, String token) {
        int count;
        Cursor c = null;
        String tableName = table.getName();

        try {
            final SQLiteDatabase db = mDb.getWritableDatabase();
            StringBuilder deleteQuery = new StringBuilder("_id <= ");
            deleteQuery.append(last_id);
            if (null != token) {
                deleteQuery.append(" AND ");
                deleteQuery.append(KEY_TOKEN);
                deleteQuery.append(" = '");
                deleteQuery.append(token);
                deleteQuery.append("'");
            }
            db.delete(tableName, deleteQuery.toString(), null);

            StringBuilder countQuery = new StringBuilder("SELECT COUNT(*) FROM " + tableName);
            if (null != token) {
                countQuery.append(" WHERE token='");
                countQuery.append(token);
                countQuery.append("'");
            }
            c = db.rawQuery(countQuery.toString(), null);
            c.moveToFirst();
            count = c.getInt(0);
        } catch (SQLiteException e) {
            TDLog.e(TAG, "could not clean data from " + tableName, e);
            if (c != null) c.close();
            mDb.deleteDatabase();
            count = DB_UPDATE_ERROR;
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } finally {

            }
        }
        return count;
    }

    public void cleanupEvents(Table table, String token) {
        final String tableName = table.getName();

        try {
            final SQLiteDatabase db = mDb.getWritableDatabase();
            db.delete(tableName, KEY_TOKEN + " = '" + token + "'", null);
        } catch (final SQLiteException e) {
            TDLog.e(TAG, "Could not clean records. Re-initializing database.", e);
            mDb.deleteDatabase();
        }
    }

    /**
     * Removes events before time
     * @param time the unix epoch in milliseconds to remove events before
     * @param table the table to remove events from
     */
    public void cleanupEvents(long time, Table table) {
        final String tableName = table.getName();

        try {
            final SQLiteDatabase db = mDb.getWritableDatabase();
            db.delete(tableName, KEY_CREATED_AT + " <= " + time, null);
        } catch (final SQLiteException e) {
            TDLog.e(TAG, "Could not clean timed-out records. Re-initializing database.", e);
            mDb.deleteDatabase();
        }
    }

    /**
     * Returns the data string to send to the server and the maximum ID of the row that we are sending,
     * so we know what rows to delete when a track request was successful.
     * @param table the table to read the JSON from
     * @param token the token of the project you want to retrieve the records for
     * @param limit the maximum number of rows returned.
     * @return String array containing the maximum ID, the data string.
     */
    public String[] generateDataString(Table table, String token, int limit) {
        Cursor c = null;
        String data = null;
        String last_id = null;

        final String tableName = table.getName();
        try {
            final SQLiteDatabase db = mDb.getReadableDatabase();

            StringBuilder rawDataQuery = new StringBuilder("SELECT * FROM ");
            rawDataQuery.append(tableName);
            if (null != token) {
                rawDataQuery.append(" WHERE ");
                rawDataQuery.append(KEY_TOKEN);
                rawDataQuery.append(" = '");
                rawDataQuery.append(token);
                rawDataQuery.append("'");
            }
            rawDataQuery.append(" ORDER BY ");
            rawDataQuery.append(KEY_CREATED_AT);
            rawDataQuery.append(" ASC LIMIT ");
            rawDataQuery.append(limit);

            final JSONArray arr = new JSONArray();

            c = db.rawQuery(rawDataQuery.toString(), null);
            if (c != null) {
                while (c.moveToNext()) {
                    if (c.isLast()) {
                        last_id = c.getString(c.getColumnIndex("_id"));
                    }
                    try {
                        String keyData = c.getString(c.getColumnIndex(KEY_DATA));
                        if (!TextUtils.isEmpty(keyData)) {
                            int index = keyData.lastIndexOf(KEY_DATA_SPLIT_SEPARATOR);
                            if (index > -1) {
                                String hashCode = keyData.substring(index).replaceFirst(KEY_DATA_SPLIT_SEPARATOR, "");
                                String content = keyData.substring(0, index);
                                if (TextUtils.isEmpty(content) || TextUtils.isEmpty(hashCode)
                                        || !hashCode.equals(String.valueOf(content.hashCode()))) {
                                    continue;
                                }
                                keyData = content;
                            }
                            final JSONObject j = new JSONObject(keyData);
                            arr.put(j);
                        }
                    } catch (final JSONException e) {
                        // Ignore this object
                    }
                }

                if (arr.length() > 0) {
                    data = arr.toString();
                }
            }
        } catch (final SQLiteException e) {
            TDLog.e(TAG, "Could not pull records out of database " + tableName, e);
            last_id = null;
            data = null;
        } finally {
            if (c != null) {
                c.close();
            }
        }

        if (last_id != null && data != null) {
            final String[] ret = {last_id, data};
            return ret;
        }
        return null;
    }
}
