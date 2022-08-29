package cn.thinkingdata.android;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
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
    public static WifiManager wifiManager;
    //测试报告地址  线上 http://10.1.0.168:5000/save  本地 http://192.168.20.52/save
    private static String testReportServer = "http://10.1.0.168:5000/save";

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

    /**
     * 推送测试结果到服务器
     */
    public static void postToServer(TestProperties testProperties) {
        new Thread(() -> {
            OutputStreamWriter out = null;
            BufferedReader in = null;
            StringBuilder result = new StringBuilder();
            HttpURLConnection conn = null;
            try {
                URL url = new URL(testReportServer);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                //发送POST请求必须设置为true
                conn.setDoOutput(true);
                conn.setDoInput(true);
                //设置连接超时时间和读取超时时间
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                //获取输出流
                out = new OutputStreamWriter(conn.getOutputStream());
//                Log.d(TAG,testProperties.toString() );
                out.write(testProperties.toString());
                out.flush();
                out.close();
                //取得输入流，并使用Reader读取
                if (200 == conn.getResponseCode()) {
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    String line;
                    while ((line = in.readLine()) != null) {
                        result.append(line);
                    }
                } else {
                    Log.d(TAG, "ResponseCode is an error code:" + conn.getResponseCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
//            Log.d(TAG, "post result:" + result.toString());
        }).start();
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {

        private final File mDatabaseFile;

        public DatabaseHelper(Context context, String name) {
            super(context, name, null, DB_VERSION);
            mDatabaseFile = context.getDatabasePath(name);
        }

        public void deleteDatabase() {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(DatabaseAdapter.Table.EVENTS.getName(), "creattime > 0", null);
            db.close();
        }

        public void insertData(JSONObject j, String token, Context context) {
            final String tableName = DatabaseAdapter.Table.EVENTS.getName();
            Cursor c = null;
            DatabaseHelper mDb = new DatabaseHelper(context, "thinkingdata");
            SQLiteDatabase db = mDb.getWritableDatabase();

            try {
                final ContentValues cv = new ContentValues();
                cv.put(KEY_DATA, j.toString() + KEY_DATA_SPLIT_SEPARATOR + j.toString().hashCode());
                cv.put(KEY_CREATED_AT, System.currentTimeMillis());
                cv.put(KEY_TOKEN, token);
                db.insert(tableName, null, cv);
                db.close();
            } catch (final SQLiteException e) {
                TDLog.e(TAG, "could not add data to table " + tableName + ". Re-initializing database.", e);
                if (c != null) {
                    c.close();
                }

            } finally {
                try {
                    if (c != null) {
                        c.close();
                    }
                } finally {

                }
            }
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }


        /**
         * 提取数据库中数据 时间倒序
         */
        public JSONArray getFirstEvent(String token) {
            Cursor c = null;
            final JSONArray events = new JSONArray();
            try {
                final SQLiteDatabase db = getWritableDatabase();
                c = db.rawQuery("SELECT * FROM " + DatabaseAdapter.Table.EVENTS + " WHERE " + KEY_TOKEN + " = '" + token + "' ORDER BY " + KEY_CREATED_AT + " DESC ", null);
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
            Log.i(TAG, String.valueOf(events));
            return events;
        }

    }

}
