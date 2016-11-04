package com.jivesoftware.os.routing.bird.health.checkers;

import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponseImpl;
import com.jivesoftware.os.routing.bird.health.api.HealthCheckUtil;
import com.jivesoftware.os.routing.bird.health.api.HealthChecker;
import com.jivesoftware.os.routing.bird.health.api.PercentileHealthCheckConfig;
import com.jivesoftware.os.routing.bird.health.api.ResettableHealthCheck;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author jonathan.colt
 */
public class PercentileHealthChecker implements HealthChecker<Double>, ResettableHealthCheck {

    private final PercentileHealthCheckConfig config;
    private final DescriptiveStatistics dstat;
    private final LongAdder total;

    public PercentileHealthChecker(PercentileHealthCheckConfig config) {
        this.config = config;
        this.dstat = new DescriptiveStatistics(config.getSampleWindowSize());
        this.total = new LongAdder();
    }

    @Override
    public void check(Double sample, String description, String resolution) {
        if (sample != null && sample >= 0) {
            dstat.addValue(sample);
            total.increment();
        }
    }

    @Override
    public HealthCheckResponse checkHealth() throws Exception {

        double health = 1.0f;
        if (total.longValue()> 0) {
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.getMeanMax(), 0, dstat.getMean()));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.getVarianceMax(), 0, Math.abs(dstat.getVariance())));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get50ThPecentileMax(), 0, dstat.getPercentile(50)));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get75ThPecentileMax(), 0, dstat.getPercentile(75)));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get90ThPecentileMax(), 0, dstat.getPercentile(90)));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get95ThPecentileMax(), 0, dstat.getPercentile(95)));
            health = Math.min(health, HealthCheckUtil.zeroToOne(config.get99ThPecentileMax(), 0, dstat.getPercentile(99)));
        }
        return new HealthCheckResponseImpl(config.getName(),
            Math.max(health, 0f),
            status(dstat),
            config.getDescription(),
            config.getResolution(),
            System.currentTimeMillis());

    }

    @Override
    public void reset() {
        dstat.clear();
        total.reset();
    }

    private String status(DescriptiveStatistics dstats) {
        StringBuilder sb = new StringBuilder();
        sb.append(" samples:").append(total.longValue());
        if (total.longValue()> 0) {
            sb.append(" mean:").append(dstats.getMean());
            sb.append(" 50th:").append(dstats.getPercentile(50));
            sb.append(" 75th:").append(dstats.getPercentile(75));
            sb.append(" 90th:").append(dstats.getPercentile(90));
            sb.append(" 95th:").append(dstats.getPercentile(95));
            sb.append(" 99th:").append(dstats.getPercentile(99));
            sb.append(" max:").append(dstats.getMax());
        }
        return sb.toString();
    }

}
