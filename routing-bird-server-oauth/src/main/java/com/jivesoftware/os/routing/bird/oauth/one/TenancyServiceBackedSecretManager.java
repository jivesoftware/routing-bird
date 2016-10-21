/*
 * Created: 7/13/12 by brad.jordan
 * Copyright (C) 1999-2012 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth.one;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.mlogger.core.ValueType;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.oauth.TenancyRequestException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manage tenancy service secrets. https://brewspace.jiveland.com/docs/DOC-189134
 */
public class TenancyServiceBackedSecretManager {

    private final static MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final static String LAST_SUCCESSFUL_ANY_REQUEST = "tenancySecretManager>lastSuccessfulTSRequest";
    private final static String LAST_SUCCESSFUL_SECRET_REQUEST = "tenancySecretManager>lastSuccessfulSecretRequest";
    private final static String LAST_SUCCESSFUL_REMOVAL_TIME_REQUEST = "tenancySecretManager>lastSuccessfulRemovalTimeRequest";

    //Hard timeout" is based on the timestamp of the last SUCCESSFUL request to tenancy service.
    // It is used to make tenancy service validator resilient to tenancy service failure.
    public final static long DEFAULT_SECRET_TIMEOUT_HARD = TimeUnit.HOURS.toMillis(72);

    // "soft timeout" is based on the timestamp of the last request to tenancy service.
    // Whether the request was successful or not does NOT matter. It is used to limit the rate of request to tenancy service.
    public final static long DEFAULT_SECRET_TIMEOUT_SOFT = TimeUnit.MINUTES.toMillis(3);

    public final static long DEFAULT_SECRET_REQUEST_TIMEOUT = 400L;
    public final static long DEFAULT_SECRET_UPDATE_TIME_REQUEST_TIMEOUT = 1000L;
    public final static long DEFAULT_SECRET_CACHE_MISS_TIMEOUT = 30 * 1000L;

    private final String serviceName;
    private final List<HttpRequestHelper> clients;

    private final long secretTimeoutHard;
    private final long secretTimeoutSoft;
    private final long secretCacheMissTimeout;

    // didn't make it configurable as we'll move to CS 2.0 soon
    private final long secretServiceRequestTimeout = DEFAULT_SECRET_REQUEST_TIMEOUT;
    private final long secretServiceTimestampRequestTimeout = DEFAULT_SECRET_UPDATE_TIME_REQUEST_TIMEOUT;

