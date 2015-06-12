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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConnectionsDescriptorsProvider implements ConnectionDescriptorsProvider {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final List<ConnectionDescriptor> defaultConnectionDescriptor;
    private final Set<String> routingKeys = new HashSet<>();
    private final ConcurrentHashMap<String, ConnectionDescriptor> routings = new ConcurrentHashMap<>();

    public InMemoryConnectionsDescriptorsProvider(List<ConnectionDescriptor> defaultConnectionDescriptor) {
        this.defaultConnectionDescriptor = defaultConnectionDescriptor;
    }

    private String key(String tenantId, String instanceId, String connectToServiceNamed, String portName) {
        return "tenantId=" + tenantId + "&instanceId=" + instanceId + "&connectToServiceNamed=" + connectToServiceNamed + "&portName=" + portName;
    }

    public void set(String tenantId, String instanceId, String connectToServiceNamed, String portName, ConnectionDescriptor connectionDescriptor) {
        String key = key(tenantId, instanceId, connectToServiceNamed, portName);
        LOG.info("Setting routing override for " + key + " to be " + connectionDescriptor);
        routings.put(key, connectionDescriptor);
    }

    public ConnectionDescriptor get(String tenantId, String instanceId, String connectToServiceNamed, String portName) {
        return routings.get(key(tenantId, instanceId, connectToServiceNamed, portName));
    }

    public void clear(String tenantId, String instanceId, String connectToServiceNamed, String portName) {
        String key = key(tenantId, instanceId, connectToServiceNamed, portName);
        LOG.info("Clearing routing override for " + key);
        routings.remove(key);
    }

    public Collection<String> getRequestedRoutingKeys() {
        return Arrays.asList(routingKeys.toArray(new String[routingKeys.size()]));
    }

    @Override
    public ConnectionDescriptorsResponse requestConnections(ConnectionDescriptorsRequest connectionsRequest) {
        String key = key(connectionsRequest.getTenantId(),
            connectionsRequest.getInstanceId(),
            connectionsRequest.getConnectToServiceNamed(),
            connectionsRequest.getPortName());
        routingKeys.add(key);
        ConnectionDescriptor connectionDescriptor = routings.get(key);
        List<ConnectionDescriptor> connectionDescriptors = new ArrayList<>();
        String releaseGroup;
        if (connectionDescriptor == null) {
            LOG.info("current there is NOT a manual override for " + key);
            releaseGroup = "default";
            if (defaultConnectionDescriptor != null) {
                connectionDescriptors.addAll(defaultConnectionDescriptor);
            }
        } else {
            LOG.info("overriding routing for" + connectionsRequest + " to be " + connectionDescriptor);
            releaseGroup = "overridden";
            connectionDescriptors.add(connectionDescriptor);
        }
        ConnectionDescriptorsResponse response = new ConnectionDescriptorsResponse(1, Arrays.asList("Success"),
            releaseGroup, connectionDescriptors);
        return response;
    }
}
