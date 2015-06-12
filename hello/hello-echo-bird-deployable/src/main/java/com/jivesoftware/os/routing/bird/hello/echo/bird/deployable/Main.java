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
package com.jivesoftware.os.routing.bird.hello.echo.bird.deployable;

import com.jivesoftware.os.routing.bird.deployable.Deployable;
import com.jivesoftware.os.routing.bird.hello.routing.bird.service.HelloRoutingBirdService;
import com.jivesoftware.os.routing.bird.hello.routing.bird.service.HelloRoutingBirdServiceInitializer;
import com.jivesoftware.os.routing.bird.hello.routing.bird.service.HelloRoutingBirdServiceInitializer.HelloRoutingBirdServiceConfig;
import com.jivesoftware.os.routing.bird.hello.routing.bird.service.endpoints.HelloRoutingBirdServiceRestEndpoints;
import com.jivesoftware.os.routing.bird.http.client.TenantAwareHttpClient;
import com.jivesoftware.os.routing.bird.http.client.TenantRoutingHttpClientInitializer;
import com.jivesoftware.os.routing.bird.server.util.Resource;
import com.jivesoftware.os.routing.bird.shared.TenantsServiceConnectionDescriptorProvider;
import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

    public void run(String[] args) throws Exception {

        Deployable deployable = new Deployable(args);
        deployable.buildManageServer().start();

        TenantsServiceConnectionDescriptorProvider connections = deployable.getTenantRoutingProvider().getConnections("hello-echo-bird-deployable", "main");
        TenantRoutingHttpClientInitializer<String> tenantRoutingHttpClientInitializer = new TenantRoutingHttpClientInitializer<>();
        TenantAwareHttpClient<String> client = tenantRoutingHttpClientInitializer.initialize(connections);
        HelloRoutingBirdService helloRoutingBirdService = new HelloRoutingBirdServiceInitializer()
                .initialize(deployable.config(HelloRoutingBirdServiceConfig.class), client);

        deployable.addEndpoints(HelloRoutingBirdServiceRestEndpoints.class);
        deployable.addInjectables(HelloRoutingBirdService.class, helloRoutingBirdService);

        Resource resource = new Resource(new File(System.getProperty("user.dir"))).setDirectoryListingAllowed(true);
        deployable.addResource(resource);
        deployable.buildServer().start();

    }
}
