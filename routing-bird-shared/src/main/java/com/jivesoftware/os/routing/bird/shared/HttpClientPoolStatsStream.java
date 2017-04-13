package com.jivesoftware.os.routing.bird.shared;

/**
 * Created by jonathan.colt on 4/13/17.
 */
public interface HttpClientPoolStatsStream {
    boolean poolStats(String name, HttpClientPoolStats poolStats);
}
