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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceDescriptor {

    public final String publicHost;
    public final String clusterKey;
    public final String clusterName;
    public final String serviceKey;
    public final String serviceName;
    public final String releaseGroupKey;
    public final String releaseGroupName;
    public final String instanceKey;
    public final int instanceName;
    public final String versionName;
    public final String repository;
    public final Map<String, InstanceDescriptorPort> ports = new ConcurrentHashMap<>();
    public final long restartTimestampGMTMillis; // deliberately not part of hash or equals.
    public final boolean enabled;

    @JsonCreator
    public InstanceDescriptor(@JsonProperty(value = "publicHost") String publicHost,
        @JsonProperty(value = "clusterKey") String clusterKey,
        @JsonProperty("clusterName") String clusterName,
        @JsonProperty("serviceKey") String serviceKey,
        @JsonProperty("serviceName") String serviceName,
        @JsonProperty("releaseGroupKey") String releaseGroupKey,
        @JsonProperty("releaseGroupName") String releaseGroupName,
        @JsonProperty("instanceKey") String instanceKey,
        @JsonProperty("instanceName") int instanceName,
        @JsonProperty("versionName") String versionName,
        @JsonProperty("repository") String repository,
        @JsonProperty("restartTimestampGMTMillis") long restartTimestampGMTMillis,
        @JsonProperty("enabled") boolean enabled) {

        this.publicHost = publicHost;
        this.clusterKey = clusterKey;
        this.clusterName = clusterName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        this.serviceKey = serviceKey;
        this.serviceName = serviceName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        this.releaseGroupKey = releaseGroupKey;
        this.releaseGroupName = releaseGroupName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        this.instanceKey = instanceKey;
        this.instanceName = instanceName;
        this.versionName = versionName;
        this.repository = repository;
        this.restartTimestampGMTMillis = restartTimestampGMTMillis;
        this.enabled = enabled;
    }

    public static class InstanceDescriptorPort {

        public final int port;

        @JsonCreator
        public InstanceDescriptorPort(@JsonProperty(value = "port") int port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "InstanceDescriptorPort{" + "port=" + port + '}';
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + this.port;
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
            final InstanceDescriptorPort other = (InstanceDescriptorPort) obj;
            if (this.port != other.port) {
                return false;
            }
            return true;
        }

    }

    @Override
    public String toString() {
        return "InstanceDescriptor{"
            + "publicHost=" + publicHost
            + ", clusterKey=" + clusterKey
            + ", clusterName=" + clusterName
            + ", serviceKey=" + serviceKey
            + ", serviceName=" + serviceName
            + ", releaseGroupKey=" + releaseGroupKey
            + ", releaseGroupName=" + releaseGroupName
            + ", instanceKey=" + instanceKey
            + ", instanceName=" + instanceName
            + ", versionName=" + versionName
            + ", repository=" + repository
            + ", ports=" + ports
            + ", restartTimestampGMTMillis=" + restartTimestampGMTMillis
            + ", enabled=" + enabled
            + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InstanceDescriptor that = (InstanceDescriptor) o;

        if (instanceName != that.instanceName) {
            return false;
        }
        if (enabled != that.enabled) {
            return false;
        }
        if (publicHost != null ? !publicHost.equals(that.publicHost) : that.publicHost != null) {
            return false;
        }
        if (clusterKey != null ? !clusterKey.equals(that.clusterKey) : that.clusterKey != null) {
            return false;
        }
        if (clusterName != null ? !clusterName.equals(that.clusterName) : that.clusterName != null) {
            return false;
        }
        if (serviceKey != null ? !serviceKey.equals(that.serviceKey) : that.serviceKey != null) {
            return false;
        }
        if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) {
            return false;
        }
        if (releaseGroupKey != null ? !releaseGroupKey.equals(that.releaseGroupKey) : that.releaseGroupKey != null) {
            return false;
        }
        if (releaseGroupName != null ? !releaseGroupName.equals(that.releaseGroupName) : that.releaseGroupName != null) {
            return false;
        }
        if (instanceKey != null ? !instanceKey.equals(that.instanceKey) : that.instanceKey != null) {
            return false;
        }
        if (versionName != null ? !versionName.equals(that.versionName) : that.versionName != null) {
            return false;
        }
        if (repository != null ? !repository.equals(that.repository) : that.repository != null) {
            return false;
        }
        return !(ports != null ? !ports.equals(that.ports) : that.ports != null);

    }

    @Override
    public int hashCode() {
        int result = publicHost != null ? publicHost.hashCode() : 0;
        result = 31 * result + (clusterKey != null ? clusterKey.hashCode() : 0);
        result = 31 * result + (clusterName != null ? clusterName.hashCode() : 0);
        result = 31 * result + (serviceKey != null ? serviceKey.hashCode() : 0);
        result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
        result = 31 * result + (releaseGroupKey != null ? releaseGroupKey.hashCode() : 0);
        result = 31 * result + (releaseGroupName != null ? releaseGroupName.hashCode() : 0);
        result = 31 * result + (instanceKey != null ? instanceKey.hashCode() : 0);
        result = 31 * result + instanceName;
        result = 31 * result + (versionName != null ? versionName.hashCode() : 0);
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (ports != null ? ports.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        return result;
    }
}
