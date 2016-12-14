package com.jivesoftware.os.routing.bird.health.api;

import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class TriggerTimeoutHealthCheck implements ScheduledHealthCheck, ResettableHealthCheck {

    private final AtomicLong lastTriggerCount = new AtomicLong();
    private final AtomicLong lastCheckTimestamp = new AtomicLong();
    private final AtomicLong lastTriggerDelta = new AtomicLong();

    private final TriggerProvider triggerCount;
    private final String name;
    private final String description;
    private final long unhealthyForNMillisOnTrigger;
    private final double healthWhenTriggered;

    private long healthClearAfterThisTimestmap;

    public TriggerTimeoutHealthCheck(TriggerProvider triggerCount, TriggerTimeoutHealthCheckConfig config) {
        this.triggerCount = triggerCount;
        this.name = config.getName();
        this.description = config.getDescription();
        this.unhealthyForNMillisOnTrigger = config.getUnhealthyForNMillisOnTrigger();
        this.healthWhenTriggered = config.getHealthWhenTriggered();
    }

    @Override
    public void run() {
        long count = triggerCount.get();
        long delta = count - lastTriggerCount.getAndSet(count);
        lastTriggerDelta.set(delta);
        if (delta > 0) {
            healthClearAfterThisTimestmap = System.currentTimeMillis() + unhealthyForNMillisOnTrigger;
        } else if (delta < 0) { // means count was manually reset
            healthClearAfterThisTimestmap = 0;
        }
        lastCheckTimestamp.set(System.currentTimeMillis());
    }

    @Override
    public void reset() {
        healthClearAfterThisTimestmap = 0;
    }

    @Override
    public long getCheckIntervalInMillis() {
        return TimeUnit.SECONDS.toMillis(2); //TODO config
    }

    @Override
    public HealthCheckResponse checkHealth() throws Exception {
        return new HealthCheckResponse() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public double getHealth() {
                long elapse = healthClearAfterThisTimestmap - System.currentTimeMillis();
                if (elapse <= 0) {
                    return 1d;
                }
                return ((1d - ((double) elapse / (double) unhealthyForNMillisOnTrigger)) * (1d - healthWhenTriggered)) + healthWhenTriggered;
            }

            @Override
            public String getStatus() {
                return "Count: " + triggerCount.get() + " (" + lastTriggerDelta.get() + ")";
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public String getResolution() {
                long clearInMillis = healthClearAfterThisTimestmap - System.currentTimeMillis();
                return "Investigate recent triggers. Let triggers expire or manually reset. Clear in " + Math.max(clearInMillis, 0) + " ms";
            }

            @Override
            public long getTimestamp() {
                return lastCheckTimestamp.get();
            }
        };
    }

    public interface TriggerProvider {
        long get();
    }
}
