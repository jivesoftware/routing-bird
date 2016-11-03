package com.jivesoftware.os.routing.bird.server.session;

import javax.ws.rs.container.ContainerRequestContext;

/**
 *
 */
public interface SessionValidator {

    SessionStatus isAuthenticated(ContainerRequestContext requestContext) throws SessionValidationException;

    String getId(ContainerRequestContext requestContext);

    boolean exchangeAccessToken(ContainerRequestContext requestContext);
}
