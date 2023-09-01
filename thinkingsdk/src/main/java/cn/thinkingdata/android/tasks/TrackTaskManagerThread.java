/*
 * Created by dengshiwei on 2022/09/13.
 * Copyright 2015－2022 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.thinkingdata.android.tasks;

import cn.thinkingdata.android.tasks.TrackTaskManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TrackTaskManagerThread implements Runnable {
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private static final int POOL_SIZE = 1;

    private TrackTaskManager mTrackTaskManager;
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private ExecutorService mPool;
    /**
     * 是否停止
     */
    private boolean isStop = false;

    public TrackTaskManagerThread() {
        try {
            this.mTrackTaskManager = TrackTaskManager.getInstance();
            mPool = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, ThreadNameConstants.THREAD_TASK_EXECUTE);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (!isStop) {
                Runnable downloadTask = mTrackTaskManager.takeTrackEventTask();
                mPool.execute(downloadTask);
            }
            while (true) {
                Runnable downloadTask = mTrackTaskManager.pollTrackEventTask();
                if (downloadTask == null) {
                    break;
                }
                mPool.execute(downloadTask);
            }
            mPool.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isStop = true;
        if (mTrackTaskManager.isEmpty()) {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }

    public boolean isStopped() {
        return isStop;
    }
}
