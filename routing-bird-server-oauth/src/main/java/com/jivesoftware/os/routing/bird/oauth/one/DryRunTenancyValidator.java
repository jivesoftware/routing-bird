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
import com.jivesoftware.os.routing.bird.oauth.TenancyValidationException;
import com.jivesoftware.os.routing.bird.oauth.TenancyValidator;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;

/**
 *
 */
public class DryRunTenancyValidator implements TenancyValidator<OAuth1Signature, OAuth1Request> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final TenancyValidator<OAuth1Signature, OAuth1Request> tenancyValidator;

    public DryRunTenancyValidator(TenancyValidator<OAuth1Signature, OAuth1Request> tenancyValidator) {
        this.tenancyValidator = tenancyValidator;
    }

    @Override
    public boolean isValid(String tenantId, OAuth1Signature verifier, OAuth1Request request) throws TenancyValidationException {
        try {
            boolean valid = tenancyValidator.isValid(tenantId, verifier, request);
            if (valid) {
                LOG.info(tenantId + " passed validation.");
            } else {
                LOG.info(tenantId + " failed validation.");
            }
        } catch (Exception x) {
            LOG.warn(tenantId + " failed validation.", x);
        }
        return true;
    }

    @Override
    public void clearSecretCache() {
        tenancyValidator.clearSecretCache();
    }
}
