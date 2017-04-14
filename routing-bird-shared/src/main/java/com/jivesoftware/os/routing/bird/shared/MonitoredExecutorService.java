package com.jivesoftware.os.routing.bird.shared;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;

/**
 * Created by jonathan.colt on 4/13/17.
 */
public class MonitoredExecutorService extends ThreadPoolExecutor {

    public final LongAdder processed = new LongAdder();
    public final LongAdder submitted = new LongAdder();
    public final DescriptiveStatistics queueLag;


    public MonitoredExecutorService(int windowSize,
        int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        BlockingQueue<Runnable> workQueue,
        ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        queueLag = new SynchronizedDescriptiveStatistics(windowSize);
    }

    public void reset() {
        processed.reset();
        submitted.reset();
        queueLag.clear();
    }


    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submitted.increment();
        long startTime = System.currentTimeMillis();
        return super.submit(() -> {
                queueLag.addValue(System.currentTimeMillis() - startTime);
                try {
                    return task.call();
                } finally {
                    processed.increment();
                }
            }
        );
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return submit(() -> {
            task.run();
            return result;
        });
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                task.run();
                return null;
            }
        });
    }
}
