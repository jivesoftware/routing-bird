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

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TenantRoutingProvider<T> {

    private final ConcurrentHashMap<String, TenantsServiceConnectionDescriptorProvider<T>> serviceConnectionDescriptorsProvider = new ConcurrentHashMap<>();
    private final String instanceId;
    private final ConnectionDescriptorsProvider connectionsDescriptorProvider;

    public TenantRoutingProvider(String instanceId, ConnectionDescriptorsProvider connectionsDescriptorProvider) {
        this.instanceId = instanceId;
        this.connectionsDescriptorProvider = connectionsDescriptorProvider;
    }

    private String key(String connectToService, String portName) {
        return connectToService + "." + portName;
    }

    public void invalidateAll() {
        for (TenantsServiceConnectionDescriptorProvider<T> v : serviceConnectionDescriptorsProvider.values()) {
            v.invalidateAll();
        }
    }

    public void invalidateTenant(String connectToService, String portName, T tenantId) {
        TenantsServiceConnectionDescriptorProvider<T> got = serviceConnectionDescriptorsProvider.get(key(connectToService, portName));
        if (got != null) {
            got.invalidateTenant(tenantId);
        }
    }

    public TenantsRoutingReport getRoutingReport() {
        TenantsRoutingReport report = new TenantsRoutingReport();
        for (Entry<String, TenantsServiceConnectionDescriptorProvider<T>> e : serviceConnectionDescriptorsProvider.entrySet()) {
            report.serviceReport.put(e.getKey(), e.getValue().getRoutingReport());
        }
        return report;
    }

    public TenantsServiceConnectionDescriptorProvider getConnections(String connectToServiceNamed, String portName) {
        if (connectToServiceNamed == null) {
            return null;
        }
        if (portName == null) {
            return null;
        }
        String key = key(connectToServiceNamed, portName);
        TenantsServiceConnectionDescriptorProvider<T> got = serviceConnectionDescriptorsProvider.get(key);
        if (got != null) {
            return got;
        }
        got = new TenantsServiceConnectionDescriptorProvider<>(instanceId, connectionsDescriptorProvider, connectToServiceNamed, portName);
        TenantsServiceConnectionDescriptorProvider<T> had = serviceConnectionDescriptorsProvider.putIfAbsent(key(connectToServiceNamed, portName), got);
        if (had != null) {
            got = had;
        }
        return got;
    }
}
