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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TenantsServiceConnectionDescriptorsProviderTest {

    @Mock
    private ConnectionDescriptorsProvider connectionDescriptorsProvider;
    private String tenantId = "testTenant";
    private String instanceId = "testInstance";
    private String serviceId = "testService";
    private String port = "testPort";
    private String userId = "testUser";
    private ConnectionDescriptor descriptor;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        MockitoAnnotations.initMocks(this);
        ConnectionDescriptorsRequest connectionDescriptorsRequest = new ConnectionDescriptorsRequest(tenantId, instanceId, serviceId, port);

        InstanceDescriptor instanceDescriptor = new InstanceDescriptor("dc", "rk", "ph", "ck", "cn", "sk", "sn", "rgk", "rgn", "ik", 1, "vn", "r", 0, true);
        descriptor = new ConnectionDescriptor(instanceDescriptor, new HostPort("localhost", 7776), Collections.EMPTY_MAP);

        Mockito.when(connectionDescriptorsProvider.requestConnections(Mockito.eq(connectionDescriptorsRequest))).
            thenReturn(new ConnectionDescriptorsResponse(200, Collections.<String>emptyList(),
                userId, Arrays.asList(descriptor)));
    }

    @Test
    public void testGetConnections() {
        TenantsServiceConnectionDescriptorProvider<String> tenantsServiceConnectionPoolProvider = new TenantsServiceConnectionDescriptorProvider<>(
            Executors.newScheduledThreadPool(1),
            instanceId,
            connectionDescriptorsProvider,
            serviceId,
            port,
            60_000);
        tenantsServiceConnectionPoolProvider.getConnections(tenantId);
        ConnectionDescriptors connections = tenantsServiceConnectionPoolProvider.getConnections(tenantId);

        Assert.assertNotNull(connections);
        List<ConnectionDescriptor> descriptors = connections.getConnectionDescriptors();

        Assert.assertNotNull(descriptors);
        Assert.assertEquals(descriptors.size(), 1);
        Assert.assertEquals(descriptors.get(0), descriptor);

        ConnectionDescriptors connections2 = tenantsServiceConnectionPoolProvider.getConnections(tenantId);
        Assert.assertSame(connections2, connections);

        connections = tenantsServiceConnectionPoolProvider.getConnections("bogusTenant");
        Assert.assertNotNull(connections);
        descriptors = connections.getConnectionDescriptors();
        Assert.assertNotNull(descriptors);
        Assert.assertEquals(connections.getConnectionDescriptors().size(), 0);

        tenantsServiceConnectionPoolProvider.invalidateTenant(tenantId);
        connections2 = tenantsServiceConnectionPoolProvider.getConnections(tenantId);
        Assert.assertNotNull(connections2);
        Assert.assertNotSame(connections2, connections);

    }

    @Test
    public void testRoutingReport() {
        TenantsServiceConnectionDescriptorProvider<String> tenantsServiceConnectionPoolProvider = new TenantsServiceConnectionDescriptorProvider<>(
            Executors.newScheduledThreadPool(1),
            instanceId,
            connectionDescriptorsProvider,
            serviceId,
            port,
            60_000);
        tenantsServiceConnectionPoolProvider.getConnections(tenantId);
        ConnectionDescriptors connections = tenantsServiceConnectionPoolProvider.getConnections(tenantId);

        Assert.assertNotNull(connections);
        List<ConnectionDescriptor> descriptors = connections.getConnectionDescriptors();

        Assert.assertNotNull(descriptors);
        Assert.assertEquals(descriptors.size(), 1);
        Assert.assertEquals(descriptors.get(0), descriptor);
        TenantsRoutingServiceReport<String> routingReport = tenantsServiceConnectionPoolProvider.getRoutingReport();
        Assert.assertNotNull(routingReport);

        Map<String, String> tenantToUser = routingReport.tenantToUserId;
        Assert.assertNotNull(tenantToUser);
        Assert.assertEquals(tenantToUser.size(), 1);
        String retreivedId = tenantToUser.get(tenantId);
        Assert.assertEquals(retreivedId, userId);

        Map<String, ConnectionDescriptors> idtoPools = routingReport.userIdsConnectionDescriptors;
        Assert.assertNotNull(idtoPools);
        Assert.assertEquals(idtoPools.size(), 1);
        idtoPools.get(userId);
        Assert.assertEquals(idtoPools.get(userId), connections);
    }
}
