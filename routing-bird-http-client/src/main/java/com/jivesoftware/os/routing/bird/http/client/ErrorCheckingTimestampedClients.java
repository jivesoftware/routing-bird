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
import com.jivesoftware.os.routing.bird.shared.ClientCall.ClientResponse;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import com.jivesoftware.os.routing.bird.shared.TimestampedClients;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ErrorCheckingTimestampedClients<C> implements TimestampedClients<C, HttpClientException> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final long timestamp;
    private final ConnectionDescriptor[] connectionDescriptors;
    private final C[] clients;
    private final int deadAfterNErrors;
    private final long checkDeadEveryNMillis;
    private final AtomicInteger[] clientsErrors;
    private final AtomicLong[] clientsDeathTimestamp;

    public ErrorCheckingTimestampedClients(long timestamp,
        ConnectionDescriptor[] connectionDescriptors,
        C[] clients,
        int deadAfterNErrors,
        long checkDeadEveryNMillis) {
        this.timestamp = timestamp;
        this.connectionDescriptors = connectionDescriptors;
        this.clients = clients;
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
    public <R> R call(NextClientStrategy strategy, ClientCall<C, R, HttpClientException> httpCall) throws HttpClientException {
        long now = System.currentTimeMillis();
        int[] clientIndexes = strategy.getClients(connectionDescriptors);
        for (int clientIndex : clientIndexes) {
            if (clientIndex < 0) {
                continue;
            }
            long deathTimestamp = clientsDeathTimestamp[clientIndex].get();
            if (deathTimestamp == 0 || now - deathTimestamp > checkDeadEveryNMillis) {
                try {
                    LOG.debug("Next index:{} possibleClients:{}", clientIndex, clients.length);
                    ClientResponse<R> clientResponse = httpCall.call(clients[clientIndex]);
                    clientsDeathTimestamp[clientIndex].set(0);
                    clientsErrors[clientIndex].set(0);
                    if (clientResponse.responseComplete) {
                        return clientResponse.response;
                    }
                } catch (HttpClientException e) {
                    if (e.getCause() instanceof IOException) {
                        if (clientsErrors[clientIndex].incrementAndGet() > deadAfterNErrors) {
                            clientsDeathTimestamp[clientIndex].set(now + checkDeadEveryNMillis);
                        }
                    } else {
                        throw e;
                    }
                } finally {
                    strategy.usedClientAtIndex(clientIndex);
                }
            }
        }
        throw new HttpClientException("No clients are available");
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
        return "TimestampedClient{" +
            "timestamp=" + timestamp +
            ", clients=" + Arrays.toString(clients) +
            '}';
    }

}
