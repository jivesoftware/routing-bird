package com.jivesoftware.os.routing.bird.shared;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;

/**
 *
 */
public interface AuthEvaluator {

    enum AuthStatus {
        authorized,
        denied,
        not_handled
    }

    AuthStatus authorize(ContainerRequestContext requestContext) throws IOException;
}
