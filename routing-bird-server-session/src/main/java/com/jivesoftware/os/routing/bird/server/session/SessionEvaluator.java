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
                if (sessionValidator.isAuthenticated(requestContext) || sessionValidator.exchangeAccessToken(requestContext)) {
                    return AuthStatus.authorized;
                } else {
                    return AuthStatus.denied;
                }
            }
            return AuthStatus.not_handled;
        } catch (Exception e) {
            LOG.error("Failed to check authentication", e);
            throw new IOException(e);
        }
    }
}
