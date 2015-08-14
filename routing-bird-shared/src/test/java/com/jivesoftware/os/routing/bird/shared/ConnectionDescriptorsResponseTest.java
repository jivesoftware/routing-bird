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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConnectionDescriptorsResponseTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testConstruction() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("a", "b");
        List<ConnectionDescriptor> connections = new ArrayList<>();
        InstanceDescriptor instanceDescriptor = new InstanceDescriptor("ph", "ck", "cn", "sk", "sn", "rgk", "rgn", "ik", 1, "vn", "r", 0, true);
        connections.add(new ConnectionDescriptor(instanceDescriptor, new HostPort("host", 1), properties));
        ConnectionDescriptorsResponse a = new ConnectionDescriptorsResponse(1, Arrays.asList("message"), "user",
            connections);

        Assert.assertEquals(a.getConnections(), connections);
        Assert.assertEquals(a.getMessages(), Arrays.asList("message"));
        Assert.assertEquals(a.getReturnCode(), 1);
        Assert.assertEquals(a.getReleaseGroup(), "user");

        String asString = mapper.writeValueAsString(a);
        ConnectionDescriptorsResponse b = mapper.readValue(asString, ConnectionDescriptorsResponse.class);

        Assert.assertEquals(a.getConnections(), b.getConnections());
        Assert.assertEquals(a.getMessages(), b.getMessages());
        Assert.assertEquals(a.getReturnCode(), b.getReturnCode());
        Assert.assertEquals(a.getReleaseGroup(), b.getReleaseGroup());

        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertEquals(a.toString(), b.toString());

    }
}
