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
package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.routing.bird.shared.ClientConnectionsFactory;
import com.jivesoftware.os.routing.bird.shared.ClientsCloser;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.TenantRoutingClient;
import com.jivesoftware.os.routing.bird.shared.TenantsServiceConnectionDescriptorProvider;
import java.util.ArrayList;
import java.util.List;


public class TenantRoutingHttpClientInitializer<T> {

    public TenantAwareHttpClient<T> initialize(TenantsServiceConnectionDescriptorProvider<T> connectionPoolProvider) {

        ClientConnectionsFactory<HttpClient, HttpClientException> clientConnectionsFactory = connectionDescriptors -> {
            List<ConnectionDescriptor> descriptors = connectionDescriptors.getConnectionDescriptors();
            ConnectionDescriptor[] connections = descriptors.toArray(new ConnectionDescriptor[descriptors.size()]);
            HttpClient[] httpClients = new HttpClient[descriptors.size()];
            HttpClientFactoryProvider httpClientFactoryProvider = new HttpClientFactoryProvider();
            for (int i = 0; i < connections.length; i++) {
                ConnectionDescriptor connection = connections[i];
                List<HttpClientConfiguration> config = new ArrayList<>();
                config.add(HttpClientConfig
                    .newBuilder()
                    .setMaxConnections(32) // TODO expose to config
                    .setSocketTimeoutInMillis(600000) // TODO fix get this from connectionDescriptors.properties
                    .build());
                HttpClientFactory createHttpClientFactory = httpClientFactoryProvider.createHttpClientFactory(config);
                HttpClient httpClient = createHttpClientFactory.createClient(connection.getHostPort().getHost(), connection.getHostPort().getPort());
                httpClients[i] = httpClient;
            }

            return new ErrorCheckingTimestampedClients<>(System.currentTimeMillis(), connections, httpClients, 10, 10_000); //TODO config
        };

        ClientsCloser<HttpClient> clientsCloser = clients -> {
        };

        TenantRoutingClient<T, HttpClient, HttpClientException> tenantRoutingClient = new TenantRoutingClient<>(connectionPoolProvider,
            clientConnectionsFactory, clientsCloser);
        return new TenantRoutingHttpClient<>(tenantRoutingClient);
    }
}
