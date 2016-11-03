package com.jivesoftware.os.routing.bird.server.session;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.AuthEvaluator;
import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;

/**
 *
 */
public class SessionEvaluator implements AuthEvaluator {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SessionValidator sessionValidator;

    public SessionEvaluator(SessionValidator sessionValidator) {
        this.sessionValidator = sessionValidator;
    }

    @Override
    public AuthStatus authorize(ContainerRequestContext requestContext) throws IOException {
        try {
            if (sessionValidator != null) {
                SessionStatus status = sessionValidator.isAuthenticated(requestContext);
                if (status == SessionStatus.valid || sessionValidator.exchangeAccessToken(requestContext)) {
                    return AuthStatus.authorized;
                } else {
                    return status == SessionStatus.expired ? AuthStatus.expired : AuthStatus.denied;
                }
            }
            return AuthStatus.not_handled;
        } catch (Exception e) {
            LOG.error("Failed to check authentication", e);
            throw new IOException(e);
        }
    }
}
