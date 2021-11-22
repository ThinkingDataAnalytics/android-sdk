package cn.thinkingdata.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;

public class TestUtils {
    public static final String TAG = "TA_TEST.TestUtils";

    public static final String DATABASE_NAME = "thinkingdata";
    public static final int DB_VERSION = 1;
    public static final String TABLE_NAME = "events";

    public static final String KEY_DATA = "clickdata";
    public static final String KEY_CREATED_AT = "creattime";
    public static final String KEY_TOKEN = "token";

    public static final String KEY_DATA_SPLIT_SEPARATOR = "#td#";
    /**
     * 清除数据
     */
    public static void clearData(Context context) {
        Log.d(TAG, "clearData ->");
        //清实例
        Map<String, ThinkingAnalyticsSDK> map = ThinkingAnalyticsSDK.getInstanceMap(context);
        if (map != null) {
            for (String key : map.keySet()) {
                if (map.get(key).mConfig.getTDConfigMap() != null) {
                    map.get(key).mConfig.getTDConfigMap().clear();
                }
            }
        }
        if (ThinkingAnalyticsSDK.getInstanceMap(context) != null) {
            ThinkingAnalyticsSDK.getInstanceMap(context).clear();
        }
        //清sp
        File file = new File("/data/data/" + context.getPackageName() + "/shared_prefs");
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                String fName = f.getName();
                //*.xml
                SharedPreferences sharedPreferences = context.getSharedPreferences(fName.substring(0, fName.length() - 4), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
            }

        }
        //清数据库
        DatabaseHelper databaseHelper = new DatabaseHelper(context, DATABASE_NAME);
        databaseHelper.deleteDatabase();
    }

    // 日期转换成时间戳
    public static boolean convertStamp(String time) {
        SimpleDateFormat format = new SimpleDateFormat(TDConstants.TIME_PATTERN, Locale.CHINA);
        try {
            format.parse(time);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {

        private final File mDatabaseFile;

        public DatabaseHelper(Context context, String name) {
            super(context, name, null, DB_VERSION);
            mDatabaseFile = context.getDatabasePath(name);
        }

        public void deleteDatabase() {
            close();
            mDatabaseFile.delete();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }


        /**
         * 提取数据库中数据 时间倒序
         * */
        public JSONArray getFirstEvent(String token) {
            Cursor c = null;
            final JSONArray events = new JSONArray();
            try {
                final SQLiteDatabase db = getWritableDatabase();
                c = db.rawQuery("SELECT * FROM " + DatabaseAdapter.Table.EVENTS + " WHERE " + KEY_TOKEN + " = '" + token + "' ORDER BY " + KEY_CREATED_AT +" DESC ", null);
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


    public void enableWifi() {

    }

//    public static void disableWifi() {
//        Runtime runtime = Runtime.getRuntime();
//        try {
//            Log.d(TAG,"svc wifi enable" );
//            Process process = runtime.exec("svc wifi enable");
//            Log.d(TAG, "value :" + process.waitFor() + "--- : " + process.getErrorStream() + "inputSteam : " + process.getInputStream());
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }


}
