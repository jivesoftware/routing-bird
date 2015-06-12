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

public class ConnectionDescriptors {

    private final long timestamp;
    private final List<ConnectionDescriptor> connectionDescriptors;

    @JsonCreator
    public ConnectionDescriptors(@JsonProperty("timestamp") long timestamp,
            @JsonProperty("connectionDescriptors") List<ConnectionDescriptor> connectionDescriptors) {
        this.timestamp = timestamp;
        this.connectionDescriptors = connectionDescriptors;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<ConnectionDescriptor> getConnectionDescriptors() {
        return connectionDescriptors;
    }

    @Override
    public String toString() {
        return "ConnectionDescriptors{" + "timestamp=" + timestamp + ", connectionDescriptors=" + connectionDescriptors + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + (int) (this.timestamp ^ (this.timestamp >>> 32));
        hash = 41 * hash + Objects.hashCode(this.connectionDescriptors);
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
        final ConnectionDescriptors other = (ConnectionDescriptors) obj;
        if (this.timestamp != other.timestamp) {
            return false;
        }
        if (!Objects.equals(this.connectionDescriptors, other.connectionDescriptors)) {
            return false;
        }
        return true;
    }
}
