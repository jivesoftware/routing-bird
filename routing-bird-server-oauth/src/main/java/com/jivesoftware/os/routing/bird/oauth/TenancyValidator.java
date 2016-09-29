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
public interface TenancyValidator<V, R> {

    /**
     * @param tenantId
     * @param context
     * @return true if oauth signature verification was applied and passed, false if it was applied and did not pass
     * @throws TenancyValidationException on signature verification not passing for any reason different than the oauth library rejecting it.
     * TODO Interface contract revision: there is no good reason for signature verification failure to be communicated
     * in two different ways. If the exception was used to provide richer context through its message, then this method
     * should return an object indicating success or failure and the useful part of the exception message, instead of
     * the exception propagating the full stack trace that is worthless here.
     * The verification failures that currently throw are really not "exceptional" circumstances.
     */
    boolean isValid(String tenantId, V verifier, R request) throws TenancyValidationException;

    void clearSecretCache();
}
