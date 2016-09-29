/*
 * Created: 7/13/12 by brad.jordan
 * Copyright (C) 1999-2012 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth.one;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.mlogger.core.ValueType;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.oauth.TenancyRequestException;
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

    private final static MetricLogger log = MetricLoggerFactory.getLogger();
    private final static ObjectMapper jsonMapper = new ObjectMapper();

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

    private final String serviceName;
    private final HttpRequestHelper client;

    private final long secretTimeoutHard;
    private final long secretTimeoutSoft;

    // didn't make it configurable as we'll move to CS 2.0 soon
    private final long secretServiceRequestTimeout = DEFAULT_SECRET_REQUEST_TIMEOUT;
    private final long secretServiceTimestampRequestTimeout = DEFAULT_SECRET_UPDATE_TIME_REQUEST_TIMEOUT;

    // executor service to query tenancy service. Don't have to queue too many requests
    private final ExecutorService executorService = new ThreadPoolExecutor(8, 64, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(128));

    private final ConcurrentHashMap<String, TTLSecret> tenancySecrets;
    private final AtomicLong lastSecretRemovalEpochMillis = new AtomicLong(0);

    public TenancyServiceBackedSecretManager(String serviceName, HttpRequestHelper client) {
        this.serviceName = serviceName;
        this.client = client;
        this.secretTimeoutHard = DEFAULT_SECRET_TIMEOUT_HARD;
        this.secretTimeoutSoft = DEFAULT_SECRET_TIMEOUT_SOFT;
        this.tenancySecrets = new ConcurrentHashMap<String, TTLSecret>();
    }

    public TenancyServiceBackedSecretManager(String serviceName, HttpRequestHelper client, long secretTimeoutHard, long secretTimeoutSoft) {
        this.serviceName = serviceName;
        this.client = client;
        this.secretTimeoutHard = secretTimeoutHard;
        this.secretTimeoutSoft = secretTimeoutSoft;
        this.tenancySecrets = new ConcurrentHashMap<String, TTLSecret>();
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

        // case 1: secret is cached
        if (ttlSecret != null) {
            // case 1-1: cached secret is still valid, which is the most common case so don't have to log it every time
            if (!ttlSecret.isSoftTimeout()) {
                log.debug("Returning secret:{} before soft timer expires for tenant:{}  and service:{}",
                    new Object[]{ttlSecret.getSecret(), tenantId, serviceName});
                return ttlSecret.getSecret();
            }

            // case 1-2: soft timeout, reset the timer and attempt to get the secret from the server below
            ttlSecret.resetSoftTimer();

            // don't output secret normally, for secret reason
            log.info("Soft timer expired on cached secret of tenant:{}, service:{}", new Object[]{tenantId, serviceName});
            log.debug("Soft timer expired on cached secret:{}, tenant:{}, service:{}",
                new Object[]{ttlSecret.getSecret(), tenantId, serviceName});
        }

        log.info("Query tenancy service for tenant:{}, service:{}", new Object[]{tenantId, serviceName});
        JsonOAuthSecret jsonOAuthSecret;

        try {
            jsonOAuthSecret = getSecretFromSecretServiceWithTimeout(tenantId);
        } catch (TenancyRequestException e) {
            // case 1-2-1: failed to get the secret within certain amount of time, cache an empty secret
            log.error("Failed to get secret for tenant:{}, service:{}", new Object[]{tenantId, serviceName});
            if (ttlSecret != null) {
                // case 1-2-1-1: no hard timeout, return the cached secret
                if (!ttlSecret.isHardTimeout()) {
                    log.info("Return cached secret of tenant:{}, service:{}",
                        new Object[]{tenantId, serviceName});
                    log.debug("Return cached secret:{}, tenant:{}, service:{}",
                        new Object[]{ttlSecret.getSecret(), tenantId, serviceName});
                    return ttlSecret.getSecret();
                } else {
                    // case 1-2-1-2: hard timeout, has to fail
                    log.info("Hard timer expired on cached secret of tenant:{}, service:{}",
                        new Object[]{tenantId, serviceName});
                    log.debug("Hard timer expired for secret:{}, tenant:{}, service:{}",
                        new Object[]{ttlSecret.getSecret(), tenantId, serviceName});
                }
            }

            log.info("Add an empty secret for tenant:{}, service:{}", new Object[]{tenantId, serviceName});
            tenancySecrets.put(tenantId, new TTLSecret(null, this.secretTimeoutHard, this.secretTimeoutSoft));
            return null;
        }

        // case 1-2-2: service is up and returned the secret, so update cache and reset hard timeout
        String secret = jsonOAuthSecret != null ? jsonOAuthSecret.getSecret() : null;

        log.info("Updating tenant:{}, service:{} with new secret", new Object[]{tenantId, serviceName});
        log.debug("Updating tenant:{}, service:{} with new secret:{}", new Object[]{tenantId, serviceName, secret});

        tenancySecrets.put(tenantId, new TTLSecret(secret, this.secretTimeoutHard, this.secretTimeoutSoft));
        return secret;
    }

    /**
     * Tenancy service provides an endpoint to get the last timestamp when it removes any secret of any tenant.
     * We query this endpoint every minute, and if there is no update on the timestamp, it can assume all secrets are still valid,
     * so it can reset all soft and hard timers, and doesn't bother to query the actual secrets. This is
     * an additional optimization to the soft/hard timeout approach above.
     */
    public void verifyLastSecretRemovalTime() {
        long updateToLastSecretRemovalEpochMillis;

        try {
            updateToLastSecretRemovalEpochMillis = getLastSecretRemovalTimeWithTimeout();
        } catch (TenancyRequestException ex) {
            // case 1: failed to get the timestamp, don't do anything
            log.error("Failed to get the last secret removal timestamp.", ex);
            return;
        }

        // case 2: the first time get a valid timestamp, set it but do nothing on the cache
        if (lastSecretRemovalEpochMillis.get() <= 0) {
            log.info("Set last secret removal timestamp "
                + "for serviceName:{} updateToLastSecretRemovalEpochMillis:{} currentLastSecretRemovalEpochMillis:{}",
                new Object[]{serviceName, updateToLastSecretRemovalEpochMillis, lastSecretRemovalEpochMillis.get()});
            lastSecretRemovalEpochMillis.set(updateToLastSecretRemovalEpochMillis);
        } // case 3: the timestamp is later than the last time, so, clear the cache to get the secrets again
        else if (updateToLastSecretRemovalEpochMillis > lastSecretRemovalEpochMillis.get()) {
            log.info("Some secret had been removed on the server. Reset last secret removal timestamp "
                + "for serviceName:{} updateToLastSecretRemovalEpochMillis:{} currentLastSecretRemovalEpochMillis:{}",
                new Object[]{serviceName, updateToLastSecretRemovalEpochMillis, lastSecretRemovalEpochMillis.get()});
            lastSecretRemovalEpochMillis.set(updateToLastSecretRemovalEpochMillis);

            // this is necessary!!
            tenancySecrets.clear();
        } // case 4: the timestamp is the same as the last time, so, extend the cache expiration time
        else {
            log.info("No secret had been removed on the server. Reset the timers on all secrets "
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
        log.info("Clear the cache!!!");
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
    private JsonOAuthSecret getSecretFromSecretServiceWithTimeout(String tenantId) throws TenancyRequestException {
        JsonOAuthSecret response;
        String REQUEST_SECRET = "/secret/services/tenantSecret/get?serviceName=" + serviceName + "&tenantId=" + tenantId;

        try {
            response = getResponseWithTimeout(REQUEST_SECRET, DEFAULT_SECRET_REQUEST_TIMEOUT);
        } catch (Exception ex) {
            throw new TenancyRequestException("HttpClientException when sending message to " + REQUEST_SECRET, ex);
        }

        log.set(ValueType.VALUE, LAST_SUCCESSFUL_ANY_REQUEST, System.currentTimeMillis());
        log.set(ValueType.VALUE, LAST_SUCCESSFUL_SECRET_REQUEST, System.currentTimeMillis());

        try {
            if (response == null) {
                log.warn("There is no secret in secret service for tenantId:{} serviceName:{}", tenantId, serviceName);
                return null;
            } else {
                log.info("Obtained from secret service secret for tenantId:{} serviceName:{}", tenantId, serviceName);
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
    private long getLastSecretRemovalTimeWithTimeout() throws TenancyRequestException {
        JsonOAuthSecret response;
        String LAST_SECRET_REMOVAL_EPOCH_MILLIS = "/secret/services/tenantSecret/lastRemovalEpochTimestamp?serviceName=" + serviceName;
        try {
            response = getResponseWithTimeout(LAST_SECRET_REMOVAL_EPOCH_MILLIS, DEFAULT_SECRET_UPDATE_TIME_REQUEST_TIMEOUT);
            if (response == null) {
                throw new TenancyRequestException("Failed to remove secret:" + LAST_SECRET_REMOVAL_EPOCH_MILLIS);
            }
            log.set(ValueType.VALUE, LAST_SUCCESSFUL_ANY_REQUEST, System.currentTimeMillis());
            log.set(ValueType.VALUE, LAST_SUCCESSFUL_REMOVAL_TIME_REQUEST, System.currentTimeMillis());
            return response.getExpirationEpochMillis();
        } catch (Exception e) {
            throw new TenancyRequestException("Exception when requesting " + LAST_SECRET_REMOVAL_EPOCH_MILLIS, e);
        }
    }

    /**
     * Get response from an HTTP endpoint with specified timeout.
     *
     * @param endpoint    HTTP endpoint
     * @param timeout     time out value
     * @return         http response
     * @throws Exception
     */
    private JsonOAuthSecret getResponseWithTimeout(final String endpoint, long timeout) throws Exception {
        Future<JsonOAuthSecret> httpResponseFuture = executorService.submit(() -> client.executeGetRequest(endpoint, JsonOAuthSecret.class, null));
        return httpResponseFuture.get(timeout, TimeUnit.MILLISECONDS);
    }

    // for testing only
    ConcurrentHashMap<String, TTLSecret> getTenancySecrets() {
        return tenancySecrets;
    }
}
