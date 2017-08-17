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
import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;

/**
 *
 */
public class DryRunOAuthValidator implements AuthValidator<OAuth1Signature, OAuth1Request> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final AuthValidator<OAuth1Signature, OAuth1Request> authValidator;

    public DryRunOAuthValidator(AuthValidator<OAuth1Signature, OAuth1Request> authValidator) {
        this.authValidator = authValidator;
    }

    @Override
    public AuthValidationResult isValid(OAuth1Signature verifier, OAuth1Request request) throws AuthValidationException {
        OAuth1Parameters params = new OAuth1Parameters();
        params.readRequest(request);
        String consumerKey = params.getConsumerKey();
        try {
            AuthValidationResult result = authValidator.isValid(verifier, request);
            if (result.authorized) {
                LOG.info("Dry run validation passed for consumerKey:{}", consumerKey);
                return result;
            } else {
                LOG.info("Dry run validation failed for consumerKey:{}", consumerKey);
                return new AuthValidationResult(consumerKey, true);
            }
        } catch (AuthValidationException x) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dry run validation failed for consumerKey:{}", new Object[]{consumerKey}, x);
            } else {
                LOG.warn("Dry run validation failed for consumerKey:{}", consumerKey);
            }

            return new AuthValidationResult(consumerKey, true);
        }
    }

    @Override
    public void clearSecretCache() {
        authValidator.clearSecretCache();
    }

    @Override
    public void start() {
        authValidator.start();
    }

    @Override
    public void stop() {
        authValidator.stop();
    }
}
