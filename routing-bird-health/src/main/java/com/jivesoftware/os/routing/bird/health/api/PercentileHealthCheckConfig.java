package com.jivesoftware.os.routing.bird.health.api;

import org.merlin.config.defaults.DoubleDefault;
import org.merlin.config.defaults.IntDefault;
import org.merlin.config.defaults.StringDefault;

/**
 *
 * @author jonathan.colt
 */
public interface PercentileHealthCheckConfig extends HealthCheckConfig {

    @StringDefault("")
    String getResolution();

    void setResolution(String resolution);


    @IntDefault(500)
    Integer getSampleWindowSize();

    @DoubleDefault(Double.MAX_VALUE)
    Double getMeanMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double getVarianceMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get50ThPecentileMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get75ThPecentileMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get90ThPecentileMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get95ThPecentileMax();

    @DoubleDefault(Double.MAX_VALUE)
    Double get99ThPecentileMax();

}
