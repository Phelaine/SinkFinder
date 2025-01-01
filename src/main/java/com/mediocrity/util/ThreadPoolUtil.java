package com.mediocrity.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class ThreadPoolUtil {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolUtil.class);

    private volatile static ThreadPoolExecutor llmExecutor;
    private volatile static ThreadPoolExecutor resultProcessExecutor;

    static {
        initLLMExecutor();
        initResultProcessExecutor();
    }

    private static void initLLMExecutor() {

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("LLMCompute-pool-%d").build();
        // 线程池
        llmExecutor = new ThreadPoolExecutor(2 * Runtime.getRuntime().availableProcessors() + 1, 3 * Runtime.getRuntime().availableProcessors(),
                1000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1000), namedThreadFactory, new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                // execute提交
                if (t != null) {
                    t.printStackTrace();
                }
                // r的实际类型是FutureTask 那么是submit提交的，所以可以在里面get到异常
                if (r instanceof FutureTask) {
                    try {
                        Future<?> future = (Future<?>) r;
                        future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        };

    }

    public static void initResultProcessExecutor() {

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("ResultProcess-pool-%d").build();
        // 线程池
        resultProcessExecutor = new ThreadPoolExecutor(2 * Runtime.getRuntime().availableProcessors() + 1, 3 * Runtime.getRuntime().availableProcessors(),
                1000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1000), namedThreadFactory, new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                // execute提交
                if (t != null) {
                    t.printStackTrace();
                }
                // r的实际类型是FutureTask 那么是submit提交的，所以可以在里面get到异常
                if (r instanceof FutureTask) {
                    try {
                        Future<?> future = (Future<?>) r;
                        future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        };
    }

    public static ThreadPoolExecutor getPoolExecutor(ThreadType threadPoolType) {
        ThreadPoolExecutor threadPoolExecutor = null;
        switch (threadPoolType) {
            case LLM:
                logger.info("Invoke LLMing......");
                threadPoolExecutor = llmExecutor;
                break;
            case ResultProcess:
                logger.info("Result processing......");
                threadPoolExecutor = resultProcessExecutor;
                break;
            default:
                threadPoolExecutor = resultProcessExecutor;

        }
        return threadPoolExecutor;
    }

    public static <T> Future<T> submit(Callable<T> task, ThreadType type) {
        return getPoolExecutor(type).submit(task);
    }

    public static void execute(Runnable task, ThreadType type) {
        getPoolExecutor(type).execute(task);
    }
}
