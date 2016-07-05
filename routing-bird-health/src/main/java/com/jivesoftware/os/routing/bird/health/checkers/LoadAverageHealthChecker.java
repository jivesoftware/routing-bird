package com.jivesoftware.os.routing.bird.health.checkers;

import com.jivesoftware.os.mlogger.core.Counter;
import com.jivesoftware.os.mlogger.core.ValueType;
import com.jivesoftware.os.routing.bird.health.api.MinMaxHealthCheckConfig;
import com.jivesoftware.os.routing.bird.health.api.MinMaxHealthChecker;
import com.jivesoftware.os.routing.bird.health.api.ScheduledHealthCheck;
import com.sun.management.UnixOperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import org.merlin.config.defaults.LongDefault;
import org.merlin.config.defaults.StringDefault;

/**
 * @author jonathan.colt
 */
public class LoadAverageHealthChecker extends MinMaxHealthChecker implements ScheduledHealthCheck {

    static public interface LoadAverageHealthCheckerConfig extends MinMaxHealthCheckConfig {

        @StringDefault("jvm>cpu>load")
        @Override
        String getName();

        @LongDefault(5_000)
        Long getCheckIntervalInMillis();

        @LongDefault(48)
        @Override
        Long getMax();

        @StringDefault("Last minute load average.")
        @Override
        String getDescription();
    }

    private final LoadAverageHealthCheckerConfig config;
    private final OperatingSystemMXBean os;

    public LoadAverageHealthChecker(LoadAverageHealthCheckerConfig config) {
        super(config);
        this.config = config;
        this.os = ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public long getCheckIntervalInMillis() {
        return config.getCheckIntervalInMillis();
    }

    @Override
    public void run() {
        if (os != null) {
            try {
                double avg = os.getSystemLoadAverage();
                Counter counter = new Counter(ValueType.COUNT);
                counter.set((long) avg);
                check(counter, config.getDescription(), "Check logs and thread dumps.");
            } catch (Exception x) {
                // TODO what?
            }
        }
    }

}
