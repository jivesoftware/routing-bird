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
import com.jivesoftware.os.routing.bird.shared.ReturnFirstNonFailure.Favored;
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
import java.util.concurrent.atomic.LongAdder;
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
    private final long initialSLAMillis;

    public TailAtScaleStrategy(Executor executor, int windowSize, float percentile, long initialSLAMillis) {
        this.executor = executor;
        this.windowSize = windowSize;
        this.percentile = percentile;
        this.initialSLAMillis = initialSLAMillis;
    }


    /*
    Allows a selective bias to be introduce into tail at scale. For example if you wanted to nudge calls onto a different AWS region you could call this
    method with the appropriate connectionDescriptors.
     */
    public void favor(ConnectionDescriptor connectionDescriptor) {

        Map<String, Tail> currentTails = familyTails.get();
        Tail tail = currentTails.get(connectionDescriptor.getInstanceDescriptor().instanceKey);
        if (tail != null) {
            tail.completed(1);
        }
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
        AtomicLong[] clientsDeathTimestamp,
        Favored favored) throws HttpClientException {


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
                        tail = new Tail(oldTail.statistics, percentile, i, initialSLAMillis, Math.random());

                    } else {
                        tail = new Tail(new SynchronizedDescriptiveStatistics(windowSize), percentile, i, initialSLAMillis, Math.random());
                    }
                    newTails.put(instanceKey, tail);
                }

            } else {
                for (int i = 0; i < connectionDescriptors.length; i++) {
                    ConnectionDescriptor connectionDescriptor = connectionDescriptors[i];
                    Tail tail = new Tail(new SynchronizedDescriptiveStatistics(windowSize), percentile, i, initialSLAMillis, Math.random());
                    newTails.put(connectionDescriptor.getInstanceDescriptor().instanceKey, tail);
                }
            }

            familyTails.set(newTails);
        }

        Map<String, Tail> connectionTails = familyTails.get();
        while (connectionTails == null) {
            try {
                Thread.sleep(10);
                connectionTails = familyTails.get();
            } catch (InterruptedException ie) {
                throw new HttpClientException("family tails was null", ie);
            }
        }
        Tail[] tails = connectionTails.values().toArray(new Tail[0]);
        if (tails.length == 0) {
            throw new HttpClientException("No tails");
        }
        Arrays.sort(tails);


        int maxNumberOfClient = Math.min(3, tails.length); // TODO config?
        double percentile = tails[0].statistics.getPercentile(this.percentile);
        long tryAnotherInNMillis = Double.isNaN(percentile) ? 1 : (long) percentile;

        ExecutorCompletionService<Solution<ClientResponse<R>>> executorCompletionService = new ExecutorCompletionService<>(executor);
        List<Future<Solution<ClientResponse<R>>>> futures = new ArrayList<>(maxNumberOfClient);
        try {
            AtomicInteger remaining = new AtomicInteger(0);
            for (int submitted = 0; submitted < maxNumberOfClient; submitted++) {
                int idx = submitted;
                remaining.incrementAndGet();
                futures.add(executorCompletionService.submit(() -> {

                    long now = System.currentTimeMillis();
                    ClientResponse<R> clientResponse = returnFirstNonFailure.indexedCall(null,
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

                    long latency = System.currentTimeMillis() - now;
                    return new Solution<>(clientResponse, tails[idx].index, latency);
                }));

                Solution<ClientResponse<R>> solution = waitForSolution(family, tryAnotherInNMillis, executorCompletionService, remaining);
                if (solution != null && solution.answer != null) {
                    try {
                        if (favored != null) {
                            favored.favored(connectionDescriptors[solution.index], solution.latency);
                        }
                    } catch (Exception x) {
                        LOG.warn("Favored failure.", x);
                    }
                    return solution.answer.response;
                }
            }

            // Everyone is slow so lets drag somebody else into the party
            if (tails.length > maxNumberOfClient) {
                int count = tails.length - maxNumberOfClient;
                int idx = (int) (count * Math.random()) + maxNumberOfClient;
                if (idx < tails.length) {
                    remaining.incrementAndGet();
                    futures.add(executorCompletionService.submit(() -> {

                        long now = System.currentTimeMillis();
                        ClientResponse<R> clientResponse = returnFirstNonFailure.indexedCall(null,
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

                        long latency = System.currentTimeMillis() - now;
                        tails[idx].completed(latency);
                        return new Solution<>(clientResponse, tails[idx].index, latency);
                    }));
                }
            }

            while (remaining.get() > 0) {
                Solution<ClientResponse<R>> solution = waitForSolution(family, -1, executorCompletionService, remaining);
                if (solution != null && solution.answer != null) {
                    try {
                        if (favored != null) {
                            favored.favored(connectionDescriptors[solution.index], solution.latency);
                        }
                    } catch (Exception x) {
                        LOG.warn("Favored failure.", x);
                    }
                    return solution.answer.response;
                }
            }

        } finally {
            for (Future<Solution<ClientResponse<R>>> f : futures) {
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

    private <R> Solution<ClientResponse<R>> waitForSolution(String family,
        long tryAnotherInNMillis,
        ExecutorCompletionService<Solution<ClientResponse<R>>> executorCompletionService,
        AtomicInteger remaining) throws HttpClientException {

        try {
            Future<Solution<ClientResponse<R>>> future = tryAnotherInNMillis < 0
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

    private static class Solution<A> {

        public final A answer;
        public final int index;
        public final long latency;

        private Solution(A answer, int index, long latency) {
            this.answer = answer;
            this.index = index;
            this.latency = latency;
        }
    }

    private static class Tail implements Comparable<Tail> {
        private final DescriptiveStatistics statistics;
        private final float percentile;
        private final int index;
        private final double shuffle;

        private volatile double percentileLatency;
        private LongAdder completed = new LongAdder();

        private Tail(DescriptiveStatistics statistics, float percentile, int index, long initialSLAMillis, double shuffle) {
            this.statistics = statistics;
            this.percentile = percentile;
            this.index = index;
            this.percentileLatency = (double) initialSLAMillis;
            this.shuffle = shuffle;
        }

        public void completed(long latency) {
            statistics.addValue(latency);
            percentileLatency = statistics.getPercentile(percentile);
            completed.increment();
        }

        @Override
        public int compareTo(Tail o) {
            int c = Double.compare(percentileLatency, o.percentileLatency);
            if (c != 0) {
                return c;
            }
            c = Double.compare(shuffle, o.shuffle);
            if (c != 0) {
                return c;
            }
            c = Integer.compare(index, o.index);
            return c;
        }

        @Override
        public String toString() {
            return "Tail{" +
                "statistics=" + statistics +
                ", percentile=" + percentile +
                ", index=" + index +
                ", shuffle=" + shuffle +
                ", percentileLatency=" + percentileLatency +
                ", completed=" + completed +
                '}';
        }
    }

}
