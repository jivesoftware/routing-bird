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

        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        for (String name : requestContext.getPropertyNames()) {
            if (name.startsWith("rb_session_token")) {
                Object rbSessionToken = requestContext.getProperty(name);
                headers.add("Set-Cookie", new NewCookie(name, rbSessionToken.toString()));
            }
        }
    }

}
