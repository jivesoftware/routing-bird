package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class TailAtScaleStrategy implements NextClientStrategy {

    private final AtomicInteger lastIndexUsed = new AtomicInteger(0);

    @Override
    public int[] getClients(ConnectionDescriptor[] connectionDescriptors) {

        int[] res = new int[1];
        Arrays.fill(res, -1);
        return res;

    }

    @Override
    public void usedClientAtIndex(int index) {
        lastIndexUsed.set(index);
    }

}
