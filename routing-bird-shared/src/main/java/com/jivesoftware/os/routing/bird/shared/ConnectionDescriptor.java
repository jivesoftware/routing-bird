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

    private final InstanceDescriptor instanceDescriptor;
    private final boolean sslEnabled;
    private final boolean serviceAuthEnabled;
    private final HostPort hostPort;
    private final Map<String, String> properties;
    private final Map<String, String> monkeys;

    @JsonCreator
    public ConnectionDescriptor(@JsonProperty("instanceDescriptor") InstanceDescriptor instanceDescriptor,
        @JsonProperty("sslEnabled") boolean sslEnabled,
        @JsonProperty("serviceAuthEnabled") boolean serviceAuthEnabled,
        @JsonProperty("hostPort") HostPort hostPort,
        @JsonProperty("properties") Map<String, String> properties,
        @JsonProperty("monkeys") Map<String, String> monkeys) {

        this.sslEnabled = sslEnabled;
        this.serviceAuthEnabled = serviceAuthEnabled;
        this.instanceDescriptor = instanceDescriptor;
        this.hostPort = hostPort;
        this.properties = properties;
        this.monkeys = monkeys;
    }
    
    public boolean getSslEnabled() {
        return sslEnabled;
    }

    public boolean getServiceAuthEnabled() {
        return serviceAuthEnabled;
    }

    public InstanceDescriptor getInstanceDescriptor() {
        return instanceDescriptor;
    }

    public HostPort getHostPort() {
        return hostPort;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, String> getMonkeys() {
        return monkeys;
    }

    @Override
    public String toString() {
        return "ConnectionDescriptor{"
            + "instanceDescriptor=" + instanceDescriptor
            + ", sslEnabled=" + sslEnabled
            + ", publicKey=" + "*******"
            + ", hostPort=" + hostPort
            + ", properties=" + properties
            + ", monkeys=" + monkeys
            + '}';
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
