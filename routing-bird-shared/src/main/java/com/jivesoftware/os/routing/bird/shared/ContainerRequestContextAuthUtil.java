package com.jivesoftware.os.routing.bird.shared;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

/**
 *
 */
public class ContainerRequestContextAuthUtil {

    private static final String AUTHENTICATED = "rb_authenticated";
    private static final String AUTHENTICATED_BY = "rb_authenticated_by";

    private static final String DENIED = "rb_denied";
    private static final String DENIED_BY = "rb_denied_by";

    private ContainerRequestContextAuthUtil() {}

    public static void setAuthenticated(ContainerRequestContext requestContext, ContainerRequestFilter filter) {
        requestContext.setProperty(ContainerRequestContextAuthUtil.AUTHENTICATED, true);
        requestContext.setProperty(ContainerRequestContextAuthUtil.AUTHENTICATED_BY, filter);
    }

    public static boolean isAuthenticated(ContainerRequestContext requestContext) {
        Boolean hasAuth = (Boolean) requestContext.getProperty(AUTHENTICATED);
        return (hasAuth != null && hasAuth);
    }

    public static void setDenied(ContainerRequestContext requestContext, ContainerRequestFilter filter) {
        requestContext.setProperty(ContainerRequestContextAuthUtil.DENIED, true);
        requestContext.setProperty(ContainerRequestContextAuthUtil.DENIED_BY, filter);
    }

    public static boolean isDenied(ContainerRequestContext requestContext) {
        Boolean hasDeny = (Boolean) requestContext.getProperty(DENIED);
        return (hasDeny != null && hasDeny);
    }
}
