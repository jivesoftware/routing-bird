/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */

package com.jivesoftware.os.routing.bird.oauth.one;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class TenancyJsonOAuthSecret {
    
    public static TenancyJsonOAuthSecret NULL = new TenancyJsonOAuthSecret("", 0);
    
    private final String secret;
    private final long expirationEpochMillis;
   
    @JsonCreator
    public TenancyJsonOAuthSecret(
            @JsonProperty("secret") String secret,
            @JsonProperty("expirationEpochMillis") long expirationEpochMillis
            ) {
        this.secret = secret;
        this.expirationEpochMillis = expirationEpochMillis;
    }

    public String getSecret() {
        return secret;
    }
    
    public long getExpirationEpochMillis() {
        return expirationEpochMillis;
    }

    @Override
    public String toString() {
        return "JsonOAuthSecret{" + "secret=*************, expirationEpochMillis=" + expirationEpochMillis + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.secret != null ? this.secret.hashCode() : 0);
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
        final TenancyJsonOAuthSecret other = (TenancyJsonOAuthSecret) obj;
        if ((this.secret == null) ? (other.secret != null) : !this.secret.equals(other.secret)) {
            return false;
        }
        if (this.expirationEpochMillis != other.expirationEpochMillis) {
            return false;
        }
        return true;
    }
    
}