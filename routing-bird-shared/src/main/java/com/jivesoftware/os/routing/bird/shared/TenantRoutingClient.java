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

    public <R> R tenantAwareCall(T tenant, NextClientStrategy strategy, String family, ClientCall<C, R, E> call) throws E {
        if (tenant == null) {
            throw new IllegalArgumentException("tenant cannot be null.");
        }
        String routingGroup = connectionPoolProvider.getRoutingGroup(tenant);
        ConnectionDescriptors connections = connectionPoolProvider.getConnections(tenant);
        TimestampedClients<C, E> timestampedClients = tenantsHttpClient.compute(tenant, (key, existing) -> {
            String existingRoutingGroup = existing == null ? null : existing.getRoutingGroup();
            long existingTimestamp =  existing == null ? -1 : existing.getTimestamp();
            long timestamp = connections.getTimestamp();
            if (existingRoutingGroup == null || !existingRoutingGroup.equals(routingGroup) || existingTimestamp < timestamp) {
                LOG.info("Updating routes for tenant:{} family:{} routingGroup:{}->{} timestamp:{}->{}",
                    tenant, family, existingRoutingGroup, routingGroup, existingTimestamp, timestamp);
                if (existing != null) {
                    try {
                        clientsCloser.closeClients(existing.getClients());
                    } catch (Exception x) {
                        LOG.warn("Failed while trying to close clients:" + Arrays.toString(existing.getClients()), x);
                    }
                }
                TimestampedClients<C, E> updated = clientConnectionsFactory.createClients(routingGroup, connections);
                if (updated == null) {
                    throw new IllegalStateException("clientConnectionsFactory:" + clientConnectionsFactory + " should not return a null client but did!");
                }
                return updated;
            } else {
                return existing;
            }
        });
        return timestampedClients.call(strategy, family, call);
    }

    public void closeAll() {
        // TODO??
    }
}
