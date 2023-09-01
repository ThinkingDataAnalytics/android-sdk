
package cn.thinkingdata.android.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.thinkingdata.android.utils.TDLog;

public class TrackTaskManager {

    private static final String TAG = "ThinkingAnalytics.TrackTaskManager";
    private static TrackTaskManager trackTaskManager;
    private static  TrackTaskManagerThread mTrackTaskManagerThread;

    private final LinkedBlockingQueue<Runnable> mTrackEventTasks;

    /**
     * 创建一个可重用固定线程数的线程池
     */
    private final ExecutorService mPool;
    private static final int POOL_SIZE = 1;

    private TrackTaskManager() {
        mTrackEventTasks = new LinkedBlockingQueue<>();
        mPool = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, ThreadNameConstants.THREAD_TASK_EXECUTE);
            }
        });
    }

    public static synchronized TrackTaskManager getInstance() {
        try {
            if (null == trackTaskManager) {
                trackTaskManager = new TrackTaskManager();
//                mTrackTaskManagerThread = new TrackTaskManagerThread();
//                new Thread(mTrackTaskManagerThread, ThreadNameConstants.THREAD_TASK_QUEUE).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trackTaskManager;
    }

    public void addTrackEventTask(Runnable trackEvenTask) {
        try {
//            mTrackEventTasks.put(trackEvenTask);
            mPool.execute(trackEvenTask);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Runnable takeTrackEventTask() {
        try {
            return mTrackEventTasks.take();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Runnable pollTrackEventTask() {
        try {
            return mTrackEventTasks.poll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isEmpty(){
        return mTrackEventTasks.isEmpty();
    }
}
