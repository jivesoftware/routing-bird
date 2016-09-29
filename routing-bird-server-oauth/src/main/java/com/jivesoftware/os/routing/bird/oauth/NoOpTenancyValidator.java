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
public class NoOpTenancyValidator<V, R> implements TenancyValidator<V, R> {

    public static final NoOpTenancyValidator<?, ?> SINGLETON = new NoOpTenancyValidator<>();

    private NoOpTenancyValidator() {
    }

    @Override
    public boolean isValid(String tenantId, V verifier, R request) throws TenancyValidationException {
        return true;
    }

    @Override
    public void clearSecretCache() {
    }

}
