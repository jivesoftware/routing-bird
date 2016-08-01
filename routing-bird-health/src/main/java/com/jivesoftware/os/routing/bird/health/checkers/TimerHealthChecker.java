package com.jivesoftware.os.routing.bird.health.checkers;

import com.jivesoftware.os.mlogger.core.Timer;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponseImpl;
import com.jivesoftware.os.routing.bird.health.api.HealthCheckUtil;
import com.jivesoftware.os.routing.bird.health.api.HealthChecker;
import com.jivesoftware.os.routing.bird.health.api.HealthFactory;
import com.jivesoftware.os.routing.bird.health.api.ResettableHealthCheck;
import com.jivesoftware.os.routing.bird.health.api.TimerHealthCheckConfig;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author jonathan.colt
 */
public class TimerHealthChecker implements HealthChecker<Timer>, ResettableHealthCheck {

    public static final HealthFactory.HealthCheckerConstructor<Timer, TimerHealthCheckConfig> FACTORY = TimerHealthChecker::new;

    private final TimerHealthCheckConfig config;
    private final AtomicReference<TimerCallable> lastTimer = new AtomicReference<>();

    public TimerHealthChecker(TimerHealthCheckConfig config) {
        this.config = config;
    }

    @Override
    public void check(final Timer timer, final String description, final String resolution) {
        lastTimer.set(new TimerCallable(timer, description, resolution, System.currentTimeMillis()));
    }

    @Override
    public HealthCheckResponse checkHealth() throws Exception {
        Callable<HealthCheckResponse> callable = lastTimer.get();
        if (callable != null) {
            return callable.call();
        } else {
            return new HealthCheckResponseImpl(config.getName(),
                HealthCheckResponse.UNKNOWN,
                "Health is currently unknown for this check.",
                "Description is currently unknown for this check.",
                "Resolution is currently unknown for this check.",
                System.currentTimeMillis());
        }
    }

    @Override
    public void reset() {
        TimerCallable timerCallable = lastTimer.get();
        if (timerCallable != null) {
            timerCallable.timer.reset();
        }
    }

    private String healthString(Timer timer) {
        StringBuilder sb = new StringBuilder();
        sb.append(" samples:").append(timer.getSampleCount());
        sb.append(" mean:").append(timer.getMean());
        sb.append(" 50th:").append(timer.get50ThPercentile());
        sb.append(" 75th:").append(timer.get75ThPercentile());
        sb.append(" 90th:").append(timer.get90ThPercentile());
        sb.append(" 95th:").append(timer.get95ThPercentile());
        sb.append(" 99th:").append(timer.get99ThPercentile());
        sb.append(" max:").append(timer.getMax());
        return sb.toString();
    }

    private class TimerCallable implements Callable<HealthCheckResponse> {

        private final Timer timer;
        private final String description;
        private final String resolution;
        private final long time;

        private TimerCallable(Timer timer, String description, String resolution, long time) {
            this.timer = timer;
            this.description = description;
            this.resolution = resolution;
            this.time = time;
        }

        @Override
        public HealthCheckResponse call() throws Exception {
            double health = 1.0f;
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.getMeanMax(), 0, timer.getMean()));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.getVarianceMax(), 0, Math.abs(timer.getVariance())));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get50ThPecentileMax(), 0, timer.get50ThPercentile()));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get75ThPecentileMax(), 0, timer.get75ThPercentile()));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get90ThPecentileMax(), 0, timer.get90ThPercentile()));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get95ThPecentileMax(), 0, timer.get95ThPercentile()));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get99ThPecentileMax(), 0, timer.get99ThPercentile()));
            return new HealthCheckResponseImpl(config.getName(), Math.max(health, 0f), healthString(timer), description, resolution, time);
        }
    }

}
