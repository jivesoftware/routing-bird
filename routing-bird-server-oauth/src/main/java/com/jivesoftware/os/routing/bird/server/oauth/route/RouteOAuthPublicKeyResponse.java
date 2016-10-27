/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */

package com.jivesoftware.os.routing.bird.server.oauth.route;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class RouteOAuthPublicKeyResponse {

    public static RouteOAuthPublicKeyResponse NULL = new RouteOAuthPublicKeyResponse("", 0);

    private final String publicKey;
    private final long expirationEpochMillis;

    @JsonCreator
    public RouteOAuthPublicKeyResponse(
        @JsonProperty("publicKey") String publicKey,
        @JsonProperty("expirationEpochMillis") long expirationEpochMillis) {
        this.publicKey = publicKey;
        this.expirationEpochMillis = expirationEpochMillis;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public long getExpirationEpochMillis() {
        return expirationEpochMillis;
    }

    @Override
    public String toString() {
        return "RouteOAuthPublicKeyResponse{" + "publicKey=*************, expirationEpochMillis=" + expirationEpochMillis + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.publicKey != null ? this.publicKey.hashCode() : 0);
        hash = 89 * hash + (int) (this.expirationEpochMillis ^ (this.expirationEpochMillis >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RouteOAuthPublicKeyResponse other = (RouteOAuthPublicKeyResponse) obj;
        if ((this.publicKey == null) ? (other.publicKey != null) : !this.publicKey.equals(other.publicKey)) {
            return false;
        }
        if (this.expirationEpochMillis != other.expirationEpochMillis) {
            return false;
        }
        return true;
    }

}