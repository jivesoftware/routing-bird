/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth.one;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.oauth.AuthValidationException;
import com.jivesoftware.os.routing.bird.oauth.AuthValidator;
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
    public boolean isValid(OAuth1Signature verifier, OAuth1Request request) throws AuthValidationException {
        OAuth1Parameters params = new OAuth1Parameters();
        params.readRequest(request);
        String consumerKey = params.getConsumerKey();
        try {
            boolean valid = authValidator.isValid(verifier, request);
            if (valid) {
                LOG.info("Dry run validation passed for {}", consumerKey);
            } else {
                LOG.info("Dry run validation failed for {}", consumerKey);
            }
        } catch (AuthValidationException x) {
            LOG.warn("Dry run validation failed for {}", new Object[] { consumerKey }, x);
        }
        return true;
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
