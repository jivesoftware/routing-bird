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
public class FileDescriptorCountHealthChecker extends MinMaxHealthChecker implements ScheduledHealthCheck {

    static public interface FileDescriptorCountHealthCheckerConfig extends MinMaxHealthCheckConfig {

        @StringDefault("jvm>fd>count")
        @Override
        String getName();

        @LongDefault(60_000)
        Long getCheckIntervalInMillis();

        @LongDefault(500_000)
        @Override
        Long getMax();

        @StringDefault("Number of file descriptors opened by the java process.")
        @Override
        String getDescription();
    }

    private final FileDescriptorCountHealthCheckerConfig config;
    private final UnixOperatingSystemMXBean os;

    public FileDescriptorCountHealthChecker(FileDescriptorCountHealthCheckerConfig config) {
        super(config);
        this.config = config;

        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.os = operatingSystemMXBean instanceof UnixOperatingSystemMXBean ? (UnixOperatingSystemMXBean) operatingSystemMXBean : null;
    }

    @Override
    public long getCheckIntervalInMillis() {
        return config.getCheckIntervalInMillis();
    }

    @Override
    public void run() {
        if (os != null) {
            try {
                long count = os.getOpenFileDescriptorCount();
                Counter counter = new Counter(ValueType.COUNT);
                counter.set(count);
                check(counter, config.getDescription(), "Check for file handle leaks. OS max: " + os.getMaxFileDescriptorCount());
            } catch (Exception x) {
                // TODO what?
            }
        }
    }

}
