package com.jivesoftware.os.routing.bird.authentication;

import com.jivesoftware.os.routing.bird.shared.AuthEvaluator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

/**
 *
 */
public class BasicAuthEvaluator implements AuthEvaluator {

    private final Set<String> usernamesAndPasswords;

    public BasicAuthEvaluator(Set<String> usernamesAndPasswords) {
        this.usernamesAndPasswords = usernamesAndPasswords;
    }

    @Override
    public AuthStatus authorize(ContainerRequestContext requestContext) throws IOException {
        String auth = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Basic ")) {
            String usernameAndPassword = new String(Base64.getDecoder().decode(
                auth.substring("Basic ".length())), StandardCharsets.UTF_8);

            if (usernamesAndPasswords.contains(usernameAndPassword)) {
                return AuthStatus.authorized;
            } else {
                return AuthStatus.denied;
            }
        }
        return AuthStatus.not_handled;
    }

    @Override
    public String name() {
        return "BasicAuthEvaluator";
    }
}
