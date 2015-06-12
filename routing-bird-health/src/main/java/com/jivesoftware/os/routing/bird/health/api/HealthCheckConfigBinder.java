package com.jivesoftware.os.routing.bird.health.api;

import org.merlin.config.Config;

/**
 *
 * @author jonathan.colt
 */
public interface HealthCheckConfigBinder {

    <C extends Config> C bindConfig(Class<C> configurationInterfaceClass);
}
