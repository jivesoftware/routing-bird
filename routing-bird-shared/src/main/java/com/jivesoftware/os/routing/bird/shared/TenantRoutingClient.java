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

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class TenantRoutingClient<T, C, E extends Throwable> {

    static private final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final TenantsServiceConnectionDescriptorProvider<T> connectionPoolProvider;
    private final ClientConnectionsFactory<C, E> clientConnectionsFactory;
    private final ClientsCloser<C> clientsCloser;
    private final ConcurrentHashMap<T, TimestampedClients<C, E>> tenantsHttpClient = new ConcurrentHashMap<>();

    public TenantRoutingClient(TenantsServiceConnectionDescriptorProvider<T> connectionPoolProvider,
        ClientConnectionsFactory<C, E> clientConnectionsFactory,
        ClientsCloser<C> clientsCloser) {
        this.connectionPoolProvider = connectionPoolProvider;
        this.clientConnectionsFactory = clientConnectionsFactory;
        this.clientsCloser = clientsCloser;
    }

    public <R> R tenantAwareCall(T tenant, NextClientStrategy strategy, ClientCall<C, R, E> call) throws E {
        if (tenant == null) {
            throw new IllegalArgumentException("tenant cannot be null.");
        }
        ConnectionDescriptors connections = connectionPoolProvider.getConnections(tenant);
        TimestampedClients<C, E> timestampedClients = tenantsHttpClient.get(tenant);
        if (timestampedClients == null || timestampedClients.getTimestamp() < connections.getTimestamp()) {
            if (timestampedClients != null) {
                try {
                    clientsCloser.closeClients(timestampedClients.getClients());
                } catch (Exception x) {
                    LOG.warn("Failed while trying to close clients:" + Arrays.toString(timestampedClients.getClients()), x);
                }
            }
            timestampedClients = clientConnectionsFactory.createClients(connections);
            if (timestampedClients == null) {
                throw new IllegalStateException("clientConnectionsFactory:" + clientConnectionsFactory + " should not return a null client but did!");
            }
            tenantsHttpClient.put(tenant, timestampedClients);
        }
        return timestampedClients.call(strategy, call);
    }

    public void closeAll() {
        // TODO??
    }
}
