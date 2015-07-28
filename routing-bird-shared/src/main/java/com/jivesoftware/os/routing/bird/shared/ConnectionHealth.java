package com.jivesoftware.os.routing.bird.shared;

import com.jivesoftware.os.routing.bird.shared.HostPort;

/**
 *
 * @author jonathan.colt
 */
public class ConnectionHealth {
    public HostPort hostPort;
    public long timestampMillis;
    public long connectivityErrors;
    public long firstMarkedAsDeadTimestampMillis;
    public long lastMarkedAsDeadTimestampMillis;
    public String fatalError;
    public String family;
    public long attempt;
    public long success;
    public long attemptPerSecond;
    public long successPerSecond;
    public ConnectionHealthLatencyStats latencyStats;

    public ConnectionHealth() {
    }

    public ConnectionHealth(HostPort hostPort, long timestampMillis, long connectivityErrors, long firstMarkedAsDeadTimestampMillis,
        long lastMarkedAsDeadTimestampMillis, String fatalError, String family, long attempt, long success, long attemptPerSecond, long successPerSecond,
        ConnectionHealthLatencyStats deliverableLatencyStats) {
        this.hostPort = hostPort;
        this.timestampMillis = timestampMillis;
        this.connectivityErrors = connectivityErrors;
        this.firstMarkedAsDeadTimestampMillis = firstMarkedAsDeadTimestampMillis;
        this.lastMarkedAsDeadTimestampMillis = lastMarkedAsDeadTimestampMillis;
        this.fatalError = fatalError;
        this.family = family;
        this.attempt = attempt;
        this.success = success;
        this.attemptPerSecond = attemptPerSecond;
        this.successPerSecond = successPerSecond;
        this.latencyStats = deliverableLatencyStats;
    }

}
