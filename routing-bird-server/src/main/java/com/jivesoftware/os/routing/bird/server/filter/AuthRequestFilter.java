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
package com.jivesoftware.os.routing.bird.server.filter;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.oauth.AuthValidator;
import com.jivesoftware.os.routing.bird.oauth.AuthValidatorHelper;
import com.jivesoftware.os.routing.bird.oauth.one.ContainerRequestContextOAuth1Request;
import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;

public class AuthRequestFilter implements ContainerRequestFilter {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private static final Response UNAUTHORIZED = Response.status(Status.UNAUTHORIZED).entity("OAuth validation failed").build();

    private final AuthValidator<OAuth1Signature, OAuth1Request> tenancyValidator;
    private final OAuth1Signature verifier;

    public AuthRequestFilter(AuthValidator<OAuth1Signature, OAuth1Request> tenancyValidator, OAuth1Signature verifier) {
        this.tenancyValidator = tenancyValidator;
        this.verifier = verifier;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            if (tenancyValidator != null && verifier != null) {
                ContainerRequestContextOAuth1Request auth1Request = new ContainerRequestContextOAuth1Request(requestContext);
                if (AuthValidatorHelper.isValid(tenancyValidator, verifier, auth1Request, null, UNAUTHORIZED) == UNAUTHORIZED) {
                    requestContext.abortWith(UNAUTHORIZED);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to check authentication", e);
            throw e;
        }
    }
}
