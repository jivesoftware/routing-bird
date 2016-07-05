package com.jivesoftware.os.routing.bird.health.checkers;

import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.health.api.HealthCheckConfig;
import com.jivesoftware.os.routing.bird.health.api.ScheduledHealthCheck;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.merlin.config.defaults.DoubleDefault;
import org.merlin.config.defaults.IntDefault;
import org.merlin.config.defaults.LongDefault;
import org.merlin.config.defaults.StringDefault;

/**
 * @author jonathan.colt
 */
public class SystemCpuHealthChecker implements ScheduledHealthCheck {

    static public interface SystemCpuHealthCheckerConfig extends HealthCheckConfig {

        @StringDefault("jvm>cpu>usage")
        @Override
        String getName();

        @LongDefault(5_000)
        Long getCheckIntervalInMillis();

        @LongDefault(5 * 60_000L)
        Long getDurationBeforeAlertInMillis();

        @IntDefault(90)
        Integer getCpuTrigger();

        @IntDefault(70)
        Integer getCpuRecover();

        @DoubleDefault(0.0)
        Double getAlertHealth();

        @StringDefault("Percent of CPU being used by the java process.")
        @Override
        String getDescription();
    }

    private final SystemCpuHealthCheckerConfig config;
    private final MBeanServer mbs;
    private final ObjectName name;

    private int lastCpu = 0;
    private long cpuTriggerTime = 0L;
    private long cpuRecoverTime = 0L;

    public SystemCpuHealthChecker(SystemCpuHealthCheckerConfig config) {
        this.config = config;
        this.mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            this.name = ObjectName.getInstance("java.lang:type=OperatingSystem");
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException("Failed to get instance of OperatingSystem");
        }
    }

    @Override
    public long getCheckIntervalInMillis() {
        return config.getCheckIntervalInMillis();
    }

    @Override
    public void run() {
        try {
            AttributeList list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });

            if (list.isEmpty()) {
                lastCpu = 0;
                cpuTriggerTime = 0L;
                cpuRecoverTime = 0L;
                return;
            }

            Attribute att = (Attribute) list.get(0);
            Double value = (Double) att.getValue();

            if (value == -1.0) {
                lastCpu = 0;
                cpuTriggerTime = 0L;
                cpuRecoverTime = 0L;
                return;
            }

            lastCpu = (int) (value * 100);
            if (lastCpu > config.getCpuTrigger()) {
                if (cpuTriggerTime == 0L) {
                    cpuTriggerTime = System.currentTimeMillis();
                }
                cpuRecoverTime = 0L;
            } else if (lastCpu < config.getCpuRecover()) {
                if (cpuRecoverTime == 0L) {
                    cpuRecoverTime = System.currentTimeMillis();
                }
                cpuTriggerTime = 0L;
            }
        } catch (Exception x) {
            // TODO what?
        }
    }

    @Override
    public HealthCheckResponse checkHealth() throws Exception {
        return new HealthCheckResponse() {
            @Override
            public String getName() {
                return config.getName();
            }

            @Override
            public double getHealth() {
                if (cpuTriggerTime > 0) {
                    double scale = 1.0 - config.getAlertHealth();
                    long elapsed = System.currentTimeMillis() - cpuTriggerTime;
                    double pctToAlert = Math.min(1.0, (double) elapsed / config.getDurationBeforeAlertInMillis());
                    return 1.0 - scale * pctToAlert;
                } else {
                    return 1.0;
                }
            }

            @Override
            public String getStatus() {
                return "CPU " + lastCpu + "%" +
                    (cpuTriggerTime > 0 ? " (triggered " + timeAgo(System.currentTimeMillis() - cpuTriggerTime) + ")" :
                        cpuRecoverTime > 0 ? " (recovered " + timeAgo(System.currentTimeMillis() - cpuRecoverTime) + ")" :
                            "");
            }

            private String timeAgo(long millis) {
                String suffix;
                if (millis >= 0) {
                    suffix = "ago";
                } else {
                    suffix = "from now";
                    millis = Math.abs(millis);
                }

                final long hr = TimeUnit.MILLISECONDS.toHours(millis);
                final long min = TimeUnit.MILLISECONDS.toMinutes(millis - TimeUnit.HOURS.toMillis(hr));
                final long sec = TimeUnit.MILLISECONDS.toSeconds(millis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
                final long ms = TimeUnit.MILLISECONDS.toMillis(
                    millis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
                return String.format("%02d:%02d:%02d.%03d " + suffix, hr, min, sec, ms);
            }

            @Override
            public String getDescription() {
                return config.getDescription();
            }

            @Override
            public String getResolution() {
                return "Check logs and thread dumps";
            }

            @Override
            public long getTimestamp() {
                return System.currentTimeMillis();
            }
        };
    }
}
