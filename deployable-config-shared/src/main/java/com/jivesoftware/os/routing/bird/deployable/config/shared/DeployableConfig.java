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
package com.jivesoftware.os.routing.bird.deployable.config.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class DeployableConfig {

    public final String context;
    public final String instanceKey;
    public final String instanceVersion;
    public final Map<String, String> properties;

    @JsonCreator
    public DeployableConfig(@JsonProperty("context") String context,
        @JsonProperty("instanceKey") String instanceKey,
        @JsonProperty("instanceVersion") String instanceVersion,
        @JsonProperty("properties") Map<String, String> properties) {
        this.context = context;
        this.instanceKey = instanceKey;
        this.instanceVersion = instanceVersion;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "DeployableConfig{"
            + "context=" + context
            + ", instanceKey=" + instanceKey
            + ", instanceVersion=" + instanceVersion
            + ", properties=" + properties
            + '}';
    }
}
