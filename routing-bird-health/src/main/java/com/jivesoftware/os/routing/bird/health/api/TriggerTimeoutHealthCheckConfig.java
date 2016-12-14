package com.jivesoftware.os.routing.bird.health.api;

import com.jivesoftware.os.routing.bird.health.api.HealthCheckConfig;
import org.merlin.config.defaults.DoubleDefault;
import org.merlin.config.defaults.LongDefault;

/**
 *
 */
public interface TriggerTimeoutHealthCheckConfig extends HealthCheckConfig {

    @LongDefault(30_000L)
    long getUnhealthyForNMillisOnTrigger();

    @DoubleDefault(0.2)
    double getHealthWhenTriggered();
}
