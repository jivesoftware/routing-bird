package com.jivesoftware.os.routing.bird.deployable;

import com.jivesoftware.os.routing.bird.health.api.PercentileHealthCheckConfig;
import org.merlin.config.defaults.StringDefault;

/**
 *
 * @author jonathan.colt
 */
public interface DeployableMainAuthHealthCheckConfig extends PercentileHealthCheckConfig {

    @StringDefault("authorization>main>health")
    @Override
    String getName();

    @StringDefault("Success rate of auths agains manage port")
    @Override
    String getDescription();
}
