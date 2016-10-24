package com.jivesoftware.os.routing.bird.session;

import javax.ws.rs.container.ContainerRequestContext;

import static org.eclipse.jetty.io.SelectChannelEndPoint.LOG;

/**
 *
 */
public class DryRunSessionValidator implements SessionValidator {

    private final SessionValidator delegate;

    public DryRunSessionValidator(SessionValidator delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isAuthenticated(ContainerRequestContext requestContext) throws SessionValidationException {
        String id = delegate.getId(requestContext);
        try {
            boolean valid = delegate.isAuthenticated(requestContext);
            if (valid) {
                LOG.info("Dry run validation passed for id:{}", id);
            } else {
                LOG.info("Dry run validation failed for id:{}", id);
            }
        } catch (Exception x) {
            LOG.warn("Dry run validation failed for id:{}", new Object[] { id }, x);
        }
        return true;
    }

    @Override
    public String getId(ContainerRequestContext requestContext) {
        return delegate.getId(requestContext);
    }
}
