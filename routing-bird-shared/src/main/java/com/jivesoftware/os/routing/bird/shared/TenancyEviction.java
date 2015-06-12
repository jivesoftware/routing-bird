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

public class TenancyEviction {

    private final long timestamp;
    private final String instanceId;
    private final String connectToServiceId;
    private final String portName;
    private final String tenantId;

    @JsonCreator
    public TenancyEviction(
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("instanceId") String instanceId,
            @JsonProperty("connectToServiceId") String connectToServiceId,
            @JsonProperty("portName") String portName,
            @JsonProperty("tenantId") String tenantId) {
        this.timestamp = timestamp;
        this.instanceId = instanceId;
        this.connectToServiceId = connectToServiceId;
        this.portName = portName;
        this.tenantId = tenantId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getConnectToServiceId() {
        return connectToServiceId;
    }

    public String getPortName() {
        return portName;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String toString() {
        return "TenancyEviction{"
                + "timestamp=" + timestamp
                + ", instanceId=" + instanceId
                + ", connectToServiceId=" + connectToServiceId
                + ", portName=" + portName
                + ", tenantId=" + tenantId
                + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (int) (this.timestamp ^ (this.timestamp >>> 32));
        hash = 59 * hash + Objects.hashCode(this.instanceId);
        hash = 59 * hash + Objects.hashCode(this.connectToServiceId);
        hash = 59 * hash + Objects.hashCode(this.portName);
        hash = 59 * hash + Objects.hashCode(this.tenantId);
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
        final TenancyEviction other = (TenancyEviction) obj;
        if (this.timestamp != other.timestamp) {
            return false;
        }
        if (!Objects.equals(this.instanceId, other.instanceId)) {
            return false;
        }
        if (!Objects.equals(this.connectToServiceId, other.connectToServiceId)) {
            return false;
        }
        if (!Objects.equals(this.portName, other.portName)) {
            return false;
        }
        if (!Objects.equals(this.tenantId, other.tenantId)) {
            return false;
        }
        return true;
    }
}
