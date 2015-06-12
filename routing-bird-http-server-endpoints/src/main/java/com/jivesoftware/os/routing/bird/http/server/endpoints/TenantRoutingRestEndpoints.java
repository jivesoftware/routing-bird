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
package com.jivesoftware.os.routing.bird.http.server.endpoints;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.routing.bird.shared.TenantRoutingProvider;
import com.jivesoftware.os.routing.bird.shared.TenantsRoutingReport;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/tenant/routing")
public class TenantRoutingRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final TenantRoutingProvider routingProvider;

    public TenantRoutingRestEndpoints(@Context TenantRoutingProvider tenantRoutingConnectionPoolProvider) {
        this.routingProvider = tenantRoutingConnectionPoolProvider;
    }

    @GET
    @Path("/report")
    public Response report() {
        try {
            LOG.info("get report");
            TenantsRoutingReport routingReport = routingProvider.getRoutingReport();
            LOG.info("got report");
            return ResponseHelper.INSTANCE.jsonResponse(routingReport);
        } catch (Exception x) {
            LOG.error("Encountered the following error getting a routing report.", x);
            return ResponseHelper.INSTANCE.errorResponse("Encountered the following error getting a routing report.", x);
        }
    }

    @GET
    @Path("/invalidateAll")
    public Response invalidateAll() {
        try {
            LOG.info("invalidating all");
            routingProvider.invalidateAll();
            LOG.info("invalidated all");
            return ResponseHelper.INSTANCE.jsonResponse("InvalidatedAll");
        } catch (Exception x) {
            LOG.error("Encountered the following error invalidating all.", x);
            return ResponseHelper.INSTANCE.errorResponse("Encountered the following error invalidating all.", x);
        }
    }

    @GET
    @Path("/invalidate")
    public Response invalidate(
            @QueryParam("connectToServiceId") String connectToServiceId,
            @QueryParam("portName") String portName,
            @QueryParam("tenantId") String tenantId) {
        try {
            LOG.info("invalidating connectToServiceId:" + connectToServiceId + " portName:" + portName + " tenantId:" + tenantId);
            routingProvider.invalidateTenant(connectToServiceId, portName, tenantId);
            LOG.info("invalidated connectToServiceId:" + connectToServiceId + " portName:" + portName + " tenantId:" + tenantId);
            return ResponseHelper.INSTANCE.jsonResponse("Invalidated connectToServiceId:" + connectToServiceId + " for tenantId:" + tenantId);
        } catch (Exception x) {
            LOG.error("Encountered the following error invalidating connectToServiceId:" + connectToServiceId
                    + " portName:" + portName + " tenantId:" + tenantId, x);
            return ResponseHelper.INSTANCE.errorResponse("Encountered the following error invalidating connectToServiceId:" + connectToServiceId
                    + " portName:" + portName + " tenantId:" + tenantId, x);
        }
    }
}
