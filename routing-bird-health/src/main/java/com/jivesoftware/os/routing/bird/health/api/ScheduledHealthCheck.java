package com.jivesoftware.os.routing.bird.health.api;

import com.jivesoftware.os.routing.bird.health.HealthCheck;

/**
 * @author jonathan.colt
 */
public interface ScheduledHealthCheck extends Runnable, HealthCheck {

    long getCheckIntervalInMillis();

}
