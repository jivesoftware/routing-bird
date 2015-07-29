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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ConnectionDescriptorsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testConstruction() throws IOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("a", "b");

        InstanceDescriptor instanceDescriptor1 = new InstanceDescriptor("ck", "cn", "sk", "sn", "rgk", "rgn", "ik", 1, "vn", "r", 0, true);
        InstanceDescriptor instanceDescriptor2 = new InstanceDescriptor("ck", "cn", "sk", "sn", "rgk", "rgn", "ik", 2, "vn", "r", 0, true);

        ConnectionDescriptor cd1 = new ConnectionDescriptor(instanceDescriptor1,new HostPort("host1", 1), properties);
        ConnectionDescriptor cd2 = new ConnectionDescriptor(instanceDescriptor2, new HostPort("host2", 2), properties);

        ConnectionDescriptors a = new ConnectionDescriptors(3, Arrays.asList(cd1, cd2));

        Assert.assertEquals(a.getTimestamp(), 3);
        Assert.assertEquals(a.getConnectionDescriptors(), Arrays.asList(cd1, cd2));

        String asString = mapper.writeValueAsString(a);
        ConnectionDescriptors b = mapper.readValue(asString, ConnectionDescriptors.class);

        Assert.assertEquals(a.getTimestamp(), b.getTimestamp());
        Assert.assertEquals(a.getConnectionDescriptors(), b.getConnectionDescriptors());


        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertEquals(a.toString(), b.toString());
    }
}