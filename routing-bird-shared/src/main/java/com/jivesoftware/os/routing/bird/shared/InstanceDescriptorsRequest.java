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

public class InstanceDescriptorsRequest {

    public final String hostKey;

    @JsonCreator
    public InstanceDescriptorsRequest(@JsonProperty("hostKey") String hostKey) {
        this.hostKey = hostKey;
    }

    @Override
    public String toString() {
        return "InstanceDescriptorsRequest{" + "hostKey=" + hostKey + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.hostKey);
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
        final InstanceDescriptorsRequest other = (InstanceDescriptorsRequest) obj;
        if (!Objects.equals(this.hostKey, other.hostKey)) {
            return false;
        }
        return true;
    }
}
