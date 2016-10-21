/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.routing.bird.oauth.one;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.routing.bird.http.client.HttpClient;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfiguration;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.routing.bird.http.client.HttpClientSSLConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.oauth.NoOpTenancyValidator;
import com.jivesoftware.os.routing.bird.oauth.TenancyValidator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jersey.repackaged.com.google.common.collect.Lists;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.merlin.config.Config;
import org.merlin.config.defaults.BooleanDefault;
import org.merlin.config.defaults.IntDefault;
import org.merlin.config.defaults.LongDefault;
import org.merlin.config.defaults.StringDefault;

/**
 *
 */
public class TenancyServiceBackedOAuthTenancyValidatorInitializer {

    public interface TenancyServiceBackedOAuthTenancyValidatorConfig extends Config {

        @StringDefault("pleaseSetToCorrect-ServiceName")
        String getOauthEndpointsAsServiceName();

        @BooleanDefault(true)
        boolean getOauthValidatorIsEnabled();

        @BooleanDefault(true)
        boolean getOauthValidatorIsDryRun();

        @StringDefault("")
        String getOauthValidatorCertAuthoritySchemeHostPortTuples();

        @StringDefault("pleaseSetToCorrect-CertAuthorityScheme")
        String getOauthValidatorCertAuthorityScheme();

        @StringDefault("pleaseSetToCorrect-CertAuthorityHost")
        String getOauthValidatorCertAuthorityHost();

        @IntDefault(-1)
        int getOauthValidatorCertAuthorityPort();

        @IntDefault(60 * 1000)
        int getOauthValidatorCertAuthoritySocketTimeout();

        // Will expire cached secrets more than 60 seconds old ...
        @LongDefault(60 * 1000)
        long getOauthValidatorCheckForRemovedSecretsEveryNMillis();

        // Will reject requests more than five seconds old ...
        @LongDefault(5 * 1000)
        long getOauthValidatorRequestTimestampAgeLimitMillis();

        // Load balancer rejiggering is enabled by default ...
        @BooleanDefault(true)
        boolean getOauthValidatorLoadBalancerRejiggeringIdEnabled();

        @LongDefault(72 * 60 * 60 * 1000)
        long getOauthValidatorSecretTimeoutHardMillis();

        @LongDefault(3 * 60 * 1000)
        long getOauthValidatorSecretTimeoutSoftMillis();

        @LongDefault(30 * 1000)
        long getOauthValidatorSecretCacheMissTimeoutMillis();

    }

    private ScheduledExecutorService newScheduledThreadPool;

    public TenancyServiceBackedOAuthTenancyValidatorInitializer() {
        this.newScheduledThreadPool = Executors.newScheduledThreadPool(1);
    }

    public TenancyValidator<OAuth1Signature, OAuth1Request> initialize(TenancyServiceBackedOAuthTenancyValidatorConfig config) throws Exception {

        if (!config.getOauthValidatorIsEnabled()) {
            return (TenancyValidator) NoOpTenancyValidator.SINGLETON;
        }

        long secretTimeoutHard = config.getOauthValidatorSecretTimeoutHardMillis();
        long secretTimeoutSoft = config.getOauthValidatorSecretTimeoutSoftMillis();
        long secretCacheMissTimeout = config.getOauthValidatorSecretCacheMissTimeoutMillis();

        String oauthValidatorCertAuthorityHostPortTuples = config.getOauthValidatorCertAuthoritySchemeHostPortTuples();

        List<HttpRequestHelper> clients = Lists.newArrayList();
        if (oauthValidatorCertAuthorityHostPortTuples != null && !oauthValidatorCertAuthorityHostPortTuples.trim().isEmpty()) {
            String[] schemeHostPorts = oauthValidatorCertAuthorityHostPortTuples.trim().split("\\s*,\\s*");

            for (String schemeHostPort : schemeHostPorts) {
                String[] parts = schemeHostPort.split(":");
                String scheme = parts[0];
                String host = parts[1];
                int port = Integer.parseInt(parts[2]);

                List<HttpClientConfiguration> configs = Lists.newArrayList();
                HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder()
                    .setSocketTimeoutInMillis(config.getOauthValidatorCertAuthoritySocketTimeout())
                    .build();
                configs.add(httpClientConfig);

                if (scheme.equals("https")) {
                    HttpClientSSLConfig sslConfig = HttpClientSSLConfig.newBuilder()
                        .setUseSSL(true)
                        .build();
                    configs.add(sslConfig);
                }
                HttpClientFactory clientFactory = new HttpClientFactoryProvider().createHttpClientFactory(configs, false);

                HttpClient httpClient = clientFactory.createClient(null, host, port);
                HttpRequestHelper client = new HttpRequestHelper(httpClient, new ObjectMapper());
                clients.add(client);
            }
        } else {
            List<HttpClientConfiguration> configs = Lists.newArrayList();
            HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder()
                .setSocketTimeoutInMillis(config.getOauthValidatorCertAuthoritySocketTimeout())
                .build();
            configs.add(httpClientConfig);

            if (config.getOauthValidatorCertAuthorityScheme().toLowerCase().trim().equals("https")) {
                HttpClientSSLConfig sslConfig = HttpClientSSLConfig.newBuilder()
                    .setUseSSL(true)
                    .build();
                configs.add(sslConfig);
            }

            HttpClientFactory clientFactory = new HttpClientFactoryProvider().createHttpClientFactory(configs, false);
            HttpClient httpClient = clientFactory.createClient(null, config.getOauthValidatorCertAuthorityHost(),
                config.getOauthValidatorCertAuthorityPort());
            HttpRequestHelper client = new HttpRequestHelper(httpClient, new ObjectMapper());

            clients.add(client);
        }

        TenancyServiceBackedSecretManager secretManager = new TenancyServiceBackedSecretManager(config.getOauthEndpointsAsServiceName(),
            clients,
            secretTimeoutHard,
            secretTimeoutSoft,
            secretCacheMissTimeout);

        TenancyServiceBackedOAuthTenancyValidator validator = new TenancyServiceBackedOAuthTenancyValidator(
            secretManager,
            config.getOauthValidatorRequestTimestampAgeLimitMillis(),
            config.getOauthValidatorLoadBalancerRejiggeringIdEnabled()
        );

        newScheduledThreadPool.scheduleWithFixedDelay(new ExpireSecretsCache(validator),
            config.getOauthValidatorCheckForRemovedSecretsEveryNMillis(),
            config.getOauthValidatorCheckForRemovedSecretsEveryNMillis(),
            TimeUnit.MILLISECONDS);

        if (config.getOauthValidatorIsDryRun()) {
            return new DryRunTenancyValidator(validator);
        }

        return validator;
    }

    synchronized public void shutdown() {
        if (newScheduledThreadPool != null) {
            newScheduledThreadPool.shutdownNow();
        }
        newScheduledThreadPool = null;
    }

    private static class ExpireSecretsCache implements Runnable {

        private final TenancyServiceBackedOAuthTenancyValidator validator;

        public ExpireSecretsCache(TenancyServiceBackedOAuthTenancyValidator validator) {
            this.validator = validator;
        }

        @Override
        public void run() {
            try {
                validator.expireSecretCacheIfNecessary();
            } catch (Exception x) {
                // oh well we tried
            }
        }
    }
}
