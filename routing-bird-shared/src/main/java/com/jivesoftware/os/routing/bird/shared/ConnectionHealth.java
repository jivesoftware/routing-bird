package com.jivesoftware.os.routing.bird.shared;

/**
 * @author jonathan.colt
 */
public class ConnectionHealth {
    public ConnectionDescriptor connectionDescriptor;
    public long timestampMillis;
    public long connectivityErrors;
    public long firstMarkedAsDeadTimestampMillis;
    public long lastMarkedAsDeadTimestampMillis;
    public String fatalError;
    public String family;
    public long attempt;
    public long success;
    public long failure;
    public long interrupted;
    public long successPerSecond;
    public long failurePerSecond;
    public long interruptedPerSecond;
    public ConnectionHealthLatencyStats latencyStats;

    public ConnectionHealth() {
    }

    public ConnectionHealth(ConnectionDescriptor connectionDescriptor,
        long timestampMillis,
        long connectivityErrors,
        long firstMarkedAsDeadTimestampMillis,
        long lastMarkedAsDeadTimestampMillis,
        String fatalError,
        String family,
        long attempt,
        long success,
        long failure,
        long interrupted,
        long successPerSecond,
        long failurePerSecond,
        long interruptedPerSecond,
        ConnectionHealthLatencyStats deliverableLatencyStats) {
        this.connectionDescriptor = connectionDescriptor;
        this.timestampMillis = timestampMillis;
        this.connectivityErrors = connectivityErrors;
        this.firstMarkedAsDeadTimestampMillis = firstMarkedAsDeadTimestampMillis;
        this.lastMarkedAsDeadTimestampMillis = lastMarkedAsDeadTimestampMillis;
        this.fatalError = fatalError;
        this.family = family;
        this.attempt = attempt;
        this.success = success;
        this.failure = failure;
        this.interrupted = interrupted;
        this.successPerSecond = successPerSecond;
        this.failurePerSecond = failurePerSecond;
        this.interruptedPerSecond = interruptedPerSecond;
        this.latencyStats = deliverableLatencyStats;
    }

}
