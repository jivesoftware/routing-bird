package com.jivesoftware.os.routing.bird.session;

import javax.ws.rs.container.ContainerRequestContext;

/**
 *
 */
public interface SessionValidator {

    boolean isAuthenticated(ContainerRequestContext requestContext) throws SessionValidationException;

    String getId(ContainerRequestContext requestContext);
}
