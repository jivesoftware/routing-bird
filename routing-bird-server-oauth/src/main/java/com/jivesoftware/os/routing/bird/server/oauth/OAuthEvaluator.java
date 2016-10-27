/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.routing.bird.server.oauth;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.server.oauth.validator.AuthValidator;
import com.jivesoftware.os.routing.bird.server.oauth.validator.AuthValidatorHelper;
import com.jivesoftware.os.routing.bird.shared.AuthEvaluator;
import javax.ws.rs.container.ContainerRequestContext;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.glassfish.jersey.server.oauth1.internal.OAuthServerRequest;

public class OAuthEvaluator implements AuthEvaluator {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final AuthValidator<OAuth1Signature, OAuth1Request> authValidator;
    private final OAuth1Signature verifier;

    public OAuthEvaluator(AuthValidator<OAuth1Signature, OAuth1Request> authValidator, OAuth1Signature verifier) {
        this.authValidator = authValidator;
        this.verifier = verifier;
    }

    @Override
    public AuthStatus authorize(ContainerRequestContext requestContext) {
        try {
            if (authValidator != null && verifier != null) {
                OAuthServerRequest serverRequest = new OAuthServerRequest(requestContext);
                if (AuthValidatorHelper.isValid(authValidator, verifier, serverRequest, Boolean.TRUE, Boolean.FALSE) == Boolean.TRUE) {
                    return AuthStatus.authorized;
                } else {
                    return AuthStatus.denied;
                }
            }
            return AuthStatus.not_handled;
        } catch (Exception e) {
            LOG.error("Failed to check authentication", e);
            throw e;
        }
    }
}
