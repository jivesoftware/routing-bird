package com.jivesoftware.os.routing.bird.authentication;

import com.jivesoftware.os.routing.bird.shared.AuthEvaluator;
import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;

/**
 *
 * @author jonathan.colt
 */
public class NoAuthEvaluator implements AuthEvaluator {

    @Override
    public AuthStatus authorize(ContainerRequestContext requestContext) throws IOException {
        return AuthStatus.authorized;
    }

    @Override
    public String name() {
        return "NoAuthEvaluator";
    }
}
