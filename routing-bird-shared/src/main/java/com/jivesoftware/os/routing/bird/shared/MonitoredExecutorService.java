package com.jivesoftware.os.routing.bird.shared;

import java.util.concurrent.BlockingQueue;
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


    public void execute(Runnable runnable) {
        submitted.increment();
        long startTime = System.currentTimeMillis();
        super.execute(() -> {
                queueLag.addValue(System.currentTimeMillis() - startTime);
                try {
                    runnable.run();
                } finally {
                    processed.increment();
                }
            }
        );
    }


}
