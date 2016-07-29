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

public class ConnectionDescriptorsRequest {

    private final String tenantId;
    private final String instanceId;
    private final String connectToServiceNamed;
    private final String portName;
    private final String requestUuid;

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
            @JsonProperty("portName") String portName,
            @JsonProperty("requestUuid") String requestUuid) {
        this.tenantId = tenantId;
        this.instanceId = instanceId;
        this.connectToServiceNamed = connectToServiceNamed;
        this.portName = portName;
        this.requestUuid = requestUuid;
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

    public String getRequestUuid() {
        return requestUuid;
    }

    @Override
    public String toString() {
        return "ConnectionDescriptorsRequest{" +
            "tenantId='" + tenantId + '\'' +
            ", instanceId='" + instanceId + '\'' +
            ", connectToServiceNamed='" + connectToServiceNamed + '\'' +
            ", portName='" + portName + '\'' +
            ", requestUuid='" + requestUuid + '\'' +
            '}';
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("NOPE");
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException("NOPE");
    }
}
