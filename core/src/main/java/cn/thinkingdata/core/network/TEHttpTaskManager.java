/*
 * Copyright (C) 2023 ThinkingData
 */
package cn.thinkingdata.core.network;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author liulongbing
 * @since 2023/5/4
 */
public class TEHttpTaskManager {

    private static final String THREAD_TE_NET = "TE.NetWorkTask";

    private volatile static ExecutorService executor = null;

    private TEHttpTaskManager() {
    }

    public static ExecutorService getExecutor() {
        if (null == executor) {
            synchronized (TEHttpTaskManager.class) {
                if (null == executor) {
                    executor = Executors.newCachedThreadPool(new ThreadFactoryWithName(THREAD_TE_NET));
                }
            }
        }
        return executor;
    }


    static class ThreadFactoryWithName implements ThreadFactory {

        private final String name;

        ThreadFactoryWithName(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name);
        }
    }
}
