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
    public SessionStatus isAuthenticated(ContainerRequestContext requestContext) throws SessionValidationException {
        String id = delegate.getId(requestContext);
        try {
            SessionStatus status = delegate.isAuthenticated(requestContext);
            if (SessionStatus.valid == status) {
                LOG.info("Dry run validation passed for id:{}", id);
            } else {
                LOG.info("Dry run validation failed for id:{}", id);
            }
        } catch (Exception x) {
            LOG.warn("Dry run validation failed for id:{}", new Object[] { id }, x);
        }
        return SessionStatus.valid;
    }

    @Override
    public String getId(ContainerRequestContext requestContext) {
        return delegate.getId(requestContext);
    }

    @Override
    public boolean exchangeAccessToken(ContainerRequestContext requestContext) {
        return delegate.exchangeAccessToken(requestContext);
    }

}
