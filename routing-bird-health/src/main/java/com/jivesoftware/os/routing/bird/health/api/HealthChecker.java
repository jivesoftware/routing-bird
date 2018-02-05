package com.jivesoftware.os.routing.bird.health.api;

import com.jivesoftware.os.routing.bird.health.HealthCheck;

/**
 * @param <C>
 * @author jonathan.colt
 */
public interface HealthChecker<C> extends HealthCheck {

    void check(C checkable, String description, String resolution);

}
