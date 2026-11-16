package com.paul.pipeline.manager;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PCR/RNA 结束任务专用的单线程队列执行器
 * 用于处理耗时的文件保存、数据库操作，避免阻塞 WebSocket 消息处理
 *
 * 特点：
 * 1. 单线程顺序执行，保证数据一致性
 * 2. 无界队列，不会因队列满而阻塞提交
 * 3. 独立线程池，不阻塞 EventBus 和主线程
 */
public class PCRFinishTaskExecutor {
    private static final String TAG = "PCRFinishTaskExecutor";
    private static volatile PCRFinishTaskExecutor instance;
    private final ExecutorService executor;

    private PCRFinishTaskExecutor() {
        // 创建单线程池，使用无界队列
        executor = new ThreadPoolExecutor(
            1, // corePoolSize - 单线程
            1, // maximumPoolSize
            0L, // keepAliveTime
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), // 无界队列，任务不会被拒绝
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "PCRFinishTask-" + threadNumber.getAndIncrement());
                    t.setDaemon(false); // 非守护线程，确保任务完成
                    t.setPriority(Thread.NORM_PRIORITY - 1); // 稍低优先级，不影响UI
                    return t;
                }
            }
        );
        Log.i(TAG, "PCRFinishTaskExecutor 初始化完成");
    }

    public static PCRFinishTaskExecutor getInstance() {
        if (instance == null) {
            synchronized (PCRFinishTaskExecutor.class) {
                if (instance == null) {
                    instance = new PCRFinishTaskExecutor();
                }
            }
        }
        return instance;
    }

    /**
     * 提交 PCR/RNA 结束任务
     * @param task 任务
     */
    public void submit(Runnable task) {
        if (task == null) {
            Log.e(TAG, "submit: task is null!");
            return;
        }

        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            String threadName = Thread.currentThread().getName();
            Log.i(TAG, "[" + threadName + "] 开始执行任务");

            try {
                task.run();
            } catch (Exception e) {
                Log.e(TAG, "[" + threadName + "] 任务执行异常: " + e.getMessage(), e);
            } finally {
                long cost = System.currentTimeMillis() - startTime;
                Log.i(TAG, "[" + threadName + "] 任务执行完成，耗时: " + cost + "ms");
            }
        });
    }

    /**
     * 关闭执行器（通常不需要调用，除非应用退出）
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            Log.w(TAG, "关闭 PCRFinishTaskExecutor");
            executor.shutdown();
        }
    }
}
