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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TenantsServiceConnectionDescriptorProvider<T> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final String instanceId;
    private final ConnectionDescriptorsProvider connectionsProvider;
    private final String connectToServiceNamed;
    private final String portName;
    private final long refreshConnectionsAfterNMillis;
    private final Map<String, ConnectionDescriptors> releaseGroupToConnectionDescriptors = new ConcurrentHashMap<>();
    private final Map<T, String> tenantToReleaseGroup = new ConcurrentHashMap<>();
    private final Map<T, AtomicBoolean> activeTenants = new ConcurrentHashMap<>();
    private final ScheduledExecutorService connectionsRefresher;

    public TenantsServiceConnectionDescriptorProvider(ScheduledExecutorService connectionsRefresher,
        String instanceId,
        ConnectionDescriptorsProvider connectionsProvider,
        String connectToServiceNamed,
        String portName,
        long refreshConnectionsAfterNMillis) {

        this.connectionsRefresher = connectionsRefresher;
        this.instanceId = instanceId;
        this.connectionsProvider = connectionsProvider;
        this.connectToServiceNamed = connectToServiceNamed;
        this.portName = portName;
        this.refreshConnectionsAfterNMillis = refreshConnectionsAfterNMillis;
    }

    void invalidateAll() {
        tenantToReleaseGroup.clear();
        releaseGroupToConnectionDescriptors.clear();
    }

    public void invalidateTenant(T tenantId) {
        tenantToReleaseGroup.remove(tenantId);
    }

    public TenantsRoutingServiceReport<T> getRoutingReport() {
        TenantsRoutingServiceReport<T> report = new TenantsRoutingServiceReport<>();
        report.tenantToUserId.putAll(tenantToReleaseGroup);
        report.userIdsConnectionDescriptors.putAll(releaseGroupToConnectionDescriptors);
        return report;
    }

    public String getRoutingGroup(T tenantId) {
        return tenantId == null ? "unknown" : tenantToReleaseGroup.getOrDefault(tenantId, "unknown");
    }

    public ConnectionDescriptors getConnections(T tenantId) {
        if (tenantId == null) {
            return new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
        }
        ConnectionDescriptors connectionDescriptors;
        String releaseGroup = tenantToReleaseGroup.get(tenantId);
        if (releaseGroup != null) {
            connectionDescriptors = releaseGroupToConnectionDescriptors.computeIfAbsent(releaseGroup, (key) -> {
                return new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
            });
        } else {
            connectionDescriptors = refreshConnections(tenantId);
        }
        activeTenants.computeIfAbsent(tenantId, (T t) -> new AtomicBoolean()).set(true);
        return connectionDescriptors;
    }

    private ConnectionDescriptors refreshConnections(T tenantId) {
        ConnectionDescriptorsRequest connectionDescriptorsRequest = new ConnectionDescriptorsRequest(
            tenantId.toString(), instanceId, connectToServiceNamed, portName);

        ConnectionDescriptorsResponse connectionsResponse = connectionsProvider.requestConnections(connectionDescriptorsRequest);
        if (connectionsResponse == null) {
            String releaseGroup = tenantToReleaseGroup.get(tenantId);
            if (releaseGroup != null) {
                ConnectionDescriptors connectionDescriptors = releaseGroupToConnectionDescriptors.get(releaseGroup);
                if (connectionDescriptors != null) {
                    return connectionDescriptors;
                }
            }
        }

        String releaseGroup;
        ConnectionDescriptors connections = null;
        if (connectionsResponse == null) {
            releaseGroup = "unknown";
            connections = new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
        } else if (connectionsResponse.getReturnCode() < 0) {
            releaseGroup = "unknown";
            LOG.warn(Arrays.deepToString(connectionsResponse.getMessages().toArray()));
            connections = new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
        } else {
            releaseGroup = connectionsResponse.getReleaseGroup();
            List<ConnectionDescriptor> latest = connectionsResponse.getConnections();
            ConnectionDescriptors current = releaseGroupToConnectionDescriptors.get(releaseGroup);
            if (current != null) {
                tenantToReleaseGroup.put(tenantId, releaseGroup);
                if (latest.size() == current.getConnectionDescriptors().size()) {
                    Set<ConnectionDescriptor> currentAsSet = new HashSet<>(current.getConnectionDescriptors());
                    for (ConnectionDescriptor connectionDescriptor : latest) {
                        currentAsSet.remove(connectionDescriptor);
                    }
                    if (currentAsSet.isEmpty()) {
                        connections = current;
                    }
                }
            }
            if (connections == null) {
                connections = new ConnectionDescriptors(System.currentTimeMillis(), latest);
            }
        }
        releaseGroupToConnectionDescriptors.put(releaseGroup, connections);
        tenantToReleaseGroup.put(tenantId, releaseGroup);
        return connections;
    }

    public void start() {

        connectionsRefresher.scheduleWithFixedDelay(() -> {
            try {
                for (Map.Entry<T, AtomicBoolean> entry : activeTenants.entrySet()) {
                    try {
                        if (entry.getValue().compareAndSet(true, false)) {
                            refreshConnections(entry.getKey());
                        }
                    } catch (Exception x) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Error refreshing connections.", x);
                        } else {
                            LOG.warn("failure refreshing connections.");
                        }
                    }
                }
            } catch (Exception x) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connections refresher swallowed.", x);
                } else {
                    LOG.warn("Connections refresher swallowed unexpected exception:{} message:{}", x.getClass().getName(), x.getMessage());
                }
            }
        }, refreshConnectionsAfterNMillis, refreshConnectionsAfterNMillis, TimeUnit.MILLISECONDS);
    }

}
