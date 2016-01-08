package com.jivesoftware.os.routing.bird.health.api;

import org.merlin.config.defaults.DoubleDefault;
import org.merlin.config.defaults.StringDefault;

/**
 * @author jonathan.colt
 */
public interface SickHealthCheckConfig extends HealthCheckConfig {

    @Override
    @StringDefault("sick")
    String getName();

    @Override
    @StringDefault("Nothing is sick")
    String getDescription();

    @DoubleDefault(0.0)
    Double getSickHealth();

}
