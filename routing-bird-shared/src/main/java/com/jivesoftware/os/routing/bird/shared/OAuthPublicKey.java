package com.jivesoftware.os.routing.bird.shared;

/**
 *
 * @author jonathan.colt
 */
public class OAuthPublicKey {

    public String key;
    public long expirationEpochMillis;

    public OAuthPublicKey() {
    }

    public OAuthPublicKey(String key, long expirationEpochMillis) {
        this.key = key;
        this.expirationEpochMillis = expirationEpochMillis;
    }

    @Override
    public String toString() {
        return "OAuthPublicKey{" + "key=" + key + ", expirationEpochMillis=" + expirationEpochMillis + '}';
    }

}
