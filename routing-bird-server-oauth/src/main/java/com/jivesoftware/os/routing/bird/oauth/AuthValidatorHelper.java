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
public class AuthValidatorHelper {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public static <V, R, B> B isValid(AuthValidator<V, R> authValidator, V verifier, R request, B success, B failure) {
        try {
            if (authValidator.isValid(verifier, request)) {
                LOG.trace("Protocol and OAuth signature verification passed for verifier:{} request:{}",
                    verifier, request);
                return success;
            } else {
                LOG.warn("Protocol signature passed but OAuth signature did not pass for verifier:{} request:{}",
                    verifier, request);
                return failure;
            }
        } catch (AuthValidationException ex) {
            LOG.warn("Protocol signature did not pass, OAuth signature not attempted for verifier:{} request:{} protocol error:{}",
                verifier, request, ex.toString());
            return failure;
        }
    }

}
