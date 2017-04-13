package com.jivesoftware.os.routing.bird.shared;

/**
 * Created by jonathan.colt on 4/13/17.
 */
public class HttpClientPoolStats {

    public final int leased;
    public final int pending;
    public final int available;
    public final int max;

    public HttpClientPoolStats(int leased, int pending, int available, int max) {
        this.leased = leased;
        this.pending = pending;
        this.available = available;
        this.max = max;
    }
}
