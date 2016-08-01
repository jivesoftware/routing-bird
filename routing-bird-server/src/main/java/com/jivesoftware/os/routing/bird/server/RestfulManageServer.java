/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.routing.bird.server;

import com.jivesoftware.os.routing.bird.endpoints.base.RestfulBaseEndpoints;
import com.jivesoftware.os.routing.bird.endpoints.base.RestfulBaseEndpoints.ResfulServiceName;
import com.jivesoftware.os.routing.bird.endpoints.logging.level.LogLevelRestEndpoints;
import com.jivesoftware.os.routing.bird.endpoints.logging.metric.LogMetricRestfulEndpoints;
import com.jivesoftware.os.routing.bird.health.HealthCheck;
import com.jivesoftware.os.routing.bird.health.HealthCheckService;
import java.io.File;
import java.util.Arrays;

public class RestfulManageServer {

    private final RestfulServer server;
    private final HealthCheckService healthCheckService = new HealthCheckService();
    private final JerseyEndpoints jerseyEndpoints;

    public RestfulManageServer(int port,
        String applicationName,
        int maxNumberOfThreads,
        int maxQueuedRequests) {
        server = new RestfulServer(port, applicationName, maxNumberOfThreads, maxQueuedRequests);

        jerseyEndpoints = new JerseyEndpoints()
            .enableCORS()
            .humanReadableJson()
            .addEndpoint(RestfulBaseEndpoints.class).addInjectable(healthCheckService).addInjectable(new File("logs/service.log"))
            .addEndpoint(LogMetricRestfulEndpoints.class)
            .addEndpoint(LogLevelRestEndpoints.class)
            .addInjectable(ResfulServiceName.class, new ResfulServiceName(applicationName, port));

    }

    public int getThreads() {
        return server.getThreads();
    }

    public int getIdleThreads() {
        return server.getIdleThreads();
    }

    public int getBusyThreads() {
        return server.getBusyThreads();
    }

    public int getMaxThreads() {
        return server.getMaxThreads();
    }

    public boolean isLowOnThreads() {
        return server.isLowOnThreads();
    }

    public void addEndpoint(Class clazz) {
        jerseyEndpoints.addEndpoint(clazz);
    }

    public void addInjectable(Class clazz, Object injectable) {
        jerseyEndpoints.addInjectable(clazz, injectable);
    }

    public RestfulManageServer initialize() {
        server.addContextHandler("/manage", jerseyEndpoints);
        return this;
    }

    public RestfulManageServer addHealthCheck(HealthCheck... check) {
        healthCheckService.addHealthCheck(Arrays.asList(check));
        return this;
    }

    public RestfulManageServer removeHealthCheck(HealthCheck... check) {
        healthCheckService.removeHealthCheck(Arrays.asList(check));
        return this;
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
}
