package com.jivesoftware.os.routing.bird.server.session;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;

/**
 *
 */
public class RouteSessionValidator implements SessionValidator {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public static final String SESSION_ID = "rb_session_id";
    public static final String SESSION_TOKEN = "rb_session_token";

    private final String instanceKey;
    private final HttpRequestHelper requestHelper;
    private final String validatorPath;
    private final String exchangePath;
    private final long sessionCacheDurationMillis;

    private final Map<String, Session> sessions = Maps.newConcurrentMap();

    public RouteSessionValidator(String instanceKey,
        HttpRequestHelper requestHelper,
        String validatorPath,
        String exchangePath,
        long sessionCacheDurationMillis) {

        this.instanceKey = instanceKey;
        this.requestHelper = requestHelper;
        this.validatorPath = validatorPath;
        this.exchangePath = exchangePath;
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

    @Override
    public boolean exchangeAccessToken(ContainerRequestContext requestContext) {
        String sessionId = (String) requestContext.getProperty("rb_session_id");
        if (sessionId == null) {
            List<String> accessToken = requestContext.getUriInfo().getQueryParameters().get("rb_access_token");
            if (accessToken != null && !accessToken.isEmpty()) {
                byte[] sessionToken = requestHelper.executeGet(exchangePath + "/" + instanceKey + "/" + accessToken.get(0));
                if (sessionToken != null) {
                    requestContext.setProperty("rb_session_id", instanceKey);
                    requestContext.setProperty("rb_session_token", new String(sessionToken, StandardCharsets.UTF_8));
                    return true;
                }
            }
        }
        return false;
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
