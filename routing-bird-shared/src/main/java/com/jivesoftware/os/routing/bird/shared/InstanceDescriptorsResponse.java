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
import java.util.ArrayList;
import java.util.List;

public class InstanceDescriptorsResponse {

    public static final String PORT_MAIN = "main";
    public static final String PORT_MANAGE = "manage";
    public static final String PORT_DEBUG = "debug";
    public static final String PORT_JMX = "jmx";
    public final String requestingHostId;
    public final boolean decommisionRequestingHost;
    public final List<InstanceDescriptor> instanceDescriptors = new ArrayList<>();

    @JsonCreator
    public InstanceDescriptorsResponse(@JsonProperty("requestingHostId") String requestingHostId,
            @JsonProperty("decommisionRequestingHost") boolean decommisionRequestingHost) {
        this.requestingHostId = requestingHostId;
        this.decommisionRequestingHost = decommisionRequestingHost;
    }

    @Override
    public String toString() {
        return "InstanceDescriptorsResponse{"
                + "requestingHostId=" + requestingHostId
                + ", decommisionRequestingHost=" + decommisionRequestingHost
                + ", instanceDescriptors=" + instanceDescriptors + '}';
    }
}