    // executor service to query tenancy service. Don't have to queue too many requests
    private final ExecutorService executorService = new ThreadPoolExecutor(8, 64, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128));

    private final ConcurrentHashMap<String, TTLSecret> tenancySecrets;
    private final AtomicLong lastSecretRemovalEpochMillis = new AtomicLong(0);

    public TenancyServiceBackedSecretManager(String serviceName,
        List<HttpRequestHelper> clients,
        long secretTimeoutHard,
        long secretTimeoutSoft,
        long secretCacheMissTimeout) {
        this.serviceName = serviceName;
        this.clients = clients;
        this.secretTimeoutHard = secretTimeoutHard;
        this.secretTimeoutSoft = secretTimeoutSoft;
        this.secretCacheMissTimeout = secretCacheMissTimeout;
        this.tenancySecrets = new ConcurrentHashMap<>();
    }

    static class TTLSecret {

        private final long secretTimeoutHard;
        private final long secretTimeoutSoft;
        private final AtomicLong expirationHardInMillis;
        private final AtomicLong expirationSoftInMillis;
        private final String secret;

        public TTLSecret(final String secret, final long secretTimeoutHard, final long secretTimeoutSoft) {
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

    /**
     * Cache the secrets for up to a 72 hour window, and update the secret every 3 mins.
     *
     * Having both a hard and soft timer serves two purposes:
     *   1. long hard-timer is more resilient to tenancy outages
     *   2. short soft-timer ensures we don't hammer the secret service on every request
     *
     * @param tenantId tenantId of the jive instance we need to return a secret for
     * @return secret corresponding to this tenant -- nullable
     */
    public String getSecret(String tenantId) {
        TTLSecret ttlSecret = tenancySecrets.get(tenantId);

        if (ttlSecret != null) {
            if (!ttlSecret.isSoftTimeout()) {
                return ttlSecret.getSecret();
            }
            ttlSecret.resetSoftTimer();
            LOG.info("Soft timer expired on cached secret of tenant:{}, service:{}", new Object[]{tenantId, serviceName});
        }

        LOG.info("Query tenancy service for tenant:{}, service:{}", new Object[]{tenantId, serviceName});
        JsonOAuthSecret jsonOAuthSecret;

        for (HttpRequestHelper client : clients) {

            try {
                jsonOAuthSecret = getSecretFromSecretServiceWithTimeout(client, tenantId);
                String secret = jsonOAuthSecret != null ? jsonOAuthSecret.getSecret() : null;
                if (secret != null && !secret.isEmpty()) {
                    LOG.info("Updating tenant:{}, service:{} with new secret", new Object[]{tenantId, serviceName});
                    tenancySecrets.put(tenantId, new TTLSecret(secret, this.secretTimeoutHard, this.secretTimeoutSoft));
                    return secret;
                }
            } catch (TenancyRequestException e) {
                LOG.error("Failed to get secret for tenant:{}, service:{}", new Object[]{tenantId, serviceName});
                if (ttlSecret != null) {
                    if (!ttlSecret.isHardTimeout()) {
                        LOG.info("Return cached secret of tenant:{}, service:{}",new Object[]{tenantId, serviceName});
                        return ttlSecret.getSecret();
                    } else {
                        LOG.info("Hard timer expired on cached secret of tenant:{}, service:{}",new Object[]{tenantId, serviceName});
                    }
                }
            }
        }

        LOG.info("Add an empty secret for tenant:{}, service:{}", new Object[]{tenantId, serviceName});
        tenancySecrets.put(tenantId, new TTLSecret(null, this.secretTimeoutHard, this.secretCacheMissTimeout));
        return null;

    }

    /**
     * Tenancy service provides an endpoint to get the last timestamp when it removes any secret of any tenant.
     * We query this endpoint every minute, and if there is no update on the timestamp, it can assume all secrets are still valid,
     * so it can reset all soft and hard timers, and doesn't bother to query the actual secrets. This is
     * an additional optimization to the soft/hard timeout approach above.
     */
    public void verifyLastSecretRemovalTime() {
        long updateToLastSecretRemovalEpochMillis = 0;
        for (HttpRequestHelper client : clients) {
            try {
                updateToLastSecretRemovalEpochMillis = Math.max(updateToLastSecretRemovalEpochMillis, getLastSecretRemovalTimeWithTimeout(client));
            } catch (TenancyRequestException ex) {
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
            tenancySecrets.clear();
        } else {
            LOG.info("No secret had been removed on the server. Reset the timers on all secrets "
                + "for serviceName:{} updateToLastSecretRemovalEpochMillis:{} currentLastSecretRemovalEpochMillis:{}",
                new Object[]{serviceName, updateToLastSecretRemovalEpochMillis, lastSecretRemovalEpochMillis.get()});

            // don't have to query for individual secrets since they weren't updated
            for (TTLSecret secret : tenancySecrets.values()) {
                if (secret.getSecret() != null) {                 // for null values we want them to be updated
                    secret.resetSoftTimer();
                    secret.resetHardTimer();
                }
            }
        }
    }

    /**
     *  Manually clear the cache and timestamp
     */
    public void clearCache() {
        LOG.info("Clear the cache!!!");
        tenancySecrets.clear();
        lastSecretRemovalEpochMillis.set(0L);
    }

    /**
     *
     * Get secret from tenancy service with timeout.
     *
     * @param tenantId   tenant id
     * @return  -- nullable
     * @throws com.jivesoftware.service.server.tenancy.validator.TenancyRequestException
     */
    private JsonOAuthSecret getSecretFromSecretServiceWithTimeout(HttpRequestHelper client, String tenantId) throws TenancyRequestException {
        JsonOAuthSecret response;
        String REQUEST_SECRET = "/secret/services/tenantSecret/get?serviceName=" + serviceName + "&tenantId=" + tenantId;

        try {
            Future<JsonOAuthSecret> httpResponseFuture = executorService.submit(() -> client.executeGetRequest(REQUEST_SECRET, JsonOAuthSecret.class, null));
            response = httpResponseFuture.get(DEFAULT_SECRET_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            throw new TenancyRequestException("HttpClientException when sending message to " + REQUEST_SECRET, ex);
        }

        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_ANY_REQUEST, System.currentTimeMillis());
        LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_SECRET_REQUEST, System.currentTimeMillis());

        try {
            if (response == null) {
                LOG.warn("There is no secret in secret service for tenantId:{} serviceName:{}", tenantId, serviceName);
                return null;
            } else {
                LOG.info("Obtained from secret service secret for tenantId:{} serviceName:{}", tenantId, serviceName);
                return response;
            }
        } catch (Exception ex) {
            throw new TenancyRequestException("Failed to deserialize secret from " + REQUEST_SECRET, ex);
        }
    }

    /**
     * Get the last removal timestamp from tenancy service.
     *
     * @return  removal timestamp
     * @throws com.jivesoftware.service.server.tenancy.validator.TenancyRequestException
     */
    private long getLastSecretRemovalTimeWithTimeout(HttpRequestHelper client) throws TenancyRequestException {
        LastSecretRemovalEpochMillis response;
        String LAST_SECRET_REMOVAL_EPOCH_MILLIS = "/secret/services/tenantSecret/lastRemovalEpochTimestamp?serviceName=" + serviceName;
        try {

            Future<LastSecretRemovalEpochMillis> httpResponseFuture = executorService.submit(() -> client.executeGetRequest(LAST_SECRET_REMOVAL_EPOCH_MILLIS,
                LastSecretRemovalEpochMillis.class, null));
            response = httpResponseFuture.get(DEFAULT_SECRET_UPDATE_TIME_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);
            if (response == null) {
                throw new TenancyRequestException("Failed to remove secret:" + LAST_SECRET_REMOVAL_EPOCH_MILLIS);
            }
            LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_ANY_REQUEST, System.currentTimeMillis());
            LOG.set(ValueType.VALUE, LAST_SUCCESSFUL_REMOVAL_TIME_REQUEST, System.currentTimeMillis());
            return response.lastSecretRemovalEpochMillis;
        } catch (Exception e) {
            throw new TenancyRequestException("Exception when requesting " + LAST_SECRET_REMOVAL_EPOCH_MILLIS, e);
        }
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
    ConcurrentHashMap<String, TTLSecret> getTenancySecrets() {
        return tenancySecrets;
    }
}
