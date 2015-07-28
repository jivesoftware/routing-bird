package com.jivesoftware.os.routing.bird.shared;

/**
 *
 * @author jonathan.colt
 */
public class ConnectionHealthLatencyStats {

    public double latencyMean;
    public double latencyMin;
    public double latencyMax;
    public double latency50th;
    public double latency75th;
    public double latency90th;
    public double latency95th;
    public double latency99th;
    public double latency999th;

    public ConnectionHealthLatencyStats() {
    }

    public ConnectionHealthLatencyStats(double latencyMean,
        double latencyMin,
        double latencyMax,
        double latency50th,
        double latency75th,
        double latency90th,
        double latency95th,
        double latency99th,
        double latency999th) {
        this.latencyMean = latencyMean;
        this.latencyMin = latencyMin;
        this.latencyMax = latencyMax;
        this.latency50th = latency50th;
        this.latency75th = latency75th;
        this.latency90th = latency90th;
        this.latency95th = latency95th;
        this.latency99th = latency99th;
        this.latency999th = latency999th;
    }

}
