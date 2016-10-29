package com.jivesoftware.os.routing.bird.server.oauth.route;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.mlogger.core.ValueType;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.server.oauth.AuthRequestException;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthPublicKeyProvider;
import com.jivesoftware.os.routing.bird.shared.OAuthPublicKey;

/**
 * @author jonathan.colt
 */
public class RouteOAuthPublicKeyProvider implements OAuthPublicKeyProvider {

    private final static MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final static String LAST_SUCCESSFUL_ANY_REQUEST = "routeOAuthPublicKeyProvider>lastSuccessfulRequest";
    private final static String LAST_SUCCESSFUL_PUBLIC_KEY_REQUEST = "routeOAuthPublicKeyProvider>lastSuccessfulPublicKeyRequest";

    private final HttpRequestHelper requestHelper;
    private final String routesValidatorPath;
    private final long publicKeyExpirationMillis;

    public RouteOAuthPublicKeyProvider(HttpRequestHelper requestHelper, String routesValidatorPath, long publicKeyExpirationMillis) {
        this.requestHelper = requestHelper;
        this.routesValidatorPath = routesValidatorPath;
        this.publicKeyExpirationMillis = publicKeyExpirationMillis;
    }

    @Override
    public OAuthPublicKey lookupPublicKey(String id) throws AuthRequestException {
        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_ANY_REQUEST, System.currentTimeMillis());
        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_PUBLIC_KEY_REQUEST, System.currentTimeMillis());

        try {
            String publicKey = requestHelper.executeGetRequest(routesValidatorPath + '/' + id, String.class, null);
            if (publicKey != null) {
                return new OAuthPublicKey(publicKey, System.currentTimeMillis() + publicKeyExpirationMillis);
            }

            LOG.warn("Failed to get public key from authority for id:{}", id);
            return null;
        } catch (Exception ex) {
            throw new AuthRequestException("Failed to get public key from authority", ex);
        }
    }

    @Override
    public long removalsTimestampMillis() throws Exception {
        return 0;
    }
}
