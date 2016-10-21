/*
 * Created: 7/13/12 by brad.jordan
 * Copyright (C) 1999-2012 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth.one;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.oauth.AuthRequestException;
import com.jivesoftware.os.routing.bird.oauth.AuthValidationException;
import com.jivesoftware.os.routing.bird.oauth.OAuthPublicKeyProvider;
import com.jivesoftware.os.routing.bird.shared.OAuthPublicKey;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class OAuthSecretManager {

    private final static MetricLogger LOG = MetricLoggerFactory.getLogger();

    //Hard timeout" is based on the timestamp of the last SUCCESSFUL request to public key provider service.
    // It is used to make public key provider service validator resilient to public key provider service failure.
    public final static long DEFAULT_SECRET_TIMEOUT_HARD = TimeUnit.HOURS.toMillis(72);

    // "soft timeout" is based on the timestamp of the last request to public key provider service.
    // Whether the request was successful or not does NOT matter. It is used to limit the rate of request to public key provider service.
    public final static long DEFAULT_SECRET_TIMEOUT_SOFT = TimeUnit.MINUTES.toMillis(3);

    public final static long DEFAULT_PUBLIC_KEY_REQUEST_TIMEOUT = 400L;
    public final static long DEFAULT_SECRET_UPDATE_TIME_REQUEST_TIMEOUT = 1000L;
    public final static long DEFAULT_SECRET_CACHE_MISS_TIMEOUT = 30 * 1000L;

    private final String serviceName;
    private final List<OAuthPublicKeyProvider> oAuthProviders;

    private final long secretTimeoutHard;
    private final long secretTimeoutSoft;
    private final long secretCacheMissTimeout;

    private final long secretServiceRequestTimeout = DEFAULT_PUBLIC_KEY_REQUEST_TIMEOUT;
    private final long secretServiceTimestampRequestTimeout = DEFAULT_SECRET_UPDATE_TIME_REQUEST_TIMEOUT;

    private final ExecutorService executorService = new ThreadPoolExecutor(8, 64, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128));

    private final ConcurrentHashMap<String, TTLPublicKey> idTTLPublicKeys;
    private final AtomicLong lastSecretRemovalEpochMillis = new AtomicLong(0);

    public OAuthSecretManager(String serviceName,
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

        // for testing only
        public void setHardTimerExpirationTime(long timestamp) {
            this.expirationHardInMillis.set(timestamp);
        }

        // for testing only
        public void setSoftTimerExpirationTime(long timestamp) {
            this.expirationSoftInMillis.set(timestamp);
        }

        public boolean isHardTimeout() {
            return System.currentTimeMillis() > expirationHardInMillis.get();
        }

        public boolean isSoftTimeout() {
            return System.currentTimeMillis() > expirationSoftInMillis.get();
        }
    }

  
    public String getSecret(String id) throws AuthValidationException {
        TTLPublicKey ttlPublicKey = idTTLPublicKeys.get(id);

        if (ttlPublicKey != null) {
            if (!ttlPublicKey.isSoftTimeout()) {
                return ttlPublicKey.getSecret();
            }
            ttlPublicKey.resetSoftTimer();
            LOG.info("Soft timer expired on cached secret of tenant:{}, service:{}", new Object[]{id, serviceName});
        }

        LOG.info("Query oauth provider service/s for id:{}, service:{}", new Object[]{id, serviceName});
        OAuthPublicKey oAuthPublicKey;

        for (OAuthPublicKeyProvider oAuthProvider : oAuthProviders) {

            try {
                oAuthPublicKey = oAuthProvider.lookupPublicKey(id);
                String publicKey = oAuthPublicKey != null ? oAuthPublicKey.key : null;
                if (publicKey != null && !publicKey.isEmpty()) {
                    LOG.info("Updating tenant:{}, service:{} with new secret", new Object[]{id, serviceName});
                    idTTLPublicKeys.put(id, new TTLPublicKey(publicKey, this.secretTimeoutHard, this.secretTimeoutSoft));
                    return publicKey;
                }
            } catch (AuthRequestException e) {
                LOG.error("Failed to get secret for tenant:{}, service:{}", new Object[]{id, serviceName});
                if (ttlPublicKey != null) {
                    if (!ttlPublicKey.isHardTimeout()) {
                        LOG.info("Return cached secret of tenant:{}, service:{}",new Object[]{id, serviceName});
                        return ttlPublicKey.getSecret();
                    } else {
                        LOG.info("Hard timer expired on cached secret of tenant:{}, service:{}",new Object[]{id, serviceName});
                    }
                }
            }
        }

        LOG.info("Add an empty secret for tenant:{}, service:{}", new Object[]{id, serviceName});
        idTTLPublicKeys.put(id, new TTLPublicKey(null, this.secretTimeoutHard, this.secretCacheMissTimeout));
        return null;

    }

   
    public void verifyLastSecretRemovalTime() throws Exception {
        long updateToLastSecretRemovalEpochMillis = 0;
        for (OAuthPublicKeyProvider oAuthProvider : oAuthProviders) {
            try {
                updateToLastSecretRemovalEpochMillis = Math.max(updateToLastSecretRemovalEpochMillis, oAuthProvider.removalsTimestampMillis());
            } catch (AuthRequestException ex) {
                // case 1: failed to get the timestamp, don't do anything
                LOG.error("Failed to get the last secret removal timestamp.", ex);
                return;
            }
        }

        if (lastSecretRemovalEpochMillis.get() <= 0) {
            LOG.info("Set last secret removal timestamp "
                + "for serviceName:{} updateToLastSecretRemovalEpochMillis:{} currentLastSecretRemovalEpochMillis:{}",
                new Object[]{serviceName, updateToLastSecretRemovalEpochMillis, lastSecretRemovalEpochMillis.get()});
            lastSecretRemovalEpochMillis.set(updateToLastSecretRemovalEpochMillis);
        } else if (updateToLastSecretRemovalEpochMillis > lastSecretRemovalEpochMillis.get()) {
            LOG.info("Some secret had been removed on the server. Reset last secret removal timestamp "
                + "for serviceName:{} updateToLastSecretRemovalEpochMillis:{} currentLastSecretRemovalEpochMillis:{}",
                new Object[]{serviceName, updateToLastSecretRemovalEpochMillis, lastSecretRemovalEpochMillis.get()});
            lastSecretRemovalEpochMillis.set(updateToLastSecretRemovalEpochMillis);

            // this is necessary!!
            idTTLPublicKeys.clear();
        } else {
            LOG.info("No secret had been removed on the server. Reset the timers on all secrets "
                + "for serviceName:{} updateToLastSecretRemovalEpochMillis:{} currentLastSecretRemovalEpochMillis:{}",
                new Object[]{serviceName, updateToLastSecretRemovalEpochMillis, lastSecretRemovalEpochMillis.get()});

            // don't have to query for individual secrets since they weren't updated
            for (TTLPublicKey secret : idTTLPublicKeys.values()) {
                if (secret.getSecret() != null) {                 // for null values we want them to be updated
                    secret.resetSoftTimer();
                    secret.resetHardTimer();
                }
            }
        }
    }

    public void clearCache() {
        LOG.info("Clear the cache!!!");
        idTTLPublicKeys.clear();
        lastSecretRemovalEpochMillis.set(0L);
    }

  
  
    public static class LastSecretRemovalEpochMillis {

        public long lastSecretRemovalEpochMillis;

        public LastSecretRemovalEpochMillis() {
        }

        public LastSecretRemovalEpochMillis(long lastSecretRemovalEpochMillis) {
            this.lastSecretRemovalEpochMillis = lastSecretRemovalEpochMillis;
        }
    }

    // for testing only
    ConcurrentHashMap<String, TTLPublicKey> getIdTTLPublicKeys() {
        return idTTLPublicKeys;
    }
}
