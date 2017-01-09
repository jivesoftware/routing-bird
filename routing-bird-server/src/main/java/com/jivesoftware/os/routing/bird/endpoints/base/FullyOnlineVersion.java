package com.jivesoftware.os.routing.bird.endpoints.base;

/**
 * Created by jonathan.colt on 1/9/17.
 */
public interface FullyOnlineVersion {
    /**
     *
     * @return null if not fully online else return version of fully online service
     */
    String getFullyOnlineVersion();
}
