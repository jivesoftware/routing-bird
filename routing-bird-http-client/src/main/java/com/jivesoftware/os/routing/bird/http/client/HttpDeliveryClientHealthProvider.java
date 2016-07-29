package com.jivesoftware.os.routing.bird.http.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.ConnectionHealth;
import com.jivesoftware.os.routing.bird.shared.ConnectionHealthLatencyStats;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.routing.bird.shared.InstanceConnectionHealth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author jonathan.colt
 */
public class HttpDeliveryClientHealthProvider implements ClientHealthProvider, Runnable {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String instanceId;
    private final HttpRequestHelper httpRequestHelper;
    private final String path;
    private final long interval;
    private final int sampleWindow;
    private final Map<HostPort, Health> healths = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public HttpDeliveryClientHealthProvider(String instanceId, HttpRequestHelper httpRequestHelper, String path, long interval, int sampleWindow) {
        this.instanceId = instanceId;
        this.httpRequestHelper = httpRequestHelper;
        this.path = path;
        this.interval = interval;
        this.sampleWindow = sampleWindow;
    }

    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(this, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void stop() throws InterruptedException {
        if (httpRequestHelper != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Override
    public ClientHealth get(ConnectionDescriptor connectionDescriptor) {
        return healths.computeIfAbsent(connectionDescriptor.getHostPort(), (key) -> {
            return new Health(connectionDescriptor, sampleWindow);
        });
    }

    @Override
    public void run() {
        try {
            List<ConnectionHealth> deliverableHealth = new ArrayList<>();
            long time = System.currentTimeMillis();
            for (Health h : healths.values()) {
                for (Map.Entry<String, FamilyStats> familyStats : h.familyStats.entrySet()) {
                    FamilyStats fs = familyStats.getValue();

                    ConnectionHealthLatencyStats latencyStats = new ConnectionHealthLatencyStats(fs.ds.getMean(),
                        fs.ds.getMin(),
                        fs.ds.getMax(),
                        fs.ds.getPercentile(50d),
                        fs.ds.getPercentile(75d),
                        fs.ds.getPercentile(90d),
                        fs.ds.getPercentile(95d),
                        fs.ds.getPercentile(99d),
                        fs.ds.getPercentile(99.9d));

                    deliverableHealth.add(new ConnectionHealth(h.connectionDescriptor,
                        h.timestamp,
                        h.connectivityErrors,
                        h.firstMarkedAsDeadTimestamp,
                        h.lastMarkedAsDeadTimestamp,
                        h.fatalError.get() == null ? "" : ExceptionUtils.getStackTrace(h.fatalError.get()),
                        familyStats.getKey(),
                        fs.attempts.get(),
                        fs.success.get(),
                        fs.failure.get(),
                        fs.successPerSecond(time),
                        fs.failurePerSecond(time),
                        latencyStats));
                }
            }

            httpRequestHelper.executeRequest(new InstanceConnectionHealth(instanceId, deliverableHealth), path, String.class, null);
        } catch (Exception x) {
            LOG.warn("Failed to deliver client health.", x);
        }
    }

    static class Health implements ClientHealth {

        ConnectionDescriptor connectionDescriptor;
        long timestamp;
        long connectivityErrors = 0;
        long firstMarkedAsDeadTimestamp = -1;
        long lastMarkedAsDeadTimestamp = -1;
        AtomicReference<Exception> fatalError = new AtomicReference<>();
        Map<String, FamilyStats> familyStats = new ConcurrentHashMap<>();
        int sampleWindow;

        public Health(ConnectionDescriptor connectionDescriptor, int sampleWindow) {
            this.connectionDescriptor = connectionDescriptor;
            this.sampleWindow = sampleWindow;
        }

        @Override
        public void attempt(String family) {
            long currentTimeMillis = System.currentTimeMillis();
            FamilyStats stats = familyStats.computeIfAbsent(family, t -> new FamilyStats(sampleWindow));
            stats.attempt(currentTimeMillis);
        }

        @Override
        public void success(String family, long latency) {
            long currentTimeMillis = System.currentTimeMillis();
            firstMarkedAsDeadTimestamp = -1;
            lastMarkedAsDeadTimestamp = -1;
            connectivityErrors = 0;
            fatalError.set(null);
            FamilyStats stats = familyStats.computeIfAbsent(family, t -> new FamilyStats(sampleWindow));
            stats.success(currentTimeMillis, latency);
            timestamp = currentTimeMillis;
        }

        @Override
        public void markedDead() {
            long currentTimeMillis = System.currentTimeMillis();
            firstMarkedAsDeadTimestamp = currentTimeMillis;
            timestamp = currentTimeMillis;
        }

        @Override
        public void connectivityError(String family) {
            long currentTimeMillis = System.currentTimeMillis();
            connectivityErrors++;
            FamilyStats stats = familyStats.computeIfAbsent(family, t -> new FamilyStats(sampleWindow));
            stats.failure(currentTimeMillis);
            timestamp = currentTimeMillis;
        }

        @Override
        public void fatalError(String family, Exception x) {
            long currentTimeMillis = System.currentTimeMillis();
            fatalError.set(x);
            FamilyStats stats = familyStats.computeIfAbsent(family, t -> new FamilyStats(sampleWindow));
            stats.failure(currentTimeMillis);
            timestamp = currentTimeMillis;
        }

        @Override
        public void stillDead() {
            long currentTimeMillis = System.currentTimeMillis();
            lastMarkedAsDeadTimestamp = currentTimeMillis;
            timestamp = currentTimeMillis;
        }

    }

    static class FamilyStats {

        final AtomicLong attempts = new AtomicLong(0);
        final AtomicLong success = new AtomicLong(0);
        final AtomicLong failure = new AtomicLong(0);
        final DescriptiveStatistics ds;
        long successes;
        long failures;
        long lastSuccessSecond;
        long lastFailureSecond;
        long successPerSecond;
        long failurePerSecond;

        FamilyStats(int window) {
            ds = new DescriptiveStatistics(window);
        }

        public void attempt(long timestamp) {
            attempts.incrementAndGet();
        }

        public void success(long timestamp, long latency) {
            long second = timestamp / 1000;
            if (lastSuccessSecond < second) {
                lastSuccessSecond = second;
                successPerSecond = successes;
                successes = 0;
            }
            successes++;
            success.incrementAndGet();
            ds.addValue(latency);
        }

        public void failure(long timestamp) {
            long second = timestamp / 1000;
            if (lastFailureSecond < second) {
                lastFailureSecond = second;
                failurePerSecond = failures;
                failures = 0;
            }
            failures++;
            failure.incrementAndGet();
        }

        public long successPerSecond(long time) {
            long second = time / 1000;
            if (lastSuccessSecond < second) {
                if (second - lastSuccessSecond > 1) {
                    successPerSecond = 0;
                } else {
                    successPerSecond = successes;
                }
                successes = 0;
                lastSuccessSecond = successes;
            }
            return successPerSecond;
        }

        public long failurePerSecond(long time) {
            long second = time / 1000;
            if (lastFailureSecond < second) {
                if (second - lastFailureSecond > 1) {
                    failurePerSecond = 0;
                } else {
                    failurePerSecond = failures;
                }
                failures = 0;
                lastFailureSecond = failures;
            }
            return failurePerSecond;
        }
    }

    static HttpRequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider().createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        HttpRequestHelper requestHelper = new HttpRequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

}
