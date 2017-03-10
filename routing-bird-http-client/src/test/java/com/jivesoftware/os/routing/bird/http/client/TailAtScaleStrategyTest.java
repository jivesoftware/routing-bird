package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import java.util.Collections;
import java.util.concurrent.Executors;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TailAtScaleStrategyTest {

    private ConnectionDescriptor[] connectionDescriptors = null;
    private NextClientStrategy tas = null;

    @BeforeClass
    public void before() throws Exception {
        connectionDescriptors = new ConnectionDescriptor[1];
        connectionDescriptors[0] = new ConnectionDescriptor(
            new InstanceDescriptor("dc", "rk", "ph", "ck", "cn", "sk", "sn", "rgk", "rgn", "ik", 1, "vn", "r", "pk", 0, true),
            false, false,
            new HostPort("foobar", 12345),
            Collections.EMPTY_MAP, Collections.EMPTY_MAP);

        tas = new TailAtScaleStrategy(Executors.newCachedThreadPool(), 100, 95f);
    }

    @Test
    public void testGetClient() throws Exception {
        //int[] a = tas.getClients(connectionDescriptors);
        //Assert.assertTrue(1 == a.length);
    }

    @Test
    public void testUsedClientAtIndex() throws Exception {
        //tas.usedClientAtIndex(0);
        //Assert.assertTrue(true);
    }

}
