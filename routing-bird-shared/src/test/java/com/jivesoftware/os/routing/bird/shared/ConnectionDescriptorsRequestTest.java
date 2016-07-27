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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConnectionDescriptorsRequestTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testConstruction() throws IOException {

        ConnectionDescriptorsRequest a = new ConnectionDescriptorsRequest("tenant", "instance", "to", "port", null);
        Assert.assertEquals(a.getTenantId(), "tenant");
        Assert.assertEquals(a.getInstanceId(), "instance");
        Assert.assertEquals(a.getConnectToServiceNamed(), "to");
        Assert.assertEquals(a.getPortName(), "port");

        String asString = mapper.writeValueAsString(a);
        ConnectionDescriptorsRequest b = mapper.readValue(asString, ConnectionDescriptorsRequest.class);

        Assert.assertEquals(a.getTenantId(), b.getTenantId());
        Assert.assertEquals(a.getInstanceId(), b.getInstanceId());
        Assert.assertEquals(a.getConnectToServiceNamed(), b.getConnectToServiceNamed());
        Assert.assertEquals(a.getPortName(), b.getPortName());

        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertEquals(a.toString(), b.toString());

    }
}