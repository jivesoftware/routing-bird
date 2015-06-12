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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class ConnectionDescriptorsRequest {

    private final String tenantId;
    private final String instanceId;
    private final String connectToServiceNamed;
    private final String portName;

    /**
     *
     * @param tenantId empty string is ok and can be used to get all a connection pool regardless of tenant.
     * @param instanceId cannot be null
     * @param connectToServiceNamed cannot be null
     * @param portName cannot be null
     */
    @JsonCreator
    public ConnectionDescriptorsRequest(@JsonProperty("tenantId") String tenantId,
            @JsonProperty("instanceId") String instanceId,
            @JsonProperty("connectToServiceNamed") String connectToServiceNamed,
            @JsonProperty("portName") String portName) {
        this.tenantId = tenantId;
        this.instanceId = instanceId;
        this.connectToServiceNamed = connectToServiceNamed;
        this.portName = portName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getConnectToServiceNamed() {
        return connectToServiceNamed;
    }

    public String getPortName() {
        return portName;
    }

    @Override
    public String toString() {
        return "ConnectionsRequest{" + "tenantId=" + tenantId
                + ", instanceId=" + instanceId + ", connectToServiceNamed=" + connectToServiceNamed + ", portName=" + portName + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + Objects.hashCode(this.tenantId);
        hash = 73 * hash + Objects.hashCode(this.instanceId);
        hash = 73 * hash + Objects.hashCode(this.connectToServiceNamed);
        hash = 73 * hash + Objects.hashCode(this.portName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConnectionDescriptorsRequest other = (ConnectionDescriptorsRequest) obj;
        if (!Objects.equals(this.tenantId, other.tenantId)) {
            return false;
        }
        if (!Objects.equals(this.instanceId, other.instanceId)) {
            return false;
        }
        if (!Objects.equals(this.connectToServiceNamed, other.connectToServiceNamed)) {
            return false;
        }
        if (!Objects.equals(this.portName, other.portName)) {
            return false;
        }
        return true;
    }
}
