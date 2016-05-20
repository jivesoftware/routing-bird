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
import com.jivesoftware.os.routing.bird.http.client.ClientHealthProvider.ClientHealth;
import com.jivesoftware.os.routing.bird.shared.ClientCall;
import com.jivesoftware.os.routing.bird.shared.ClientCall.ClientResponse;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import com.jivesoftware.os.routing.bird.shared.TimestampedClients;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import jdk.jfr.events.ThrowablesEvent;

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
        long now = System.currentTimeMillis();
        int[] clientIndexes = strategy.getClients(connectionDescriptors);
        Exception lastException = null;
        for (int clientIndex : clientIndexes) {
            if (clientIndex < 0) {
                continue;
            }
            long deathTimestamp = clientsDeathTimestamp[clientIndex].get();
            if (deathTimestamp == 0 || now - deathTimestamp > checkDeadEveryNMillis) {
                try {
                    LOG.debug("Next index:{} possibleClients:{}", clientIndex, clients.length);
                    clientHealths[clientIndex].attempt(family);
                    long start = System.currentTimeMillis();
                    ClientResponse<R> clientResponse = httpCall.call(clients[clientIndex]);
                    clientHealths[clientIndex].success(family, System.currentTimeMillis() - start);
                    clientsDeathTimestamp[clientIndex].set(0);
                    clientsErrors[clientIndex].set(0);
                    if (clientResponse.responseComplete) {
                        return clientResponse.response;
                    }
                } catch (HttpClientException e) {
                    Throwable cause = e;
                    for (int i = 0; i < 10 && cause != null; i++) {
                        if (cause instanceof InterruptedException || cause instanceof InterruptedIOException || cause instanceof ClosedByInterruptException) {
                            LOG.debug("Client:{} was interrupted for strategy:{} family:{}", new Object[] { clients[clientIndex], strategy, family }, e);
                            throw e;
                        }
                        cause = cause.getCause();
                    }
                    if (e.getCause() instanceof IOException) {
                        lastException = e;
                        int errorCount = clientsErrors[clientIndex].incrementAndGet();
                        if (errorCount > deadAfterNErrors) {
                            LOG.warn("Client:{} has had {} errors and will be marked as dead for {} millis.",
                                new Object[] { clients[clientIndex], errorCount, checkDeadEveryNMillis }, e);
                            clientsDeathTimestamp[clientIndex].set(now + checkDeadEveryNMillis);
                            clientHealths[clientIndex].markedDead();
                        }
                        clientHealths[clientIndex].connectivityError(family);
                    } else {
                        clientHealths[clientIndex].fatalError(family, e);
                        throw e;
                    }
                } catch (Exception e) {
                    clientHealths[clientIndex].fatalError(family, e);
                    throw e;
                } finally {
                    strategy.usedClientAtIndex(clientIndex);
                }
            } else {
                clientHealths[clientIndex].stillDead();
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(strategy.getClass().getSimpleName()).append(" ").append(strategy);
        for (int i = 0; i < connectionDescriptors.length; i++) {
            long deathTimestamp = clientsDeathTimestamp[i].get();
            sb.append(", client[").append(i).append("]={").append(connectionDescriptors[i].getHostPort())
                .append(", isDead:").append((deathTimestamp != 0 && now < deathTimestamp))
                .append(", errors:").append(clientsErrors[i])
                .append(", deathTimestamp:").append(deathTimestamp)
                .append('}');
        }

        throw new HttpClientException("No clients are available. possible:" + sb + " filteredIndexes:" + Arrays.toString(clientIndexes), lastException);
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
