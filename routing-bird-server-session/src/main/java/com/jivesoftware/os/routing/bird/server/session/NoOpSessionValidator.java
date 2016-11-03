package com.jivesoftware.os.routing.bird.server.session;

import javax.ws.rs.container.ContainerRequestContext;

/**
 *
 */
public class NoOpSessionValidator implements SessionValidator {

    public static final NoOpSessionValidator SINGLETON = new NoOpSessionValidator();

    private NoOpSessionValidator() {
    }

    @Override
    public SessionStatus isAuthenticated(ContainerRequestContext requestContext) throws SessionValidationException {
        return SessionStatus.valid;
    }

    @Override
    public String getId(ContainerRequestContext requestContext) {
        return null;
    }

    @Override
    public boolean exchangeAccessToken(ContainerRequestContext requestContext) {
        return false;
    }

}
