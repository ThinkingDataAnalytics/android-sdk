package com.thinking.analyselibrary;

import android.os.Build;
import android.os.HandlerThread;

import com.thinking.analyselibrary.utils.TDConstants;
import com.thinking.analyselibrary.utils.TDLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "ThinkingAnalyticsSDK.Exception";

    private static final int JOIN_TIMEOUT_MS = 3000; // 设置等待超时时长

    private static final int CRASH_REASON_LENGTH_LIMIT = 1024 * 16; // CRASH REASON 属性长度限制。默认 16 K。

    private static ExceptionHandler sInstance;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    public ExceptionHandler() {
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static void init() {
        if (sInstance == null) {
            synchronized (ExceptionHandler.class) {
                if (sInstance == null) {
                    sInstance = new ExceptionHandler();
                }
            }
        }
    }

    // cut string by byte limitations
    private static byte[] cutToBytes(String s, int charLimit) throws UnsupportedEncodingException {
        byte[] utf8 = s.getBytes("UTF-8");
        if (utf8.length <= charLimit) {
            return utf8;
        }
        if ((utf8[charLimit] & 0x80) == 0) {
            // the limit doesn't cut an UTF-8 sequence
            return Arrays.copyOf(utf8, charLimit);
        }
        int i = 0;
        while ((utf8[charLimit-i-1] & 0x80) > 0 && (utf8[charLimit-i-1] & 0x40) == 0) {
            ++i;
        }
        if ((utf8[charLimit-i-1] & 0x80) > 0) {
            // we have to skip the starter UTF-8 byte
            return Arrays.copyOf(utf8, charLimit-i-1);
        } else {
            // we passed all UTF-8 bytes
            return Arrays.copyOf(utf8, charLimit-i);
        }
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
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
                if (instance.shouldTrackCrash()) {
                    try {
                        final JSONObject messageProp = new JSONObject();

                        try {
                            if (result.getBytes("UTF-8").length > CRASH_REASON_LENGTH_LIMIT) { // #app_crashed_reason 最大长度 16 KB
                                messageProp.put(TDConstants.KEY_CRASH_REASON,
                                        new String(cutToBytes(result, CRASH_REASON_LENGTH_LIMIT), "UTF-8"));
                            } else {
                                messageProp.put(TDConstants.KEY_CRASH_REASON, result);
                            }
                        } catch (UnsupportedEncodingException e) {
                            TDLog.d(TAG, "Exception occurred in getBytes. ");
                            if (result.length() > CRASH_REASON_LENGTH_LIMIT / 2) {
                                messageProp.put(TDConstants.KEY_CRASH_REASON, result.substring(0, CRASH_REASON_LENGTH_LIMIT / 2));
                            }
                        }
                        instance.autoTrack(TDConstants.APP_CRASH_EVENT_NAME, messageProp);
                    } catch (JSONException e) {
                    }
                }
            }
        });


        quitSafely(DataHandle.THREAD_NAME_SAVE_WORKER, 0);

        ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
            @Override
            public void process(ThinkingAnalyticsSDK thinkingAnalyticsSDK) {
                thinkingAnalyticsSDK.flush();
            }
        });

        quitSafely(DataHandle.THREAD_NAME_SEND_WORKER, JOIN_TIMEOUT_MS);

        if (mDefaultExceptionHandler != null) {
            mDefaultExceptionHandler.uncaughtException(t, e);
        } else {
            killProcessAndExit();
        }
    }

    private void quitSafely(String threadName, long timeout) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName)) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        if (t instanceof  HandlerThread) {
                            ((HandlerThread) t).getLooper().quitSafely();
                            if (timeout > 0) {
                                t.join(timeout);
                            } else {
                                t.join();
                            }
                        }
                    } else {
                        // Just wait for sending exception data
                        t.join(timeout);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void killProcessAndExit() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }


}
