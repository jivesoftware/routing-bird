/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.server.oauth.validator;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.server.oauth.AuthValidationException;

/**
 *
 */
public class AuthValidatorHelper {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public static <V, R> AuthValidationResult isValid(AuthValidator<V, R> authValidator, V verifier, R request) {
        try {
            AuthValidationResult result = authValidator.isValid(verifier, request);
            if (result != null && result.authorized) {
                LOG.trace("Protocol and OAuth signature verification passed for verifier:{} request:{}",
                    verifier, request);
                return result;
            } else {
                LOG.warn("Protocol signature passed but OAuth signature did not pass for verifier:{} request:{}",
                    verifier, request);
                return result;
            }
        } catch (AuthValidationException ex) {
            LOG.warn("Protocol signature did not pass, OAuth signature not attempted for verifier:{} request:{} protocol error:{}",
                verifier, request, ex.toString());
            return new AuthValidationResult(null, false);
        }
    }

}
