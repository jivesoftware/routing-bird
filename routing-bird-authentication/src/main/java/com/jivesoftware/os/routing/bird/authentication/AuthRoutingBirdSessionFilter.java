package com.jivesoftware.os.routing.bird.authentication;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

/**
 *
 */
public class AuthRoutingBirdSessionFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        Object rbSessionId = requestContext.getProperty("rb_session_id");
        Object rbSessionToken = requestContext.getProperty("rb_session_token");
        if (rbSessionId == null || rbSessionToken == null) {
            return;
        }
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        headers.add("Set-Cookie", new NewCookie("rb_session_id", rbSessionId.toString()));
        headers.add("Set-Cookie", new NewCookie("rb_session_token", rbSessionToken.toString()));
    }

}
