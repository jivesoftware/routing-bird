package com.jivesoftware.os.routing.bird.deployable;

import com.jivesoftware.os.routing.bird.health.api.PercentileHealthCheckConfig;
import org.merlin.config.defaults.StringDefault;

/**
 *
 * @author jonathan.colt
 */
public interface DeployableManageAuthHealthCheckConfig extends PercentileHealthCheckConfig {

    @StringDefault("authorization>manage>health")
    @Override
    String getName();

    @StringDefault("Success rate of auths agains manage port")
    @Override
    String getDescription();
}
