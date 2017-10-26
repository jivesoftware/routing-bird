/*
 * Created: 7/13/12 by brad.jordan
 * Copyright (C) 1999-2012 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.server.oauth;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.OAuthPublicKey;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OAuthPublicKeyProviderSecretManager implements OAuthSecretManager {

    private final static MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String serviceName;
    private final List<OAuthPublicKeyProvider> oAuthProviders;

    private final long secretTimeoutHard;
    private final long secretTimeoutSoft;
    private final long secretCacheMissTimeout;

    private final ConcurrentHashMap<String, TTLPublicKey> idTTLPublicKeys;
    private final AtomicLong lastSecretRemovalEpochMillis = new AtomicLong(0);

    public OAuthPublicKeyProviderSecretManager(String serviceName,
        List<OAuthPublicKeyProvider> oAuthProviders,
        long secretTimeoutHard,
        long secretTimeoutSoft,
        long secretCacheMissTimeout) {
        this.serviceName = serviceName;
        this.oAuthProviders = oAuthProviders;
        this.secretTimeoutHard = secretTimeoutHard;
        this.secretTimeoutSoft = secretTimeoutSoft;
        this.secretCacheMissTimeout = secretCacheMissTimeout;
        this.idTTLPublicKeys = new ConcurrentHashMap<>();
    }

    static class TTLPublicKey {

        private final long secretTimeoutHard;
        private final long secretTimeoutSoft;
        private final AtomicLong expirationHardInMillis;
        private final AtomicLong expirationSoftInMillis;
        private final String secret;

        public TTLPublicKey(final String secret, final long secretTimeoutHard, final long secretTimeoutSoft) {
            this.secretTimeoutHard = secretTimeoutHard;
            this.secretTimeoutSoft = secretTimeoutSoft;
            this.expirationHardInMillis = new AtomicLong(System.currentTimeMillis() + secretTimeoutHard);
            this.expirationSoftInMillis = new AtomicLong(System.currentTimeMillis() + secretTimeoutSoft);
            this.secret = secret;
        }

        public String getSecret() {
            return secret;
        }

        public void resetHardTimer() {
            this.expirationHardInMillis.set(System.currentTimeMillis() + secretTimeoutHard);
        }

        public void resetSoftTimer() {
            this.expirationSoftInMillis.set(System.currentTimeMillis() + secretTimeoutSoft);
        }

        public boolean isHardTimeout() {
            return System.currentTimeMillis() > expirationHardInMillis.get();
        }

        public boolean isSoftTimeout() {
            return System.currentTimeMillis() > expirationSoftInMillis.get();
        }
    }

    @Override
    public String getSecret(String id) throws AuthValidationException {
        TTLPublicKey ttlPublicKey = idTTLPublicKeys.get(id);

        if (ttlPublicKey != null) {
            if (!ttlPublicKey.isSoftTimeout()) {
                return ttlPublicKey.getSecret();
            }
            ttlPublicKey.resetSoftTimer();
            LOG.debug("Soft timer expired on cached secret of tenant:{}, service:{}", id, serviceName);
        }

        LOG.debug("Query oauth provider service/s for id:{}, service:{}", id, serviceName);
        OAuthPublicKey oAuthPublicKey;

        for (OAuthPublicKeyProvider oAuthProvider : oAuthProviders) {
            try {
                oAuthPublicKey = oAuthProvider.lookupPublicKey(id);
                String publicKey = oAuthPublicKey != null ? oAuthPublicKey.key : null;
                if (publicKey != null && !publicKey.isEmpty()) {
                    LOG.debug("Updating tenant:{}, service:{} with new secret", id, serviceName);
                    idTTLPublicKeys.put(id, new TTLPublicKey(publicKey, this.secretTimeoutHard, this.secretTimeoutSoft));
                    return publicKey;
                }
            } catch (AuthRequestException e) {
                LOG.error("Failed to get secret for tenant:{}, service:{}", id, serviceName);
                if (ttlPublicKey != null) {
                    if (!ttlPublicKey.isHardTimeout()) {
                        LOG.debug("Return cached secret of tenant:{}, service:{}", id, serviceName);
                        return ttlPublicKey.getSecret();
                    } else {
                        LOG.debug("Hard timer expired on cached secret of tenant:{}, service:{}", id, serviceName);
                    }
                }
            }
        }

        LOG.debug("Add an empty secret for tenant:{}, service:{}", id, serviceName);
        idTTLPublicKeys.put(id, new TTLPublicKey(null, this.secretTimeoutHard, this.secretCacheMissTimeout));
        return null;
    }

    @Override
    public void verifyLastSecretRemovalTime() throws Exception {
        long updateToLastSecretRemovalEpochMillis = 0;
        for (OAuthPublicKeyProvider oAuthProvider : oAuthProviders) {
            try {
                updateToLastSecretRemovalEpochMillis = Math.max(updateToLastSecretRemovalEpochMillis, oAuthProvider.removalsTimestampMillis());
            } catch (AuthRequestException ex) {
                LOG.error("Failed to get the last secret removal timestamp.", ex);
                return;
            }
        }

        if (lastSecretRemovalEpochMillis.get() <= 0) {
            LOG.debug("Set last secret removal timestamp "
                    + "for serviceName:{} updateToLastSecretRemovalEpochMillis:{} currentLastSecretRemovalEpochMillis:{}",
                serviceName, updateToLastSecretRemovalEpochMillis, lastSecretRemovalEpochMillis.get());
            lastSecretRemovalEpochMillis.set(updateToLastSecretRemovalEpochMillis);
        } else if (updateToLastSecretRemovalEpochMillis > lastSecretRemovalEpochMillis.get()) {
            LOG.debug("Some secret had been removed on the server. Reset last secret removal timestamp "
                    + "for serviceName:{} updateToLastSecretRemovalEpochMillis:{} currentLastSecretRemovalEpochMillis:{}",
                serviceName, updateToLastSecretRemovalEpochMillis, lastSecretRemovalEpochMillis.get());
            lastSecretRemovalEpochMillis.set(updateToLastSecretRemovalEpochMillis);

            // this is needed
            idTTLPublicKeys.clear();
        } else {
            LOG.debug("No secret had been removed on the server. Reset the timers on all secrets "
                    + "for serviceName:{} updateToLastSecretRemovalEpochMillis:{} currentLastSecretRemovalEpochMillis:{}",
                serviceName, updateToLastSecretRemovalEpochMillis, lastSecretRemovalEpochMillis.get());

            // don't have to query for individual secrets since they weren't updated
            for (TTLPublicKey secret : idTTLPublicKeys.values()) {
                if (secret.getSecret() != null) {                 // for null values we want them to be updated
                    secret.resetSoftTimer();
                    secret.resetHardTimer();
                }
            }
        }
    }

    @Override
    public void clearCache() {
        LOG.info("Clearing the cache");
        idTTLPublicKeys.clear();
        lastSecretRemovalEpochMillis.set(0L);
    }

}
