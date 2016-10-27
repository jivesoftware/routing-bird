package com.jivesoftware.os.routing.bird.server.session;

import com.google.common.collect.Lists;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.AuthEvaluator;
import com.jivesoftware.os.routing.bird.shared.ContainerRequestContextAuthUtil;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *
 */
public class SessionEvaluator implements AuthEvaluator {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final SessionValidator sessionValidator;
    private final List<Pattern> patterns;

    public SessionEvaluator(SessionValidator sessionValidator, String... paths) {
        this.sessionValidator = sessionValidator;
        this.patterns = Lists.newArrayListWithCapacity(paths.length);

        for (String path : paths) {
            patterns.add(Pattern.compile(path));
        }
    }

    @Override
    public AuthStatus authorize(ContainerRequestContext requestContext) throws IOException {
        try {
            if (sessionValidator != null) {
                for (Pattern pattern : patterns) {
                    String path = '/' + requestContext.getUriInfo().getPath();
                    if (pattern.matcher(path).matches()) {
                        if (sessionValidator.isAuthenticated(requestContext)) {
                            return AuthStatus.authorized;
                        } else {
                            return AuthStatus.denied;
                        }
                    }
                }
            }
            return AuthStatus.not_handled;
        } catch (Exception e) {
            LOG.error("Failed to check authentication", e);
            throw new IOException(e);
        }
    }
}
