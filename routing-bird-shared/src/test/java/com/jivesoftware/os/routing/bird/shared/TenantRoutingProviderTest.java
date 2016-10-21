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
package com.jivesoftware.os.routing.bird.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TenantRoutingProviderTest {

    private AtomicReference<ConnectionDescriptorsResponse> connectionDescriptorsProviderResponse = new AtomicReference<>();
    private ConnectionDescriptorsProvider connectionDescriptorsProvider;
    private TenantRoutingProvider provider;

    @BeforeMethod
    public void setUp() {
        connectionDescriptorsProvider = new ConnectionDescriptorsProvider() {
            @Override
            public ConnectionDescriptorsResponse requestConnections(ConnectionDescriptorsRequest connectionsRequest, String expectedReleaseGroup) {
                if (connectionsRequest.getTenantId().equals("tenant")
                    && connectionsRequest.getInstanceId().equals("1234")
                    && connectionsRequest.getConnectToServiceNamed().equals("serviceA")
                    && connectionsRequest.getPortName().equals("port1")) {
                    return connectionDescriptorsProviderResponse.get();
                } else {
                    return null;
                }
            }
        };
        provider = new TenantRoutingProvider(Executors.newScheduledThreadPool(1), "1234", connectionDescriptorsProvider);
    }

    @Test
    public void testGetConnections() throws Exception {

        List<ConnectionDescriptor> connections = new ArrayList<>();
        InstanceDescriptor instanceDescriptor = new InstanceDescriptor("dc", "rk", "ph", "ck", "cn", "sk", "sn", "rgk", "rgn", "ik", 1, "vn", "r", "pk", 0, true);
        connections.add(new ConnectionDescriptor(instanceDescriptor, false, false, new HostPort("a", 1), Collections.EMPTY_MAP, Collections.EMPTY_MAP));
        ConnectionDescriptorsResponse response = new ConnectionDescriptorsResponse(0, null, "releaseGroupA", connections, null);
        connectionDescriptorsProviderResponse.set(response);

        TenantsServiceConnectionDescriptorProvider descriptorProvider = provider.getConnections("invalid", null, 60_000);
        Assert.assertNull(descriptorProvider);

        descriptorProvider = provider.getConnections(null, "invalid", 60_000);
        Assert.assertNull(descriptorProvider);

        descriptorProvider = provider.getConnections(null, null, 60_000);
        Assert.assertNull(descriptorProvider);

        descriptorProvider = provider.getConnections("serviceA", "port1", 60_000);
        Assert.assertNotNull(descriptorProvider);
        ConnectionDescriptors connectionDescriptors = descriptorProvider.getConnections("invalid");
        Assert.assertTrue(connectionDescriptors.getConnectionDescriptors().isEmpty());

        connectionDescriptors = descriptorProvider.getConnections("tenant");
        System.out.println(connectionDescriptors);
        Assert.assertTrue(connectionDescriptors.getConnectionDescriptors().size() == 1);
        Assert.assertEquals(connectionDescriptors.getConnectionDescriptors().get(0).getHostPort().getHost(), "a");
    }

    @Test
    public void testGetRoutingReport() throws Exception {
        TenantsRoutingReport routingReport = provider.getRoutingReport();
        Assert.assertTrue(routingReport.serviceReport.isEmpty());

        TenantsServiceConnectionDescriptorProvider connections = provider.getConnections("serviceA", "port1", 60_000);
        routingReport = provider.getRoutingReport();
        Assert.assertTrue(routingReport.serviceReport.size() == 1);

    }
}
