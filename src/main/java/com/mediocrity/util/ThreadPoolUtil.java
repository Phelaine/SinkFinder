package com.mediocrity.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class ThreadPoolUtil {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolUtil.class);

    private volatile static ThreadPoolExecutor executor;

    static {
        initExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                logger.info("AsyncProcessor shutting down.");

                executor.shutdown();

                try {
                    // 等待1秒执行关闭
                    if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        logger.error("AsyncProcessor shutdown immediately due to wait timeout.");
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    logger.error("AsyncProcessor shutdown interrupted.");
                    executor.shutdownNow();
                }

                logger.info("AsyncProcessor shutdown complete.");
            }
        }));
    }

    private static void initExecutor() {

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Compute-pool-%d").build();
        // 线程池
        executor = new ThreadPoolExecutor(2 * Runtime.getRuntime().availableProcessors() + 1, 3 * Runtime.getRuntime().availableProcessors(),
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

    public static <T> Future<T> submit(Callable<T> task) {
        logger.info("异步调用大模型能力中......");
        return executor.submit(task);
    }

    public static void execute(Runnable task) {
        executor.execute(task);
    }
}
