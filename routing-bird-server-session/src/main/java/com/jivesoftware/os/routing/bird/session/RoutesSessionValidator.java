package com.jivesoftware.os.routing.bird.session;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;

/**
 *
 */
public class RoutesSessionValidator implements SessionValidator {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public static final String SESSION_ID = "rb_session_id";
    public static final String SESSION_TOKEN = "rb_session_token";

    private final HttpRequestHelper requestHelper;
    private final String validatorPath;
    private final long sessionCacheDurationMillis;

    private final Map<String, Session> sessions = Maps.newConcurrentMap();

    public RoutesSessionValidator(HttpRequestHelper requestHelper, String validatorPath, long sessionCacheDurationMillis) {
        this.requestHelper = requestHelper;
        this.validatorPath = validatorPath;
        this.sessionCacheDurationMillis = sessionCacheDurationMillis;
    }

    @Override
    public boolean isAuthenticated(ContainerRequestContext requestContext) throws SessionValidationException {
        Cookie sessionIdCookie = requestContext.getCookies().get(SESSION_ID);
        String sessionId = sessionIdCookie == null ? null : sessionIdCookie.getValue();

        Cookie sessionTokenCookie = requestContext.getCookies().get(SESSION_TOKEN);
        String sessionToken = sessionTokenCookie == null ? null : sessionTokenCookie.getValue();

        Boolean result = null;
        if (sessionId != null && sessionToken != null) {
            Session session = sessions.get(sessionToken);
            if (session == null || session.timestamp < System.currentTimeMillis() - sessionCacheDurationMillis) {
                Map<String, String> cookies = Maps.newHashMap();
                cookies.put(SESSION_ID, sessionId);
                cookies.put(SESSION_TOKEN, sessionToken);
                result = requestHelper.executeRequest(cookies, validatorPath, Boolean.class, null);

                if (result == null) {
                    throw new SessionValidationException("Routes failed to respond to validation request for id:" + sessionId);
                } else if (result) {
                    Session got = sessions.compute(sessionToken, (key, value) -> {
                        long timestamp = System.currentTimeMillis();
                        if (value == null || timestamp > value.timestamp) {
                            value = new Session(sessionId, timestamp);
                        }
                        return value;
                    });
                    if (!got.id.equals(sessionId)) {
                        LOG.warn("Invalid session for token: {} != {}", sessionId, got.id);
                        result = false;
                    }
                } else {
                    sessions.remove(sessionToken);
                }
            }
        }

        LOG.info("Session validator for id:{} returned result:{}", result, sessionId);
        return result != null && result;
    }

    @Override
    public String getId(ContainerRequestContext requestContext) {
        Cookie sessionIdCookie = requestContext.getCookies().get(SESSION_ID);
        return sessionIdCookie == null ? null : sessionIdCookie.getValue();
    }

    private static class Session {
        private final String id;
        private final long timestamp;

        private Session(String id, long timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }
    }
}
