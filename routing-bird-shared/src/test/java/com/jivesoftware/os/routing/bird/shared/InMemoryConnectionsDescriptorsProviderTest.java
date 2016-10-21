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

import java.util.Collections;
import org.testng.Assert;
import org.testng.annotations.Test;

public class InMemoryConnectionsDescriptorsProviderTest {

    @Test
    public void testGetConnections() throws Exception {
        InMemoryConnectionsDescriptorsProvider connectionDescriptorsProvider = new InMemoryConnectionsDescriptorsProvider(null);
        ConnectionDescriptor got = connectionDescriptorsProvider.get(null, null, null, null);
        Assert.assertNull(got);

        got = connectionDescriptorsProvider.get("tenantId", "instanceId", "connectToServiceNamed", "portName");
        Assert.assertNull(got);

        ConnectionDescriptorsRequest requestConnections = new ConnectionDescriptorsRequest("tenantId", "instanceId", "connectToServiceNamed", "portName", null);
        ConnectionDescriptorsResponse response = connectionDescriptorsProvider.requestConnections(requestConnections, null);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getConnections());
        Assert.assertTrue(response.getConnections().isEmpty());
        Assert.assertEquals(response.getReleaseGroup(), "default");

        InstanceDescriptor instanceDescriptor = new InstanceDescriptor("dc", "rk", "ph", "ck", "cn", "sk", "sn", "rgk", "rgn", "ik", 1, "vn", "r", "pk", 0, true);
        ConnectionDescriptor a = new ConnectionDescriptor(instanceDescriptor, false, false, new HostPort("a", 1), Collections.EMPTY_MAP, Collections.EMPTY_MAP);
        connectionDescriptorsProvider.set("tenantId", "instanceId", "connectToServiceNamed", "portName", a);
        got = connectionDescriptorsProvider.get("tenantId", "instanceId", "connectToServiceNamed", "portName");
        Assert.assertEquals(got.getHostPort(), a.getHostPort());
        Assert.assertEquals(got.getInstanceDescriptor(), a.getInstanceDescriptor());
        Assert.assertEquals(got.getProperties(), a.getProperties());

        response = connectionDescriptorsProvider.requestConnections(requestConnections, null);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getConnections());
        Assert.assertTrue(response.getConnections().size() == 1);
        Assert.assertEquals(response.getConnections().get(0).getHostPort(), a.getHostPort());
        Assert.assertEquals(response.getConnections().get(0).getInstanceDescriptor(), a.getInstanceDescriptor());
        Assert.assertEquals(response.getConnections().get(0).getProperties(), a.getProperties());
        Assert.assertEquals(response.getReleaseGroup(), "overridden");

        connectionDescriptorsProvider.clear("tenantId", "instanceId", "connectToServiceNamed", "portName");

        got = connectionDescriptorsProvider.get("tenantId", "instanceId", "connectToServiceNamed", "portName");
        Assert.assertNull(got);
    }

}
