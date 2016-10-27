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
public class RouteOAuthPublicKeyRequest {

    private final String id;

    @JsonCreator
    public RouteOAuthPublicKeyRequest(@JsonProperty("id") String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "RouteOAuthPublicKeyRequest{" +
            "id='" + id + '\'' +
            '}';
    }
}