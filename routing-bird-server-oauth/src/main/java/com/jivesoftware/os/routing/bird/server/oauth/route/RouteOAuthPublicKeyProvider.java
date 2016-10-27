package com.jivesoftware.os.routing.bird.server.oauth.route;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.mlogger.core.ValueType;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.server.oauth.AuthRequestException;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthPublicKeyProvider;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthSecretManager.LastSecretRemovalEpochMillis;
import com.jivesoftware.os.routing.bird.shared.OAuthPublicKey;

/**
 * @author jonathan.colt
 */
public class RouteOAuthPublicKeyProvider implements OAuthPublicKeyProvider {

    private final static MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final static String LAST_SUCCESSFUL_ANY_REQUEST = "routeOAuthPublicKeyProvider>lastSuccessfulTSRequest";
    private final static String LAST_SUCCESSFUL_PUBLIC_KEY_REQUEST = "routeOAuthPublicKeyProvider>lastSuccessfulPublicKeyRequest";
    private final static String LAST_SUCCESSFUL_REMOVAL_TIME_REQUEST = "routeOAuthPublicKeyProvider>lastSuccessfulRemovalTimeRequest";

    private final HttpRequestHelper client;
    private final String providerKeyPath;
    private final String providerRemovalsPath;

    public RouteOAuthPublicKeyProvider(HttpRequestHelper client, String providerKeyPath, String providerRemovalsPath) {
        this.client = client;
        this.providerKeyPath = providerKeyPath;
        this.providerRemovalsPath = providerRemovalsPath;
    }

    @Override
    public OAuthPublicKey lookupPublicKey(String id) throws AuthRequestException {
        RouteOAuthPublicKeyRequest request = new RouteOAuthPublicKeyRequest(id);
        RouteOAuthPublicKeyResponse response = client.executeRequest(request, providerKeyPath, RouteOAuthPublicKeyResponse.class, null);

        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_ANY_REQUEST, System.currentTimeMillis());
        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_PUBLIC_KEY_REQUEST, System.currentTimeMillis());

        try {
            if (response == null) {
                LOG.warn("There is no key in provider for id:{}", id);
                return null;
            } else {
                LOG.info("Obtained key from provider for id:{}", id);
                return new OAuthPublicKey(response.getPublicKey(), response.getExpirationEpochMillis());
            }
        } catch (Exception ex) {
            throw new AuthRequestException("Failed to deserialize secret from " + providerKeyPath, ex);
        }
    }

    @Override
    public long removalsTimestampMillis() throws Exception {

        LastSecretRemovalEpochMillis response = client.executeGetRequest(providerRemovalsPath, LastSecretRemovalEpochMillis.class, null);
        if (response == null) {
            throw new AuthRequestException("Failed to check removals:" + providerRemovalsPath);
        }
        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_ANY_REQUEST, System.currentTimeMillis());
        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_REMOVAL_TIME_REQUEST, System.currentTimeMillis());
        return response.lastSecretRemovalEpochMillis;
    }
}
