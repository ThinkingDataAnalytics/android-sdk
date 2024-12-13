
package cn.thinkingdata.analytics.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TrackTaskManager {

    private static volatile TrackTaskManager trackTaskManager;

    /**
     * 创建一个可重用固定线程数的线程池
     */
    private final ExecutorService mPool;
    private static final int POOL_SIZE = 1;

    private TrackTaskManager() {
        mPool = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, ThreadNameConstants.THREAD_TASK_EXECUTE);
            }
        });
    }

    public static TrackTaskManager getInstance() {
        if (null == trackTaskManager) {
            synchronized (TrackTaskManager.class) {
                if (null == trackTaskManager) {
                    trackTaskManager = new TrackTaskManager();
                }
            }
        }
        return trackTaskManager;
    }

    public void addTrackEventTask(Runnable trackEvenTask) {
        try {
            mPool.execute(trackEvenTask);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addTask(Runnable trackEvenTask) {
        try {
            mPool.execute(trackEvenTask);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Runnable takeTrackEventTask() {
//        try {
//            return mTrackEventTasks.take();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return null;
    }

    public Runnable pollTrackEventTask() {
//        try {
//            return mTrackEventTasks.poll();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return null;
    }

    public boolean isEmpty() {
//        return mTrackEventTasks.isEmpty();
        return true;
    }
}
