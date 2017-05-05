package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.routing.bird.shared.ClientCall.ClientResponse;
import com.jivesoftware.os.routing.bird.shared.ClientHealth;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TailAtScaleStrategyTest {


    @Test
    public void testTas() throws Exception {
        TailAtScaleStrategy tas = new TailAtScaleStrategy(Executors.newCachedThreadPool(), 20, 95f, 0);

        ConnectionDescriptor[] connectionDescriptors = new ConnectionDescriptor[3];
        Integer[] clients = new Integer[3];
        ClientHealth[] clientHealths = new ClientHealth[3];
        int deadAfterNErrors = 1;
        long checkDeadEveryNMillis = 10_000;
        AtomicInteger[] clientsErrors = new AtomicInteger[3];
        AtomicLong[] clientsDeathTimestamp = new AtomicLong[3];


        for (int i = 0; i < 3; i++) {
            clients[i] = i;
            connectionDescriptors[i] = new ConnectionDescriptor(
                new InstanceDescriptor("dc", "rk", "ph", "ck", "cn", "sk", "sn", "rgk", "rgn", "ik-" + i, 1, "vn", "r", "pk", 0, true),
                false, false,
                new HostPort("test", i + 1),
                Collections.EMPTY_MAP, Collections.EMPTY_MAP);


            clientHealths[i] = new ClientHealth() {
                @Override
                public void attempt(String family) {
                }

                @Override
                public void success(String family, long latencyMillis) {
                }

                @Override
                public void markedDead() {
                }

                @Override
                public void connectivityError(String family) {
                }

                @Override
                public void fatalError(String family, Exception x) {
                }

                @Override
                public void stillDead() {
                }

                @Override
                public void interrupted(String family, Exception e) {
                }


            };
            clientsErrors[i] = new AtomicInteger();
            clientsDeathTimestamp[i] = new AtomicLong();

        }


        int[] called = new int[3];
        for (int i = 0; i < 20; i++) {
            int run = i;
            Integer got = tas.call("test",
                client -> {
                    try {
                        Integer c = (Integer) client;
                        long sleep = (long) (Math.random() * 100 * c);
                        Thread.sleep(sleep);
                    } catch (InterruptedException x) {
                        // SWALLOW
                    }
                    return new ClientResponse<Integer>(client, true);
                },
                connectionDescriptors,
                1,
                clients,
                clientHealths,
                deadAfterNErrors,
                checkDeadEveryNMillis,
                clientsErrors,
                clientsDeathTimestamp,
                null
            );
            System.out.print(got + " ");
            called[got]++;
        }


        System.out.println(Arrays.toString(called));
        Assert.assertTrue(called[0] > called[2]);

        called = new int[3];
        for (int i = 0; i < 20; i++) {
            int run = i;
            Integer got = tas.call("test",
                client -> {
                    try {
                        Integer c = (Integer) client;
                        long sleep = (long) (Math.random() * 100 * (2 - c));
                        Thread.sleep(sleep);
                    } catch (InterruptedException x) {
                        // SWALLOW
                    }
                    return new ClientResponse<Integer>(client, true);
                },
                connectionDescriptors,
                1,
                clients,
                clientHealths,
                deadAfterNErrors,
                checkDeadEveryNMillis,
                clientsErrors,
                clientsDeathTimestamp,
                null
            );
            System.out.print(got + " ");
            called[got]++;
        }

        System.out.println(Arrays.toString(called));
        Assert.assertTrue(called[2] > called[0]);

        called = new int[3];
        for (int i = 0; i < 20; i++) {
            int run = i;
            Integer got = tas.call("test",
                client -> {
                    try {
                        Integer c = (Integer) client;
                        long sleep = c == 1 ? 0 : (long) (Math.random() * 100);
                        Thread.sleep(sleep);
                    } catch (InterruptedException x) {
                        // SWALLOW
                    }
                    return new ClientResponse<Integer>(client, true);
                },
                connectionDescriptors,
                1,
                clients,
                clientHealths,
                deadAfterNErrors,
                checkDeadEveryNMillis,
                clientsErrors,
                clientsDeathTimestamp,
                null
            );
            System.out.print(got + " ");
            called[got]++;
        }

        System.out.println(Arrays.toString(called));
        Assert.assertTrue(called[1] > called[0]);
        Assert.assertTrue(called[1] > called[2]);
    }


}
