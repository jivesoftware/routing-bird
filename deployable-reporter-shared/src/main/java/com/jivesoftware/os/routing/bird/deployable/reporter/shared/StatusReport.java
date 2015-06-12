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
package com.jivesoftware.os.routing.bird.deployable.reporter.shared;

import java.util.List;

/**
 * Deliberately not final and not a formal bean because its expected that jackson will be used to map to and from json.
 */
public class StatusReport {

    public String jvmUID;
    public String jvmHome;
    public String jvmName;
    public String jvmVender;
    public String jvmVersion;
    public List<String> jvmHostnames;
    public List<String> jvmIpAddrs;
    public int timestampInSeconds;
    public int startupTimestampInSeconds;
    public float load;
    public double memoryLoad;
    public float percentageOfCPUTimeInGC;

    public long internalErrors = 0;
    public long interactionErrors = 0;

    public StatusReport() {
    }

    public StatusReport(String jvmUID,
        String jvmHome,
        String jvmName,
        String jvmVender,
        String jvmVersion,
        List<String> jvmHostnames,
        List<String> jvmIpAddrs,
        int timestampInSeconds,
        int startupTimestampInSeconds,
        float load,
        double memoryLoad,
        float percentageOfCPUTimeInGC,
        long internalErrors,
        long interactionErrors) {

        this.jvmUID = jvmUID;
        this.jvmHome = jvmHome;
        this.jvmName = jvmName;
        this.jvmVender = jvmVender;
        this.jvmVersion = jvmVersion;
        this.jvmHostnames = jvmHostnames;
        this.jvmIpAddrs = jvmIpAddrs;
        this.timestampInSeconds = timestampInSeconds;
        this.startupTimestampInSeconds = startupTimestampInSeconds;
        this.load = load;
        this.memoryLoad = memoryLoad;
        this.percentageOfCPUTimeInGC = percentageOfCPUTimeInGC;
        this.internalErrors = internalErrors;
        this.interactionErrors = interactionErrors;
    }

}
