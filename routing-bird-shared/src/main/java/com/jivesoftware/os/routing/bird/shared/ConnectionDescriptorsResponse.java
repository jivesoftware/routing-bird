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

public class ConnectionDescriptorsResponse {

    private final int returnCode;
    private final List<String> messages;
    private final String releaseGroup;
    private final List<ConnectionDescriptor> connections;
    private final String requestUuid;

    @JsonCreator
    public ConnectionDescriptorsResponse(@JsonProperty("returnCode") int returnCode,
        @JsonProperty("messages") List<String> messages,
        @JsonProperty("releaseGroup") String releaseGroup,
        @JsonProperty("connections") List<ConnectionDescriptor> connections,
        @JsonProperty("requestUuid") String requestUuid) {
        this.returnCode = returnCode;
        this.messages = messages;
        this.releaseGroup = releaseGroup;
        this.connections = connections;
        this.requestUuid = requestUuid;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getReleaseGroup() {
        return releaseGroup;
    }

    public List<ConnectionDescriptor> getConnections() {
        return connections;
    }

    public String getRequestUuid() {
        return requestUuid;
    }

    @Override
    public String toString() {
        return "ConnectionDescriptorsResponse{" +
            "returnCode=" + returnCode +
            ", messages=" + messages +
            ", releaseGroup='" + releaseGroup + '\'' +
            ", connections=" + connections +
            ", requestUuid='" + requestUuid + '\'' +
            '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.returnCode;
        hash = 29 * hash + Objects.hashCode(this.messages);
        hash = 29 * hash + Objects.hashCode(this.releaseGroup);
        hash = 29 * hash + Objects.hashCode(this.connections);
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
        final ConnectionDescriptorsResponse other = (ConnectionDescriptorsResponse) obj;
        if (this.returnCode != other.returnCode) {
            return false;
        }
        if (!Objects.equals(this.messages, other.messages)) {
            return false;
        }
        if (!Objects.equals(this.releaseGroup, other.releaseGroup)) {
            return false;
        }
        if (!Objects.equals(this.connections, other.connections)) {
            return false;
        }
        return true;
    }

}
