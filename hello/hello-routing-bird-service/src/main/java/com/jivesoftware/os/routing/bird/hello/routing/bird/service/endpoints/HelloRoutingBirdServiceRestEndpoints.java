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
package com.jivesoftware.os.routing.bird.hello.routing.bird.service.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.hello.routing.bird.service.HelloRoutingBirdService;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class HelloRoutingBirdServiceRestEndpoints {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();
    @Context
    private HelloRoutingBirdService service;

    @GET
    @Path("/hello")
    public Response hello() {
        log.debug("Hello: " + service.greetings());
        return Response.ok(service.greetings() + " " + System.identityHashCode(Runtime.getRuntime()) + "\n", MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/echo")
    public Response echo(@QueryParam("tenantId") @DefaultValue("defaultTenantId") String tenantId,
        @QueryParam("message") @DefaultValue("echo") String message,
        @QueryParam("echos") @DefaultValue("3") int echos) {
        try {
            String echo = service.echo(tenantId, message, echos);
            return Response.ok("Echo: " + echo + " " + System.identityHashCode(Runtime.getRuntime()), MediaType.TEXT_PLAIN).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed to echo: " + tenantId + " " + message + " " + echos, x);
        }
    }
}
