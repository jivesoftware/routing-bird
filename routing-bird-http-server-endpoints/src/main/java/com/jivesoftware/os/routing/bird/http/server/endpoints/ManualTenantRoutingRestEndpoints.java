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
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptor;
import com.jivesoftware.os.upena.routing.shared.HostPort;
import com.jivesoftware.os.upena.routing.shared.InMemoryConnectionsDescriptorsProvider;
import com.jivesoftware.os.upena.routing.shared.ResponseHelper;
import java.util.Collection;
import java.util.HashMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/manual/tenant/routing")
public class ManualTenantRoutingRestEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final InMemoryConnectionsDescriptorsProvider connectionsDescriptorsProvider;

    public ManualTenantRoutingRestEndpoints(@Context InMemoryConnectionsDescriptorsProvider connectionsDescriptorsProvider) {
        this.connectionsDescriptorsProvider = connectionsDescriptorsProvider;
    }

    @GET
    @Path("/clear")
    public Response clear(@QueryParam("tenantId") String tenantId,
            @QueryParam("instanceId") String instanceId,
            @QueryParam("connectToServiceNamed") String connectToServiceNamed,
            @QueryParam("portName") String portName) {
        try {
            LOG.info("clearing "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName);
            connectionsDescriptorsProvider.clear(tenantId, instanceId, connectToServiceNamed, portName);
            LOG.info("cleared "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName);
            return ResponseHelper.INSTANCE.jsonResponse("Cleared");
        } catch (Exception x) {
            LOG.info("Encountered the following error clearing "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName, x);
            return ResponseHelper.INSTANCE.errorResponse("Encountered the following error clearing "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName, x);
        }
    }

    @GET
    @Path("/set")
    public Response set(@QueryParam("tenantId") String tenantId,
            @QueryParam("instanceId") String instanceId,
            @QueryParam("connectToServiceNamed") String connectToServiceNamed,
            @QueryParam("portName") String portName,
            @QueryParam("host") String host,
            @QueryParam("port") int port) {

        try {
            LOG.info("setting "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName);
            ConnectionDescriptor connectionDescriptor = new ConnectionDescriptor(new HostPort(host, port), new HashMap<String, String>());
            connectionsDescriptorsProvider.set(tenantId, instanceId, connectToServiceNamed, portName, connectionDescriptor);
            LOG.info("set"
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName);
            return ResponseHelper.INSTANCE.jsonResponse("Set: tenantId=" + tenantId
                    + "&instanceId=" + instanceId
                    + "&connectToServiceNamed=" + connectToServiceNamed
                    + "&portName=" + portName + "->" + connectionDescriptor);
        } catch (Exception x) {
            LOG.info("Encountered the following error setting "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName, x);
            return ResponseHelper.INSTANCE.errorResponse("Encountered the following error setting "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName, x);
        }
    }

    @GET
    @Path("/get")
    public Response get(@QueryParam("tenantId") String tenantId,
            @QueryParam("instanceId") String instanceId,
            @QueryParam("connectToServiceNamed") String connectToServiceNamed,
            @QueryParam("portName") String portName) {
        try {

            LOG.info("getting "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName);
            ConnectionDescriptor connectionDescriptor = connectionsDescriptorsProvider.get(tenantId, instanceId, connectToServiceNamed, portName);
            LOG.info("got"
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName);
            return ResponseHelper.INSTANCE.jsonResponse(connectionDescriptor);
        } catch (Exception x) {
            LOG.info("Encountered the following error getting "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName, x);
            return ResponseHelper.INSTANCE.errorResponse("Encountered the following error getting "
                    + "tenantId:" + tenantId + " instanceId:" + instanceId + " connectToServiceNamed:" + connectToServiceNamed + " portName:" + portName, x);
        }

    }

    @GET
    @Path("/keys")
    public Response getKeys() {
        try {
            LOG.info("getting requestedRoutingKeys");
            Collection<String> requestedRoutingKeys = connectionsDescriptorsProvider.getRequestedRoutingKeys();
            LOG.info("got requestedRoutingKeys");
            return ResponseHelper.INSTANCE.jsonResponse(requestedRoutingKeys);
        } catch (Exception x) {
            LOG.info("Encountered the following error getting requestedRoutingKeys", x);
            return ResponseHelper.INSTANCE.errorResponse("Encountered the following error getting requestedRoutingKeys", x);
        }
    }
}
