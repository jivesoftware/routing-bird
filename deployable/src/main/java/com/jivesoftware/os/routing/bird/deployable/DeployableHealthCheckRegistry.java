package com.jivesoftware.os.routing.bird.deployable;

import com.jivesoftware.os.routing.bird.health.api.HealthCheckRegistry;
import com.jivesoftware.os.routing.bird.health.api.HealthChecker;

public class DeployableHealthCheckRegistry implements HealthCheckRegistry {

    private final Deployable deployable;

    public DeployableHealthCheckRegistry(Deployable deployable) {
        this.deployable = deployable;
    }

    @Override
    public void register(HealthChecker<?> healthChecker) {
        deployable.addHealthCheck(healthChecker);
    }

    @Override
    public void unregister(HealthChecker<?> healthChecker) {
        deployable.removeHealthCheck(healthChecker);
    }

}
