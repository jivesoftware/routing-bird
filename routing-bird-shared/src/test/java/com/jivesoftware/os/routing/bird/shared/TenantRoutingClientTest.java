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

import com.jivesoftware.os.routing.bird.shared.ClientCall.ClientResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.MockitoAnnotations.initMocks;

public class TenantRoutingClientTest {

    @Mock
    private ClientCall<TestClient, Boolean, IOException> clientCall;
    @Mock
    private ClientsCloser<TestClient> closer;
    @Mock
    private TenantsServiceConnectionDescriptorProvider<String> tenantsServiceConnectionDescriptorProvider;
    @Mock
    private ClientConnectionsFactory<TestClient, IOException> clientConnectionsFactory;
    private String tenantId = "testTenant";
    private TestClient[] testClients = new TestClient[]{new TestClient()};
    private NextClientStrategy strategy;

    @BeforeMethod
    public void setUp() {
        initMocks(this);
        try {
            Mockito.when(clientCall.call(Mockito.any(TestClient.class))).thenReturn(new ClientResponse<>(Boolean.TRUE, true));
        } catch (IOException ex) {
            Assert.fail();
        }

        initDescriptorsPool(System.currentTimeMillis());
    }

    private void initDescriptorsPool(long timestamp) {
        InstanceDescriptor instanceDescriptor = new InstanceDescriptor("dc", "rk", "ph", "ck", "cn", "sk", "sn", "rgk", "rgn", "ik", 1, "vn", "r", 0, true);
        ConnectionDescriptor descriptor = new ConnectionDescriptor(instanceDescriptor, new HostPort("localhost", 7777), Collections.emptyMap());
        ConnectionDescriptors connectionDescriptors = new ConnectionDescriptors(timestamp, Arrays.asList(descriptor));
        strategy = new TestStrategy();
        Mockito.when(tenantsServiceConnectionDescriptorProvider.getConnections(tenantId)).thenReturn(connectionDescriptors);
        Mockito.when(clientConnectionsFactory.createClients(connectionDescriptors)).thenReturn(new TestTimestampedClients(testClients));
    }

    @Test
    public void testTenantAwareCall() throws Exception {
        TenantRoutingClient<String, TestClient, IOException> instance = new TenantRoutingClient<>(
            tenantsServiceConnectionDescriptorProvider, clientConnectionsFactory, closer);
        Boolean expResult = true;
        Boolean result = instance.tenantAwareCall(tenantId, strategy, "a", clientCall);

        Assert.assertEquals(result, expResult);
        Mockito.verifyZeroInteractions(closer);

        initDescriptorsPool(System.currentTimeMillis() + 1000);

        result = instance.tenantAwareCall(tenantId, strategy, "a", clientCall);
        Assert.assertEquals(result, expResult);
        Mockito.verify(closer).closeClients(testClients);
    }

    private static class TestClient {
    }

    private static class TestStrategy implements NextClientStrategy {

        @Override
        public int[] getClients(ConnectionDescriptor[] connectionDescriptors) {
            return IntStream.range(0, connectionDescriptors.length).toArray();
        }

        @Override
        public void usedClientAtIndex(int index) {
        }
    }

    private static class TestTimestampedClients implements TimestampedClients<TestClient, IOException> {

        private final TestClient[] clients;

        public TestTimestampedClients(TestClient[] clients) {
            this.clients = clients;
        }

        @Override
        public <R> R call(NextClientStrategy strategy, String family, ClientCall<TestClient, R, IOException> httpCall) throws IOException {
            return httpCall.call(clients[0]).response;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }

        @Override
        public TestClient[] getClients() {
            return clients;
        }
    }
}
