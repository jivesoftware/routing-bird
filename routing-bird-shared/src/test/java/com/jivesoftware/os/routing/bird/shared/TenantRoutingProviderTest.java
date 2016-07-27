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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TenantRoutingProviderTest {

    private ConnectionDescriptorsProvider connectionDescriptorsProvider;
    private TenantRoutingProvider provider;

    @BeforeMethod
    public void setUp() {
        connectionDescriptorsProvider = Mockito.mock(ConnectionDescriptorsProvider.class);
        provider = new TenantRoutingProvider(Executors.newScheduledThreadPool(1), "1234", connectionDescriptorsProvider);
    }

    @Test
    public void testGetConnections() throws Exception {

        ConnectionDescriptorsRequest request = new ConnectionDescriptorsRequest("tenant", "1234", "serviceA", "port1", null);

        List<ConnectionDescriptor> connections = new ArrayList<>();
        InstanceDescriptor instanceDescriptor = new InstanceDescriptor("dc", "rk", "ph", "ck", "cn", "sk", "sn", "rgk", "rgn", "ik", 1, "vn", "r", 0, true);
        connections.add(new ConnectionDescriptor(instanceDescriptor, new HostPort("a", 1), new HashMap<>()));
        ConnectionDescriptorsResponse response = new ConnectionDescriptorsResponse(0, null, "releaseGroupA", connections, null);
        Mockito.when(connectionDescriptorsProvider.requestConnections(Mockito.eq(request), Mockito.any())).thenReturn(response);

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

        ConnectionDescriptorsRequest request = new ConnectionDescriptorsRequest("tenant", "1234", "serviceA", "port1", null);
        ConnectionDescriptorsResponse response = new ConnectionDescriptorsResponse(0, null, "releaseGroupA", null, null);
        Mockito.when(connectionDescriptorsProvider.requestConnections(Mockito.eq(request), Mockito.any())).thenReturn(response);

        TenantsServiceConnectionDescriptorProvider connections = provider.getConnections("serviceA", "port1", 60_000);
        routingReport = provider.getRoutingReport();
        Assert.assertTrue(routingReport.serviceReport.size() == 1);

    }
}
