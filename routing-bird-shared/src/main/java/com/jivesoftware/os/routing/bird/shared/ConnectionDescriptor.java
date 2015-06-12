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
import java.util.Map;

public class ConnectionDescriptor {

    private final HostPort hostPort;
    private final Map<String, String> properties;

    @JsonCreator
    public ConnectionDescriptor(@JsonProperty("hostPort") HostPort hostPort,
        @JsonProperty("properties") Map<String, String> properties) {
        this.hostPort = hostPort;
        this.properties = properties;
    }

    public HostPort getHostPort() {
        return hostPort;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "ConnectionDescriptor{" +
            "hostPort=" + hostPort +
            ", properties=" + properties +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConnectionDescriptor that = (ConnectionDescriptor) o;

        if (hostPort != null ? !hostPort.equals(that.hostPort) : that.hostPort != null) {
            return false;
        }
        return !(properties != null ? !properties.equals(that.properties) : that.properties != null);

    }

    @Override
    public int hashCode() {
        int result = hostPort != null ? hostPort.hashCode() : 0;
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
