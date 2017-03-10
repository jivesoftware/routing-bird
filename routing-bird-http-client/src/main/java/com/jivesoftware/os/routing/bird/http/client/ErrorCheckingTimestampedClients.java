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
package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ClientCall;
import com.jivesoftware.os.routing.bird.shared.ClientHealth;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.HttpClientException;
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import com.jivesoftware.os.routing.bird.shared.TimestampedClients;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ErrorCheckingTimestampedClients<C> implements TimestampedClients<C, HttpClientException> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String routingGroup;
    private final long timestamp;
    private final ConnectionDescriptor[] connectionDescriptors;
    private final C[] clients;
    private final ClientHealth[] clientHealths;
    private final int deadAfterNErrors;
    private final long checkDeadEveryNMillis;
    private final AtomicInteger[] clientsErrors;
    private final AtomicLong[] clientsDeathTimestamp;

    public ErrorCheckingTimestampedClients(String routingGroup,
        long timestamp,
        ConnectionDescriptor[] connectionDescriptors,
        C[] clients,
        ClientHealth[] clientHealths,
        int deadAfterNErrors,
        long checkDeadEveryNMillis) {

        this.routingGroup = routingGroup;
        this.timestamp = timestamp;
        this.connectionDescriptors = connectionDescriptors;
        this.clients = clients;
        this.clientHealths = clientHealths;
        this.deadAfterNErrors = deadAfterNErrors;
        this.checkDeadEveryNMillis = checkDeadEveryNMillis;
        int l = clients.length;
        this.clientsErrors = new AtomicInteger[l];
        this.clientsDeathTimestamp = new AtomicLong[l];
        for (int i = 0; i < l; i++) {
            clientsErrors[i] = new AtomicInteger(0);
            clientsDeathTimestamp[i] = new AtomicLong(0);
        }
    }

    @Override
    public <R> R call(NextClientStrategy strategy, String family, ClientCall<C, R, HttpClientException> httpCall) throws HttpClientException {

        return strategy.call(family,
            httpCall,
            connectionDescriptors,
            checkDeadEveryNMillis,
            clients,
            clientHealths,
            deadAfterNErrors,
            checkDeadEveryNMillis,
            clientsErrors, clientsDeathTimestamp);
    }


    @Override
    public String getRoutingGroup() {
        return routingGroup;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public C[] getClients() {
        return clients;
    }

    @Override
    public String toString() {
        return "TimestampedClient{"
            + "timestamp=" + timestamp
            + ", clients=" + Arrays.toString(clients)
            + '}';
    }

}
