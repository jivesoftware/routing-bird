package com.jivesoftware.os.routing.bird.health.checkers;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.health.api.HealthCheckConfig;
import com.jivesoftware.os.routing.bird.health.api.ScheduledHealthCheck;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;
import org.merlin.config.defaults.LongDefault;
import org.merlin.config.defaults.StringDefault;

/**
 *
 * @author jonathan.colt
 */
public class GCPauseHealthChecker implements ScheduledHealthCheck {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    static public interface GCPauseHealthCheckerConfig extends HealthCheckConfig {

        @StringDefault("jvm>gc>pause")
        @Override
        String getName();

        @LongDefault(500)
        Long getCheckIntervalInMillis();

        @StringDefault("Histogram of the GC pauses as observed by the process itself.")
        @Override
        String getDescription();
    }

    private final GCPauseHealthCheckerConfig config;
    private long lastCheckTime;
    private final double[] secondsHisto = new double[60];

    public GCPauseHealthChecker(GCPauseHealthCheckerConfig config) {
        this.config = config;
    }

    @Override
    public long getCheckIntervalInMillis() {
        return config.getCheckIntervalInMillis();
    }

    @Override
    public void run() {
        try {
            if (lastCheckTime == 0) {
                lastCheckTime = System.currentTimeMillis();
            } else {
                long now = System.currentTimeMillis();
                long elapseInSeconds = TimeUnit.MILLISECONDS.toSeconds(now - lastCheckTime);
                if (elapseInSeconds > 0) {
                    secondsHisto[(int) (elapseInSeconds >= secondsHisto.length ? 0 : elapseInSeconds)]++;
                }
                for (int i = 0; i < secondsHisto.length; i++) {
                    if (secondsHisto[i] > 0) {
                        secondsHisto[i] = secondsHisto[i] * 0.9d;
                    }
                }
                lastCheckTime = now;
            }

        } catch (Exception x) {
            LOG.error("Failed:", x);
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
                return 1d;
            }

            @Override
            public String getStatus() {
                NumberFormat formatter = new DecimalFormat("#0.000");
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < secondsHisto.length; i++) {
                    double d = secondsHisto[i];
                    if (d > 0) {
                        sb.append(i + "s=" + formatter.format(d) + ", ");
                    }
                }
                sb.append("60+sec=" + secondsHisto[0]);
                return sb.toString();
            }

            @Override
            public String getDescription() {
                return config.getDescription();
            }

            @Override
            public String getResolution() {
                return "Add more heap or tune the garbage collector";
            }

            @Override
            public long getTimestamp() {
                return System.currentTimeMillis();
            }
        };
    }

}
