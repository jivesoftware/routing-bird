/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth;

/**
 *
 */
public class NoOpAuthValidator<V, R> implements AuthValidator<V, R> {

    public static final NoOpAuthValidator<?, ?> SINGLETON = new NoOpAuthValidator<>();

    private NoOpAuthValidator() {
    }

    @Override
    public boolean isValid(String id, V verifier, R request) throws AuthValidationException {
        return true;
    }

    @Override
    public void clearSecretCache() {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

}
