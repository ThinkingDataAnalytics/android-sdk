package cn.thinkingdata.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import cn.thinkingdata.android.utils.PropertyUtils;
import cn.thinkingdata.android.utils.TDConstants;
import cn.thinkingdata.android.utils.TDLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class TDQuitSafelyService {
    static final String TAG = "ThinkingAnalytics.Quit";

    private static TDQuitSafelyService sInstance;
    private Context mContext;
    private boolean mExceptionHandlerInitialed;

    private TDQuitSafelyService(Context context) {
        mContext = context.getApplicationContext();
        if (TDContextConfig.getInstance(mContext).quitSafelyEnabled()) {
            Thread shutDownHook = new ShutDownHooksThread();
            Runtime.getRuntime().addShutdownHook(shutDownHook);
            initExceptionHandler();
        }
    }

    synchronized void initExceptionHandler() {
        if (!mExceptionHandlerInitialed) {
            new ExceptionHandler();
            mExceptionHandlerInitialed = true;
        }
    }

    /**
     * 获取 TDQuitSafelyService 实例.
     *
     * 该实例用于保障 APP 退出时保存缓存中的数据，并尝试上报到服务器.
     *  - 未捕获异常 通过 ExceptionHandler 实现
     *  - exit(0) 退出，通过Runtime 的 shutdownHook 实现
     *  - 用户手动结束进程，通过 KeepAliveService 实现
     *
     * @param context app Context
     * @return TDQuitSafelyService 实例
     */
    static TDQuitSafelyService getInstance(Context context) {
        if (sInstance == null) {
            if (context == null) return null;
            synchronized (ExceptionHandler.class) {
                if (sInstance == null) {
                    sInstance = new TDQuitSafelyService(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * 在应用进入前台的时候调用此接口，防止应用退到后台后 Service 被回收导致无法正确保存数据
     */
    void start() {
        try {
            if (TDContextConfig.getInstance(mContext).quitSafelyEnabled()) {
                mContext.startService(new Intent(mContext, TDKeepAliveService.class));
            }
        } catch (Exception e) {
            TDLog.w(TAG, "Unexpected exception occurred: " + e.getMessage());
        }
    }

    private void quit() {
        if (TDContextConfig.getInstance(mContext).quitSafelyEnabled()) {
            TDLog.i(TAG, "The App is quiting...");

            ThinkingAnalyticsSDK.allInstances(new ThinkingAnalyticsSDK.InstanceProcessor() {
                @Override
                public void process(ThinkingAnalyticsSDK thinkingAnalyticsSDK) {
                    thinkingAnalyticsSDK.flush();
                }
            });

            quitSafely(DataHandle.THREAD_NAME_SAVE_WORKER, TDContextConfig.getInstance(mContext).getQuitSafelyTimeout());
            quitSafely(DataHandle.THREAD_NAME_SEND_WORKER, TDContextConfig.getInstance(mContext).getQuitSafelyTimeout());
            mContext.stopService(new Intent(mContext, TDKeepAliveService.class));
        }
    }

    private void quitSafely(String threadName, long timeout) {
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().equals(threadName)) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            if (t instanceof HandlerThread) {
                                Looper l = ((HandlerThread) t).getLooper();
                                if (null != l) {
                                    l.quitSafely();
                                    if (timeout > 0) {
                                        t.join(timeout);
                                    } else {
                                        t.join(500);
                                    }
                                }
                            }
                        } else {
                            // Just wait for sending exception data
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ShutDownHooksThread extends Thread {

        @Override
        public void run() {
            super.run();
            TDLog.d(TAG, "ShutdownHook start");
            quit();
            TDLog.d(TAG, "ShutdownHook end");
        }
    }

    private class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        private static final int CRASH_REASON_LENGTH_LIMIT = 1024 * 16; // CRASH REASON 属性长度限制。默认 16 K。

        private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

        ExceptionHandler() {
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
                    if (instance.shouldTrackCrash()) {
                        try {
                            final JSONObject messageProp = new JSONObject();

                            try {
                                if (result.getBytes("UTF-8").length > CRASH_REASON_LENGTH_LIMIT) { // #app_crashed_reason 最大长度 16 KB
                                    messageProp.put(TDConstants.KEY_CRASH_REASON,
                                            new String(PropertyUtils.cutToBytes(result, CRASH_REASON_LENGTH_LIMIT), "UTF-8"));
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

            quit();
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
            } else {
                mContext.stopService(new Intent(mContext, TDKeepAliveService.class));
            }

            if (mDefaultExceptionHandler != null) {
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

    public static class TDKeepAliveService extends Service {

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            TDLog.d(TAG, "onStartCommand: pid=" + android.os.Process.myPid());
            return START_NOT_STICKY;
        }

        @Override
        public void onTaskRemoved(Intent rootIntent) {
            super.onTaskRemoved(rootIntent);
            getInstance(this).quit();
        }

        @Override
        public void onDestroy() {
            TDLog.d(TAG, "KeepAliveService onDestroy");
            super.onDestroy();
        }
    }
}

