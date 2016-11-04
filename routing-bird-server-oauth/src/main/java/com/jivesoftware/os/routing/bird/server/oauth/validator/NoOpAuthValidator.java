/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.server.oauth.validator;

import com.jivesoftware.os.routing.bird.server.oauth.AuthValidationException;

/**
 *
 */
public class NoOpAuthValidator<V, R> implements AuthValidator<V, R> {

    public static final NoOpAuthValidator<?, ?> SINGLETON = new NoOpAuthValidator<>();

    private NoOpAuthValidator() {
    }

    @Override
    public AuthValidationResult isValid(V verifier, R request) throws AuthValidationException {
        return new AuthValidationResult(null, true);
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
