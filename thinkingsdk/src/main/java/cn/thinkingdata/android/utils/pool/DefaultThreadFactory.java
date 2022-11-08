/*
 * Copyright (C) 2022 ThinkingData
 */
package cn.thinkingdata.android.utils.pool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import cn.thinkingdata.android.utils.TDLog;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2022/8/12
 * @since
 */
public class DefaultThreadFactory implements ThreadFactory {

    private static final String TAG = "ThinkingAnalytics.DefaultThreadFactory";

    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final String namePrefix;

    public DefaultThreadFactory() {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        namePrefix = "task pool No." + poolNumber.getAndIncrement() + ", thread No.";
    }

    public Thread newThread(Runnable runnable) {
        String threadName = namePrefix + threadNumber.getAndIncrement();
        TDLog.i(TAG, "Thread production, name is [" + threadName + "]");
        Thread thread = new Thread(group, runnable, threadName, 0);
        if (thread.isDaemon()) {   //设为非后台线程
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) { //优先级为normal
            thread.setPriority(Thread.NORM_PRIORITY);
        }

        // 捕获多线程处理中的异常
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                TDLog.i(TAG, "Running task appeared exception! Thread [" + thread.getName() + "], because [" + ex.getMessage() + "]");
            }
        });
        return thread;
    }
}
