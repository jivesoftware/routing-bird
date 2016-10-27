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
    public boolean isAuthenticated(ContainerRequestContext requestContext) throws SessionValidationException {
        return true;
    }

    @Override
    public String getId(ContainerRequestContext requestContext) {
        return null;
    }
}
