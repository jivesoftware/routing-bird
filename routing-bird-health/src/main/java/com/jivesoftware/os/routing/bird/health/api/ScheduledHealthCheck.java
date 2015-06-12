package com.jivesoftware.os.routing.bird.health.api;

import com.jivesoftware.os.routing.bird.health.HealthCheck;

/**
 * @author jonathan.colt
 */
public interface ScheduledHealthCheck extends Runnable, HealthCheck {

    /**
     The number of millis to wait between each check.
     @return
     */
    long getCheckIntervalInMillis();
}
