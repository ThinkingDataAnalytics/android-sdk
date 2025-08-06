/*
 * Copyright (C) 2022 ThinkingData
 */

package cn.thinkingdata.analytics.autotrack;

import android.content.Context;
import android.content.res.Resources;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.thinkingdata.analytics.TDPresetProperties;
import cn.thinkingdata.analytics.ThinkingAnalyticsSDK;
import cn.thinkingdata.analytics.crash.CrashLogListener;
import cn.thinkingdata.analytics.utils.PropertyUtils;
import cn.thinkingdata.analytics.utils.TDConstants;
import cn.thinkingdata.analytics.utils.TDDebugException;
import cn.thinkingdata.core.utils.TDLog;

/**
 * Exception catch initializer class.
 * */
public class TAExceptionHandler {
    static final String TAG = "ThinkingAnalytics.ExceptionHandler";

    private static TAExceptionHandler sInstance;
    private final Context mContext;
    private boolean mExceptionHandlerInitialed;
    //private static int EXCEPTION_HANDLE_DELAY_MS = 3000;

    private TAExceptionHandler(Context context) {
        mContext = context.getApplicationContext();
    }

    public synchronized void initExceptionHandler() {
        if (!mExceptionHandlerInitialed) {
            final List<String> crashEnabledList = new ArrayList<>();
            try {
                Resources resources = mContext.getResources();
                String[] array = resources.getStringArray(resources.getIdentifier(
                        "TACrashConfig", "array", mContext.getPackageName()));
                crashEnabledList.addAll(Arrays.asList(array));
            } catch (Exception e) {
            //ignored
            }
            if (crashEnabledList.isEmpty()) {
                //only java
                new JavaExceptionHandler();
            } else {
                //init log listener
                CrashLogListener listener = new CrashLogListener() {
                    @Override
                    public void onFile(final File logFile) {
                        final String result = readFileContent(logFile);
                        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
                            @Override
                            public void process(ThinkingAnalyticsSDK instance) {
                                if (instance.mTrackCrash) {
                                    try {
                                        final JSONObject messageProp = new JSONObject();
                                        try {
                                            if (result.getBytes("UTF-8").length > 1024 * 16) { // #app_crashed_reason max length 16 KB
                                                if (!TDPresetProperties.disableList.contains(TDConstants.KEY_CRASH_REASON)) {
                                                    messageProp.put(TDConstants.KEY_CRASH_REASON,
                                                            new String(PropertyUtils.cutToBytes(result, 1024 * 16), "UTF-8"));
                                                }
                                            } else {
                                                if (!TDPresetProperties.disableList.contains(TDConstants.KEY_CRASH_REASON)) {
                                                    messageProp.put(TDConstants.KEY_CRASH_REASON, result);
                                                }
                                            }
                                        } catch (UnsupportedEncodingException e) {
                                            if (result.length() > 1024 * 16 / 2 && !TDPresetProperties.disableList.contains(TDConstants.KEY_CRASH_REASON)) {
                                                messageProp.put(TDConstants.KEY_CRASH_REASON, result.substring(0, 1024 * 16 / 2));
                                            }
                                        }
                                        //track crash & end
                                        instance.trackAppCrashAndEndEvent(messageProp);
                                        //delete log
                                        logFile.delete();
                                    } catch (JSONException e) {
                                        //ignore
                                    }
                                }
                            }
                        });
                    }
                };
                //check log file
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        checkLocalLog(mContext);
                    }
                }).start();

                try {
                    //init crash
                    Class<?> clazzCrash = Class.forName("cn.thinkingdata.android.crash.TACrash");
                    Method methodCrashInstance = clazzCrash.getMethod("getInstance");
                    Object clazzCrashObj = methodCrashInstance.invoke(null);
                    //init crash config
                    Method methodInit = clazzCrash.getMethod("init", Context.class);
                    methodInit.invoke(clazzCrashObj, mContext);
                    //enable log
                    Method methodEnableLog = clazzCrash.getMethod("enableLog");
                    methodEnableLog.invoke(clazzCrashObj);
                    if (crashEnabledList.contains("java")) {
                        //init java crash handler
                        Method methodInitJava = clazzCrash.getMethod("initJavaCrashHandler", boolean.class);
                        methodInitJava.invoke(clazzCrashObj, true);
                    }
                    if (crashEnabledList.contains("anr") || crashEnabledList.contains("native")) {
                        //init native crash handler
                        Method methodInitNative = clazzCrash.getMethod("initNativeCrashHandler",
                                boolean.class, boolean.class, boolean.class, boolean.class);
                        methodInitNative.invoke(clazzCrashObj, true, true, true, true);
                        if (crashEnabledList.contains("anr")) {
                            //init anr crash handler
                            Method methodInitANR = clazzCrash.getMethod("initANRHandler");
                            methodInitANR.invoke(clazzCrashObj);
                        }
                    }
                    //set log listener
                    Method methodInitListener = clazzCrash.getMethod("initCrashLogListener", CrashLogListener.class);
                    methodInitListener.invoke(clazzCrashObj, listener);
                } catch (Exception e) {
                    //ignored
                }
            }
            mExceptionHandlerInitialed = true;
        }
    }

    private void checkLocalLog(Context mContext) {
        String logDir = mContext.getCacheDir().getAbsolutePath() + File.separator + "tacrash";
        CrashLogListener logListener = new CrashLogListener() {
            @Override
            public void onFile(final File logFile) {
                final String result = readFileContent(logFile);
                ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
                    @Override
                    public void process(ThinkingAnalyticsSDK instance) {
                        if (instance.mTrackCrash) {
                            try {
                                final JSONObject messageProp = new JSONObject();
                                try {
                                    if (result.getBytes("UTF-8").length > 1024 * 16) { // #app_crashed_reason max length 16 KB
                                        if (!TDPresetProperties.disableList.contains(TDConstants.KEY_CRASH_REASON)) {
                                            messageProp.put(TDConstants.KEY_CRASH_REASON,
                                                    new String(PropertyUtils.cutToBytes(result, 1024 * 16), "UTF-8"));
                                        }
                                    } else {
                                        if (!TDPresetProperties.disableList.contains(TDConstants.KEY_CRASH_REASON)) {
                                            messageProp.put(TDConstants.KEY_CRASH_REASON, result);
                                        }
                                    }
                                } catch (UnsupportedEncodingException e) {
                                    if (result.length() > 1024 * 16 / 2 && !TDPresetProperties.disableList.contains(TDConstants.KEY_CRASH_REASON)) {
                                        messageProp.put(TDConstants.KEY_CRASH_REASON, result.substring(0, 1024 * 16 / 2));
                                    }
                                }
                                //track crash
                                instance.autoTrack(TDConstants.APP_CRASH_EVENT_NAME, messageProp);
                                //delete log
                                logFile.delete();
                            } catch (JSONException e) {
                                //ignore
                            }
                        }
                    }
                });
            }
        };

        File logF = new File(logDir);
        if (logF.exists()) {
            File[] logs = logF.listFiles();
            if (logs != null) {
                for (File log : logs) {
                    logListener.onFile(log);
                }
            }
        }
    }

    public static TAExceptionHandler getInstance(Context context) {
        if (sInstance == null) {
            if (context == null) {
                return null;
            }
            synchronized (JavaExceptionHandler.class) {
                if (sInstance == null) {
                    sInstance = new TAExceptionHandler(context);
                }
            }
        }
        return sInstance;
    }

    private static class JavaExceptionHandler implements Thread.UncaughtExceptionHandler {

        private static final int CRASH_REASON_LENGTH_LIMIT = 1024 * 16; // CRASH REASON The default attribute length limit is 16K.

        private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

        JavaExceptionHandler() {
            mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(this);
        }

        private void processException(final Throwable e) {

            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            Throwable cause = e.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            printWriter.close();

            final String result = writer.toString().replaceAll("(\r\n|\n\r|\n|\r)", "<br>");
            // Only one worker thread - giving priority to storing the event first and then flush
            ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
                @Override
                public void process(ThinkingAnalyticsSDK instance) {
                    if (instance.mTrackCrash) {
                        try {
                            final JSONObject messageProp = new JSONObject();

                            try {
                                if (result.getBytes("UTF-8").length > CRASH_REASON_LENGTH_LIMIT) { // #app_crashed_reason The maximum length is 16 KB
                                    if (!TDPresetProperties.disableList.contains(TDConstants.KEY_CRASH_REASON)) {
                                        messageProp.put(TDConstants.KEY_CRASH_REASON,
                                                new String(PropertyUtils.cutToBytes(result, CRASH_REASON_LENGTH_LIMIT), "UTF-8"));
                                    }
                                } else {
                                    if (!TDPresetProperties.disableList.contains(TDConstants.KEY_CRASH_REASON)) {
                                        messageProp.put(TDConstants.KEY_CRASH_REASON, result);
                                    }
                                }
                            } catch (UnsupportedEncodingException e) {
                                TDLog.d(TAG, "Exception occurred in getBytes. ");
                                if (result.length() > CRASH_REASON_LENGTH_LIMIT / 2 && !TDPresetProperties.disableList.contains(TDConstants.KEY_CRASH_REASON)) {
                                    messageProp.put(TDConstants.KEY_CRASH_REASON, result.substring(0, CRASH_REASON_LENGTH_LIMIT / 2));
                                }
                            }
                            instance.trackAppCrashAndEndEvent(messageProp);
                        } catch (JSONException e) {
                            //ignore
                        }
                    }
                }
            });
        }

        @Override
        public void uncaughtException(final Thread t, final Throwable e) {

            boolean notTDDebugException = true;
            Throwable cause = e;
            while (null != cause) {
                if (cause instanceof TDDebugException) {
                    notTDDebugException = false;
                    break;
                }
                cause = cause.getCause();
            }
            if (notTDDebugException) {
                processException(e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }

            if (mDefaultExceptionHandler != null) {
                //killProcessAndExit();
                mDefaultExceptionHandler.uncaughtException(t, e);
            } else {
                killProcessAndExit();
            }
        }


        private void killProcessAndExit() {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }

    static String readFileContent(File file) {
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
                sbf.append("\n");
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }
}

