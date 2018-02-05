package com.jivesoftware.os.routing.bird.health.api;

/**
 * @author jonathan.colt
 */
public interface HealthCheckRegistry {

    void register(HealthChecker<?> healthChecker);

    void unregister(HealthChecker<?> healthChecker);

}
