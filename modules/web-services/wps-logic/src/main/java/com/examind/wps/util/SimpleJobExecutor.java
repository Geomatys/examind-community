package com.examind.wps.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;
import org.springframework.stereotype.Component;

/**
 * TODO: Set number of concurrent execution configurable.
 *
 * @author Alexis Manin (Geomatys)
 */
@Component
public class SimpleJobExecutor implements AutoCloseable {

    /**
     * Executor thread pool.
     */
    private final ExecutorService executor;

    private SimpleJobExecutor() {
        // TODO: change executor type according to concurrent job limitation (i.e create a fixedthreadpool, etc.)
        executor = Executors.newCachedThreadPool(new SimpleThreadFactory());
    }

    public <T> Future<T> submit(final Callable<T> job) {
        return executor.submit(job);
    }

    public Future<?> submit(final Runnable job) {
        return executor.submit(job);
    }

    @PreDestroy
    @Override
    public void close() throws Exception {
        try {
            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } finally {
            // in case there's tasks which didn't finished in specified timeout.
            executor.shutdownNow();
        }
    }

    private static final String THREAD_NAME_PREFIX = "wps-simple-";

    /**
     * A simple thread factory whose thread names will start with {@link #THREAD_NAME_PREFIX}.
     *
     * @implNote Copied from {@link Executors#defaultThreadFactory() } code.
     */
    static class SimpleThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        SimpleThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                                  Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  THREAD_NAME_PREFIX + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon())
                t.setDaemon(false);

            return t;
        }
    }
}
