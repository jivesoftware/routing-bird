package com.jivesoftware.os.routing.bird.health.api;

import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponseImpl;

/**
 *
 */
public class NoOpHealthChecker<T> implements HealthChecker<T> {

    private final String name;

    public NoOpHealthChecker(String name) {
        this.name = name;
    }

    @Override
    public void check(T checkable, String description, String resolution) {
    }

    @Override
    public HealthCheckResponse checkHealth() throws Exception {
        return new HealthCheckResponseImpl(name, 1.0, "", "", "", System.currentTimeMillis());
    }
}
