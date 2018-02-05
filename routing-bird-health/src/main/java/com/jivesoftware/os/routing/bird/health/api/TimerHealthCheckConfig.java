package com.jivesoftware.os.routing.bird.health.api;

import org.merlin.config.defaults.DoubleDefault;
import org.merlin.config.defaults.IntDefault;

/**
 * @author jonathan.colt
 */
public interface TimerHealthCheckConfig extends HealthCheckConfig {

    @DoubleDefault(Double.MAX_VALUE)
    Double getMeanMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double getVarianceMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get50ThPercentileMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get75ThPercentileMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get90ThPercentileMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get95ThPercentileMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get99ThPercentileMax();

}
