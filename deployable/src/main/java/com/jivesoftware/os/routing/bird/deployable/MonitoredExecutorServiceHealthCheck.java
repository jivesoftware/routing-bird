package com.jivesoftware.os.routing.bird.deployable;

import com.jivesoftware.os.routing.bird.health.HealthCheck;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.shared.MonitoredExecutorService;

/**
 * Created by jonathan.colt on 4/13/17.
 */
public class MonitoredExecutorServiceHealthCheck implements HealthCheck {

    private final String poolName;
    private final MonitoredExecutorService executorService;

    public MonitoredExecutorServiceHealthCheck(String poolName, MonitoredExecutorService executorService) {
        this.poolName = poolName;
        this.executorService = executorService;
    }

    @Override
    public HealthCheckResponse checkHealth() throws Exception {

        StringBuilder messages = new StringBuilder();

            messages.append(poolName)
                .append(" samples:").append(executorService.processed.longValue())
                .append(" mean:").append(executorService.queueLag.getMean())
                .append(" 50:").append(executorService.queueLag.getPercentile(50d))
                .append(" 75:").append(executorService.queueLag.getPercentile(75d))
                .append(" 90:").append(executorService.queueLag.getPercentile(90d))
                .append(" 95:").append(executorService.queueLag.getPercentile(95d))
                .append(" 99:").append(executorService.queueLag.getPercentile(99d))
                .append(" max:").append(executorService.queueLag.getMax())
                .append("\n");

        return new HealthCheckResponse() {

            @Override
            public String getName() {
                return "threadPool>" + poolName;
            }

            @Override
            public double getHealth() {
                return 1d;
            }

            @Override
            public String getStatus() {
                return executorService.submitted.longValue()+" "+executorService.processed.longValue();
            }

            @Override
            public String getDescription() {
                return messages.toString();
            }

            @Override
            public String getResolution() {
                return "Look into thread pool configuration.";
            }

            @Override
            public long getTimestamp() {
                return System.currentTimeMillis();
            }
        };

    }
}