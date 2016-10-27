package com.jivesoftware.os.routing.bird.server.oauth.route;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.mlogger.core.ValueType;
import com.jivesoftware.os.routing.bird.server.oauth.AuthRequestException;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthPublicKeyProvider;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptors;
import com.jivesoftware.os.routing.bird.shared.OAuthPublicKey;
import com.jivesoftware.os.routing.bird.shared.TenantsServiceConnectionDescriptorProvider;

/**
 * @author jonathan.colt
 */
public class RouteOAuthPublicKeyProvider implements OAuthPublicKeyProvider {

    private final static MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final static String LAST_SUCCESSFUL_ANY_REQUEST = "routeOAuthPublicKeyProvider>lastSuccessfulRequest";
    private final static String LAST_SUCCESSFUL_PUBLIC_KEY_REQUEST = "routeOAuthPublicKeyProvider>lastSuccessfulPublicKeyRequest";

    private final TenantsServiceConnectionDescriptorProvider<String> connectionDescriptorProvider;
    private final long publicKeyExpirationMillis;

    public RouteOAuthPublicKeyProvider(TenantsServiceConnectionDescriptorProvider<String> connectionDescriptorProvider, long publicKeyExpirationMillis) {
        this.connectionDescriptorProvider = connectionDescriptorProvider;
        this.publicKeyExpirationMillis = publicKeyExpirationMillis;
    }

    @Override
    public OAuthPublicKey lookupPublicKey(String id) throws AuthRequestException {
        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_ANY_REQUEST, System.currentTimeMillis());
        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_PUBLIC_KEY_REQUEST, System.currentTimeMillis());

        try {
            ConnectionDescriptors connectionDescriptors = connectionDescriptorProvider.getConnections("");
            for (ConnectionDescriptor connectionDescriptor : connectionDescriptors.getConnectionDescriptors()) {
                if (connectionDescriptor.getInstanceDescriptor().instanceKey.equals(id)) {
                    LOG.info("Obtained key from routes for id:{}", id);
                    return new OAuthPublicKey(connectionDescriptor.getInstanceDescriptor().publicKey, System.currentTimeMillis() + publicKeyExpirationMillis);
                }
            }

            LOG.warn("There is no key in routes for id:{}", id);
            return null;
        } catch (Exception ex) {
            throw new AuthRequestException("Failed to get secret from routes", ex);
        }
    }

    @Override
    public long removalsTimestampMillis() throws Exception {
        return 0;
    }
}
