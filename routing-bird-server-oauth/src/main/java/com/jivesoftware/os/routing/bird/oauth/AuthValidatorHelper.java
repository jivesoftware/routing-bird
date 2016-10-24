/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;

/**
 *
 */
public class AuthValidatorHelper<V, R, B> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();


    /*
     * This method does not propagate any AuthValidationException to the caller so that it can be returned in the
     * http response. This means that in cases where a customer fails security checks, they will not be able to know why
     * and customer support has to be involved to scan the service logs for the messages from this method.
     * Passing to the client details for security failures leaks information about the security resource blocking
     * authentication, which is discouraged because it leakw information (resource and code stack traces) to attackers
     * and discloses the security protocol that requires NDA-release.
     * DO NOT CHANGE THIS BEHAVIOR unless cleared with Information Security Director, David Cook.
     */
    public static <V, R, B> B isValid(AuthValidator<V, R> authValidator, V verifier, R request, B success, B failure) {
        try {
            if (authValidator.isValid(verifier, request)) {
                LOG.trace("Jive protocol and OAuth signature verification passed for verifier:{} request:{}",
                    verifier, request);
                return success;
            } else {
                LOG.warn("Jive protocol signature passed but OAuth signature did not pass for verifier:{} request:{}",
                    verifier, request);
                return failure;
            }
        } catch (AuthValidationException ex) {
            LOG.warn("Jive protocol signature did not pass, OAuth signature not attempted for verifier:{} request:{} protocol error:{}",
                new Object[]{verifier, request, ex.toString()});
            return failure;
        }
    }

}
