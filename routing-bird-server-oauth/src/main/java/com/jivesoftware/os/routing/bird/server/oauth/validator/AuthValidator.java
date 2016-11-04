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
public interface AuthValidator<V, R> {

    /**
     * @param verifier
     * @param request
     * @return the consumerKey if oauth signature verification was applied and passed, null if it was applied and did not pass
     * @throws AuthValidationException on signature verification not passing for any reason different than the oauth library rejecting it.
     * TODO Interface contract revision: there is no good reason for signature verification failure to be communicated
     * in two different ways. If the exception was used to provide richer context through its message, then this method
     * should return an object indicating success or failure and the useful part of the exception message, instead of
     * the exception propagating the full stack trace that is worthless here.
     * The verification failures that currently throw are really not "exceptional" circumstances.
     */
    AuthValidationResult isValid(V verifier, R request) throws AuthValidationException;

    void clearSecretCache();

    void start();

    void stop();
}
