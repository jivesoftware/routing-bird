package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.routing.bird.shared.ClientCall;
import com.jivesoftware.os.routing.bird.shared.ClientHealth;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.HttpClientException;
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import com.jivesoftware.os.routing.bird.shared.ReturnFirstNonFailure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private final AtomicInteger stickyIndex = new AtomicInteger(0);
    private volatile AtomicLong versions = new AtomicLong();
    private final AtomicReference<Map<String, Map<String, Tail>>> familyTails = new AtomicReference<>();
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
        boolean won = true;
        if (v < connectionDescriptorsVersion) {
            while (!versions.compareAndSet(v, connectionDescriptorsVersion)) {
                v = versions.get();
                if (v >= connectionDescriptorsVersion) {
                    won = false;
                    break;
                }
            }
        }

        if (won) {
            Map<String, Map<String, Tail>> newTails = new ConcurrentHashMap<>();
            Map<String, Map<String, Tail>> currentTails = familyTails.get();
            if (currentTails == null) {
                familyTails.set(newTails);
            } else {

                Set<String> retainTheseConnections = new HashSet<>();
                for (ConnectionDescriptor connectionDescriptor : connectionDescriptors) {
                    retainTheseConnections.add(connectionDescriptor.getInstanceDescriptor().instanceKey);
                }

                for (Entry<String, Map<String, Tail>> e : currentTails.entrySet()) {
                    String oldFamily = e.getKey();
                    Map<String, Tail> oldConnectionTails = e.getValue();

                    Map<String, Tail> newConnectionTails = newTails.computeIfAbsent(oldFamily, k -> new ConcurrentHashMap<>());
                    for (int i = 0; i < connectionDescriptors.length; i++) {
                        ConnectionDescriptor connectionDescriptor = connectionDescriptors[i];
                        String instanceKey = connectionDescriptor.getInstanceDescriptor().instanceKey;
                        Tail tail;
                        if (retainTheseConnections.contains(instanceKey)) {
                            Tail oldTail = oldConnectionTails.get(instanceKey);
                            tail = new Tail(oldTail.statistics, windowSize, i);

                        } else {
                            tail = new Tail(new SynchronizedDescriptiveStatistics(windowSize), percentile, i);
                        }
                        newConnectionTails.put(instanceKey, tail);
                    }

                }
            }
        }

        Map<String, Tail> connectionTails = familyTails.get().computeIfAbsent(family, f -> {
            Map<String, Tail> tails = new ConcurrentHashMap<>();
            for (int i = 0; i < connectionDescriptors.length; i++) {
                ConnectionDescriptor connectionDescriptor = connectionDescriptors[i];
                Tail tail = new Tail(new SynchronizedDescriptiveStatistics(windowSize), percentile, i);
                tails.put(connectionDescriptor.getInstanceDescriptor().instanceKey, tail);
            }
            return tails;
        });

        Tail[] tails = connectionTails.values().toArray(new Tail[0]);
        Arrays.sort(tails);


        int maxNumberOfClient = clients.length;
        long tryAnotherInNMillis = (long) tails[0].statistics.getPercentile(percentile);

        ExecutorCompletionService<R> completionService = new ExecutorCompletionService<>(executor);
        List<Future<R>> futures = new ArrayList<>(maxNumberOfClient);
        try {
            for (int submitted = 0; submitted < maxNumberOfClient; submitted++) {

                int i = submitted;
                futures.add(completionService.submit(() -> returnFirstNonFailure._call(null,
                    family,
                    System.currentTimeMillis(),
                    httpCall,
                    tails[i].index,
                    clients,
                    clientHealths,
                    deadAfterNErrors,
                    checkDeadEveryNMillis,
                    clientsErrors,
                    clientsDeathTimestamp)));

                try {
                    Future<R> got = completionService.poll(tryAnotherInNMillis, TimeUnit.MILLISECONDS);
                    if (got != null) {
                        try {
                            return got.get();
                        } catch (ExecutionException ex) {

                        }
                    }
                } catch (InterruptedException x) {
                    throw new HttpClientException("InterruptedException", x);
                }
            }
        } finally {
            for (Future<R> f : futures) {
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


    private static class Tail implements Comparable<Tail> {
        private final DescriptiveStatistics statistics;
        private final float percentile;
        private final int index;

        private Tail(DescriptiveStatistics statistics, float percentile, int index) {
            this.statistics = statistics;
            this.percentile = percentile;
            this.index = index;
        }

        @Override
        public int compareTo(Tail o) {
            int c = Double.compare(statistics.getPercentile(percentile), o.statistics.getPercentile(percentile));
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
