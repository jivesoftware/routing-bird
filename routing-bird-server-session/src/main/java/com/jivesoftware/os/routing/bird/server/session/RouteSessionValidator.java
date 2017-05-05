package com.jivesoftware.os.routing.bird.server.session;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.http.client.NonSuccessStatusCodeException;

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
    public static final String SESSION_REDIR = "rb_session_redir_url";

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
    public SessionStatus isAuthenticated(ContainerRequestContext requestContext) throws SessionValidationException {
        String sessionTokenKey = SESSION_TOKEN + "_" + instanceKey;
        Cookie sessionTokenCookie = requestContext.getCookies().get(sessionTokenKey);
        String sessionToken = sessionTokenCookie == null ? null : sessionTokenCookie.getValue();

        Boolean result;
        if (sessionToken != null) {
            Session session = sessions.get(sessionToken);
            if (session == null || session.timestamp < System.currentTimeMillis() - sessionCacheDurationMillis) {
                Map<String, String> requestParams = Maps.newHashMap();
                requestParams.put(SESSION_ID, instanceKey);
                requestParams.put(SESSION_TOKEN, sessionToken);
                result = requestHelper.executeRequest(requestParams, validatorPath, Boolean.class, null);
                if (result == null) {
                    throw new SessionValidationException("Routes failed to respond to validation request for id:" + instanceKey);
                } else if (result) {
                    Session got = sessions.compute(sessionToken, (key, value) -> {
                        long timestamp = System.currentTimeMillis();
                        if (value == null || timestamp > value.timestamp) {
                            value = new Session(instanceKey, timestamp);
                        }
                        return value;
                    });
                    if (!got.id.equals(instanceKey)) {
                        LOG.warn("Invalid session for token: {} != {}", instanceKey, got.id);
                        return SessionStatus.invalid;
                    }
                } else {
                    sessions.remove(sessionToken);
                    return SessionStatus.expired;
                }
            }

            Cookie redirCookie = requestContext.getCookies().get(SESSION_REDIR);
            if (redirCookie != null) {
                requestContext.getHeaders().putSingle(SESSION_REDIR, redirCookie.getValue());
            }

            return SessionStatus.valid;
        }
        return SessionStatus.invalid;
    }

    @Override
    public String getId(ContainerRequestContext requestContext) {
        Cookie sessionIdCookie = requestContext.getCookies().get(SESSION_ID);
        return sessionIdCookie == null ? null : sessionIdCookie.getValue();
    }

    @Override
    public boolean exchangeAccessToken(ContainerRequestContext requestContext) {
        List<String> accessToken = requestContext.getUriInfo().getQueryParameters().get("rb_access_token");
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                byte[] sessionToken = requestHelper.executeGet(exchangePath + "/" + instanceKey + "/" + accessToken.get(0));
                if (sessionToken != null) {
                    requestContext.setProperty("rb_session_token_" + instanceKey, new String(sessionToken, StandardCharsets.UTF_8));

                    List<String> redirUrl = requestContext.getUriInfo().getQueryParameters().get("rb_access_redir_url");
                    if (redirUrl != null && !redirUrl.isEmpty()) {
                        requestContext.setProperty(SESSION_REDIR, redirUrl.get(0));
                        requestContext.getHeaders().putSingle(SESSION_REDIR, redirUrl.get(0));
                    }

                    return true;
                }
            } catch (NonSuccessStatusCodeException e) {
                LOG.warn("access token rejected.", e);
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
