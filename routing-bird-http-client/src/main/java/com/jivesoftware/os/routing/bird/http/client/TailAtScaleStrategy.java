package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ClientCall;
import com.jivesoftware.os.routing.bird.shared.ClientCall.ClientResponse;
import com.jivesoftware.os.routing.bird.shared.ClientHealth;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.HttpClientException;
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import com.jivesoftware.os.routing.bird.shared.ReturnFirstNonFailure;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;

public class TailAtScaleStrategy implements NextClientStrategy {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private volatile AtomicLong versions = new AtomicLong();
    private final AtomicReference<Map<String, Tail>> familyTails = new AtomicReference<>();
    private final ReturnFirstNonFailure returnFirstNonFailure = new ReturnFirstNonFailure();

    private final Executor executor;
    private final int windowSize;
    private final float percentile;

    public TailAtScaleStrategy(Executor executor, int windowSize, float percentile) {
        this.executor = executor;
        this.windowSize = windowSize;
        this.percentile = percentile;
    }

    @Override
    public <C, R> R call(String family,
        ClientCall<C, R, HttpClientException> httpCall,
        ConnectionDescriptor[] connectionDescriptors,
        long connectionDescriptorsVersion,
        C[] clients,
        ClientHealth[] clientHealths,
        int deadAfterNErrors,
        long checkDeadEveryNMillis,
        AtomicInteger[] clientsErrors,
        AtomicLong[] clientsDeathTimestamp) throws HttpClientException {

        long v = versions.get();
        boolean won = false;
        if (v < connectionDescriptorsVersion) {
            won = true;
            while (!versions.compareAndSet(v, connectionDescriptorsVersion)) {
                v = versions.get();
                if (v >= connectionDescriptorsVersion) {
                    won = false;
                    break;
                }
            }
        }

        if (won) {
            Map<String, Tail> newTails = new ConcurrentHashMap<>();
            Map<String, Tail> currentTails = familyTails.get();
            if (currentTails != null) {
                Set<String> retainTheseConnections = new HashSet<>();
                for (ConnectionDescriptor connectionDescriptor : connectionDescriptors) {
                    retainTheseConnections.add(connectionDescriptor.getInstanceDescriptor().instanceKey);
                }

                for (int i = 0; i < connectionDescriptors.length; i++) {
                    ConnectionDescriptor connectionDescriptor = connectionDescriptors[i];
                    String instanceKey = connectionDescriptor.getInstanceDescriptor().instanceKey;
                    Tail tail;
                    if (retainTheseConnections.contains(instanceKey)) {
                        Tail oldTail = currentTails.get(instanceKey);
                        tail = new Tail(oldTail.statistics, percentile, i);

                    } else {
                        tail = new Tail(new SynchronizedDescriptiveStatistics(windowSize), percentile, i);
                    }
                    newTails.put(instanceKey, tail);
                }

            } else {
                for (int i = 0; i < connectionDescriptors.length; i++) {
                    ConnectionDescriptor connectionDescriptor = connectionDescriptors[i];
                    Tail tail = new Tail(new SynchronizedDescriptiveStatistics(windowSize), percentile, i);
                    newTails.put(connectionDescriptor.getInstanceDescriptor().instanceKey, tail);
                }
            }

            familyTails.set(newTails);
        }

        Map<String, Tail> connectionTails = familyTails.get();
        Tail[] tails = connectionTails.values().toArray(new Tail[0]);
        Arrays.sort(tails);

        int maxNumberOfClient = clients.length;
        double percentile = tails[0].statistics.getPercentile(this.percentile);
        long tryAnotherInNMillis = Double.isNaN(percentile) ? 1 : (long) percentile;

        ExecutorCompletionService<ClientResponse<R>> executorCompletionService = new ExecutorCompletionService<>(executor);
        List<Future<ClientResponse<R>>> futures = new ArrayList<>(maxNumberOfClient);
        try {
            AtomicInteger remaining = new AtomicInteger(0);
            for (int submitted = 0; submitted < maxNumberOfClient; submitted++) {
                int idx = submitted;
                remaining.incrementAndGet();
                futures.add(executorCompletionService.submit(() -> {

                    long now = System.currentTimeMillis();
                    ClientResponse<R> clientResponse = returnFirstNonFailure._call(null,
                        family,
                        now,
                        httpCall,
                        tails[idx].index,
                        clients,
                        clientHealths,
                        deadAfterNErrors,
                        checkDeadEveryNMillis,
                        clientsErrors,
                        clientsDeathTimestamp);

                    tails[idx].completed(System.currentTimeMillis() - now);
                    return clientResponse;
                }));

                ClientResponse<R> got = waitForSolution(family, tryAnotherInNMillis, executorCompletionService, remaining);
                if (got != null) {
                    return got.response;
                }
            }

            while (remaining.get() > 0) {
                ClientResponse<R> got = waitForSolution(family, -1, executorCompletionService, remaining);
                if (got != null) {
                    return got.response;
                }
            }

        } finally {
            for (Future<ClientResponse<R>> f : futures) {
                f.cancel(true);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < connectionDescriptors.length; i++) {
            long deathTimestamp = clientsDeathTimestamp[i].get();
            sb.append(", client[").append(i).append("]={").append(connectionDescriptors[i].getHostPort())
                .append(", isDead:").append((deathTimestamp != 0 && System.currentTimeMillis() < deathTimestamp))
                .append(", errors:").append(clientsErrors[i])
                .append(", deathTimestamp:").append(deathTimestamp)
                .append('}');
        }

        throw new HttpClientException("No clients are available. possible:" + sb + " filteredIndexes:" + Arrays.toString(tails));
    }

    private <R> ClientResponse<R> waitForSolution(String family,
        long tryAnotherInNMillis,
        ExecutorCompletionService<ClientResponse<R>> executorCompletionService,
        AtomicInteger remaining) throws HttpClientException {
        try {
            Future<ClientResponse<R>> future = tryAnotherInNMillis < 0
                ? executorCompletionService.take()
                : executorCompletionService.poll(tryAnotherInNMillis, TimeUnit.MILLISECONDS);

            if (future != null) {
                remaining.decrementAndGet();
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    boolean interrupted = false;
                    Throwable cause = e;
                    for (int i = 0; i < 10 && cause != null; i++) {
                        if (cause instanceof InterruptedException
                            || cause instanceof InterruptedIOException
                            || cause instanceof ClosedByInterruptException) {
                            interrupted = true;
                            break;
                        }
                        cause = cause.getCause();
                    }

                    // todo disambiguate stat (i.e. requestName, queryKey)
                    if (interrupted) {
                        LOG.inc("solve>" + family + "request>>solvableInterrupted");
                    } else {
                        LOG.inc("solve>" + family + "request>>solvableError>" + e.getCause().getClass().getSimpleName());
                    }

                    LOG.debug("Solver failed to execute", e.getCause());
                    LOG.incBucket("solve>" + family + "throughput>failure", 1_000L, 100);
                    LOG.incBucket("solve>" + family + "throughput>failure>", 1_000L, 100);
                }
            }
        } catch (InterruptedException x) {
            throw new HttpClientException("InterruptedException", x);
        }
        return null;
    }

    private static class Tail implements Comparable<Tail> {
        private final DescriptiveStatistics statistics;
        private final float percentile;
        private final int index;

        private volatile double percentileLatency;

        private Tail(DescriptiveStatistics statistics, float percentile, int index) {
            this.statistics = statistics;
            this.percentile = percentile;
            this.index = index;
        }

        public void completed(long latency) {
            statistics.addValue(latency);
            percentileLatency = statistics.getPercentile(percentile);
        }

        @Override
        public int compareTo(Tail o) {
            int c = Double.compare(percentileLatency, o.percentileLatency);
            if (c != 0) {
                return c;
            }
            c = Integer.compare(index, o.index);
            return c;
        }

        @Override
        public String toString() {
            return "Tail{" +
                "percentile=" + percentile +
                ", value=" + statistics.getPercentile(percentile) +
                ", index=" + index +
                '}';
        }
    }

}
