package com.jivesoftware.os.routing.bird.session;

import com.google.common.collect.Lists;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
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
public class SessionRequestFilter implements ContainerRequestFilter {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private static final Response UNAUTHORIZED = Response.status(Status.UNAUTHORIZED).entity("Session authentication failed").build();

    private final SessionValidator sessionValidator;
    private final List<Pattern> patterns;

    public SessionRequestFilter(SessionValidator sessionValidator, String... paths) {
        this.sessionValidator = sessionValidator;
        this.patterns = Lists.newArrayListWithCapacity(paths.length);

        for (String path : paths) {
            patterns.add(Pattern.compile(path));
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            if (sessionValidator != null) {
                for (Pattern pattern : patterns) {
                    String path = requestContext.getUriInfo().getPath();
                    if (pattern.matcher(path).matches()) {
                        if (!sessionValidator.isAuthenticated(requestContext)) {
                            requestContext.abortWith(UNAUTHORIZED);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to check authentication", e);
            throw new IOException(e);
        }
    }
}
