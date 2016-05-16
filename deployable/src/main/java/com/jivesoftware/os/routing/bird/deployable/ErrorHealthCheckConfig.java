package com.jivesoftware.os.routing.bird.deployable;

import com.jivesoftware.os.routing.bird.health.api.HealthCheckConfig;
import org.merlin.config.defaults.DoubleDefault;
import org.merlin.config.defaults.IntDefault;

/**
 *
 * @author jonathan.colt
 */
public interface ErrorHealthCheckConfig extends HealthCheckConfig {

    @IntDefault(60_000)
    int getInternalUnhealthyForNMillisEveryError();

    @DoubleDefault(0.20)
    double getInternalHealthWhenErrorsExceeded();

    @IntDefault(60_000)
    int getExternalUnhealthyForNMillisEveryError();

    @DoubleDefault(0.20)
    double getExternalHealthWhenErrorsExceeded();

}
