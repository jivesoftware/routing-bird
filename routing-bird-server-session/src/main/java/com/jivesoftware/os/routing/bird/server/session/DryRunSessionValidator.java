package com.jivesoftware.os.routing.bird.server.session;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import javax.ws.rs.container.ContainerRequestContext;

/**
 *
 */
public class DryRunSessionValidator implements SessionValidator {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

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
