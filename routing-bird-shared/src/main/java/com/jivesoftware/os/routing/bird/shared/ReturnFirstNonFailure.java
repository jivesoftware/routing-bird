package com.jivesoftware.os.routing.bird.shared;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ClientCall.ClientResponse;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by jonathan.colt on 3/9/17.
 */
public class ReturnFirstNonFailure {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public <C, R> R call(IndexedClientStrategy strategy,
        String family,
        ClientCall<C, R, HttpClientException> httpCall,
        ConnectionDescriptor[] connectionDescriptors,
        long connectionDescriptorsVersion,
        C[] clients,
        ClientHealth[] clientHealths,
        int deadAfterNErrors,
        long checkDeadEveryNMillis,
        AtomicInteger[] clientsErrors,
        AtomicLong[] clientsDeathTimestamp
    ) throws HttpClientException {

        long now = System.currentTimeMillis();
        int[] clientIndexes = strategy.getClients(connectionDescriptors);
        for (int clientIndex : clientIndexes) {
            if (clientIndex < 0) {
                continue;
            }
            _call(strategy, family, now, httpCall, clientIndex, clients, clientHealths, deadAfterNErrors, checkDeadEveryNMillis, clientsErrors,
                clientsDeathTimestamp);
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

        throw new HttpClientException("No clients are available. possible:" + sb + " filteredIndexes:" + Arrays.toString(clientIndexes));
    }

    public <C, R> R _call(IndexedClientStrategy strategy,
        String family,
        long now,
        ClientCall<C, R, HttpClientException> httpCall,
        int clientIndex,
        C[] clients,
        ClientHealth[] clientHealths,
        int deadAfterNErrors,
        long checkDeadEveryNMillis,
        AtomicInteger[] clientsErrors,
        AtomicLong[] clientsDeathTimestamp) throws HttpClientException {

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
                if (strategy != null) {
                    strategy.usedClientAtIndex(clientIndex);
                }
            }
        } else {
            clientHealths[clientIndex].stillDead();
        }
        return null;
    }
}
