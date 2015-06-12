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
import java.util.List;
import java.util.Objects;

public class HostKeyRequest {

    public final List<String> hostNames;
    public final List<String> ipAddresses;

    @JsonCreator
    public HostKeyRequest(@JsonProperty("hostNames") List<String> hostNames,
            @JsonProperty("ipAddresses") List<String> ipAddresses) {
        this.hostNames = hostNames;
        this.ipAddresses = ipAddresses;
    }

    @Override
    public String toString() {
        return "HostKeyRequest{"
                + "hostNames=" + hostNames
                + ", ipAddresses=" + ipAddresses
                + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.hostNames);
        hash = 17 * hash + Objects.hashCode(this.ipAddresses);
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
        final HostKeyRequest other = (HostKeyRequest) obj;
        if (!Objects.equals(this.hostNames, other.hostNames)) {
            return false;
        }
        if (!Objects.equals(this.ipAddresses, other.ipAddresses)) {
            return false;
        }
        return true;
    }
}
