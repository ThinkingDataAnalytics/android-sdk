/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.analytics.utils;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Choreographer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import cn.thinkingdata.analytics.data.TDContextConfig;
import cn.thinkingdata.core.utils.ProcessUtil;

/**
 * @author liulongbing
 * @since 2024/10/11
 */
public class CommonUtil {

    static long firstVsync;
    static long secondVsync;
    static volatile int fps;
    static final Object frameLock = new Object();

    public static boolean isMainProcess(Context context) {
        if (context == null) {
            return true;
        }
        String currentProcess = ProcessUtil.getCurrentProcessName(context);
        String mainProcess = getMainProcessName(context);
        return !TextUtils.isEmpty(currentProcess) && mainProcess.equals(currentProcess);
    }

    public static String getMainProcessName(Context context) {
        String processName = "";
        if (context == null) {
            return "";
        }
        TDContextConfig contextConfig = TDContextConfig.getInstance(context);
        processName = contextConfig.getMainProcessName();
        if (processName.length() == 0) {
            try {
                processName = context.getApplicationInfo().processName;
            } catch (Exception ex) {
                //ignored
            }
        }
        return processName;
    }

    public static int getFPS() {
        if (fps == 0) {
            fps = 60;
        }
        return fps;
    }

    public static void listenFPS() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final Choreographer.FrameCallback secondCallBack = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    synchronized (frameLock) {
                        secondVsync = frameTimeNanos;
                        if (secondVsync <= firstVsync) {
                            fps = 60;
                        } else {
                            try {
                                long hz = 1000000000 / (secondVsync - firstVsync);
                                if (hz > 70) {
                                    fps = 60;
                                } else {
                                    fps = ( int ) hz;
                                }
                            } catch (Exception e) {
                                fps = 60;
                            }
                        }
                    }
                }
            };

            final Choreographer.FrameCallback firstCallBack = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    synchronized (frameLock) {
                        firstVsync = frameTimeNanos;
                        Choreographer.getInstance().postFrameCallback(secondCallBack);
                    }
                }
            };
            final Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    handler.postDelayed(this, 500);
                    Choreographer.getInstance().postFrameCallback(firstCallBack);
                }
            };
            handler.postDelayed(runnable, 500);
        }
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static JSONObject cloneJsonObject(JSONObject json) {
        if (json == null) return null;
        JSONObject cloneJson = new JSONObject();
        try {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);
                if (value instanceof JSONObject) {
                    cloneJson.put(key, cloneJsonObject(( JSONObject ) value));
                } else if (value instanceof JSONArray) {
                    cloneJson.put(key, cloneJsonArray(( JSONArray ) value));
                } else {
                    cloneJson.put(key, value);
                }
            }
        } catch (JSONException e) {
            cloneJson = json;
        }
        return cloneJson;
    }

    public static JSONArray cloneJsonArray(JSONArray array) {
        if (array == null) return null;
        JSONArray cloneArray = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) {
                cloneArray.put(cloneJsonObject(( JSONObject ) value));
            } else if (value instanceof JSONArray) {
                cloneArray.put(cloneJsonArray(( JSONArray ) value));
            } else {
                cloneArray.put(value);
            }
        }
        return cloneArray;
    }


}
