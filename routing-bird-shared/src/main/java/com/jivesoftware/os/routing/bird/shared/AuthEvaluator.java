package com.jivesoftware.os.routing.bird.shared;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;

/**
 *
 */
public interface AuthEvaluator {

    enum AuthStatus {
        expired("Session has expired"),
        authorized("Authorization succeeded"),
        denied("Authorization denied"),
        not_handled("Authorization unavailable");

        private final String description;

        AuthStatus(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    AuthStatus authorize(ContainerRequestContext requestContext) throws IOException;
}
