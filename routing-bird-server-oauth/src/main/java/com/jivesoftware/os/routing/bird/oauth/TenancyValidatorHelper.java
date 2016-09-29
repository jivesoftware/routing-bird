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
public class TenancyValidatorHelper<V, R, B> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();


    /*
     * This method does not propagate any TenancyValidationException to the caller so that it can be returned in the
     * http response. This means that in cases where a customer fails security checks, they will not be able to know why
     * and customer support has to be involved to scan the service logs for the messages from this method.
     * Passing to the client details for security failures leaks information about the security resource blocking
     * authentication, which is discouraged because it leakw information (resource and code stack traces) to attackers
     * and discloses the security protocol that requires NDA-release.
     * DO NOT CHANGE THIS BEHAVIOR unless cleared with Information Security Director, David Cook.
     */
    public static <V, R, B> B isValid(TenancyValidator<V, R> tenancyValidator, String tenancy, V verifier, R request, B success, B failure) {
        try {
            if (tenancyValidator.isValid(tenancy, verifier, request)) {
                LOG.trace("Jive protocol and OAuth signature verification passed for tenancy:{} verifier:{} request:{}",
                    tenancy, verifier, request);
                return success;
            } else {
                LOG.warn("Jive protocol signature passed but OAuth signature did not pass for tenancy:{} verifier:{} request:{}",
                    tenancy, verifier, request);
                return failure;
            }
        } catch (TenancyValidationException ex) {
            LOG.warn("Jive protocol signature did not pass, OAuth signature not attempted for tenancy:{} verifier:{} request:{} protocol error:{}",
                new Object[]{tenancy, verifier, request, ex.toString()});
            return failure;
        }
    }

}
