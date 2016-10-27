package com.jivesoftware.os.routing.bird.server.oauth.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.routing.bird.http.client.HttpClient;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfiguration;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.routing.bird.http.client.HttpClientSSLConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthPublicKeyProvider;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthPublicKeyProviderFactory;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthSecretManager;
import com.jivesoftware.os.routing.bird.server.oauth.validator.AuthValidator;
import com.jivesoftware.os.routing.bird.server.oauth.validator.DefaultOAuthValidator;
import com.jivesoftware.os.routing.bird.server.oauth.validator.NoOpAuthValidator;
import java.util.List;
import java.util.concurrent.Executors;
import jersey.repackaged.com.google.common.collect.Lists;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.merlin.config.Config;
import org.merlin.config.defaults.BooleanDefault;
import org.merlin.config.defaults.IntDefault;
import org.merlin.config.defaults.LongDefault;

/**
 *
 */
public class RouteOAuthValidatorInitializer {

    public interface RouteOAuthValidatorConfig extends Config {

        @BooleanDefault(value = true)
        boolean getOauthValidatorIsEnabled();

        @IntDefault(value = 60 * 1000)
        int getOauthValidatorCertAuthoritySocketTimeout();

        // Will expire cached secrets more than 60 seconds old ...
        @LongDefault(value = 60 * 1000)
        long getOauthValidatorCheckForRemovedSecretsEveryNMillis();

        // Will reject requests more than five seconds old ...
        @LongDefault(value = 5 * 1000)
        long getOauthValidatorRequestTimestampAgeLimitMillis();

        @LongDefault(value = 72 * 60 * 60 * 1000)
        long getOauthValidatorSecretTimeoutHardMillis();

        @LongDefault(value = 3 * 60 * 1000)
        long getOauthValidatorSecretTimeoutSoftMillis();

        @LongDefault(value = 30 * 1000)
        long getOauthValidatorSecretCacheMissTimeoutMillis();

    }

    public AuthValidator<OAuth1Signature, OAuth1Request> initialize(RouteOAuthValidatorConfig config,
        String routesHost,
        int routesPort,
        String routesScheme,
        String routesProviderKeyPath,
        String routesProviderRemovalsPath) throws Exception {

        if (!config.getOauthValidatorIsEnabled()) {
            return (AuthValidator) NoOpAuthValidator.SINGLETON;
        }

        OAuthPublicKeyProviderFactory authPublicKeyProviderFactory = (httpRequestHelper) -> {
            return new RouteOAuthPublicKeyProvider(httpRequestHelper, routesProviderKeyPath, routesProviderRemovalsPath);
        };

        List<OAuthPublicKeyProvider> oAuthPublicKeyProviders = Lists.newArrayList();

        List<HttpClientConfiguration> configs = Lists.newArrayList();
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder()
            .setSocketTimeoutInMillis(config.getOauthValidatorCertAuthoritySocketTimeout())
            .build();
        configs.add(httpClientConfig);

        if (routesScheme.toLowerCase().trim().equals("https")) {
            HttpClientSSLConfig sslConfig = HttpClientSSLConfig.newBuilder()
                .setUseSSL(true)
                .build();
            configs.add(sslConfig);
        }

        HttpClientFactory clientFactory = new HttpClientFactoryProvider().createHttpClientFactory(configs, false);
        HttpClient httpClient = clientFactory.createClient(null, routesHost, routesPort);
        HttpRequestHelper client = new HttpRequestHelper(httpClient, new ObjectMapper());

        oAuthPublicKeyProviders.add(authPublicKeyProviderFactory.create(client));

        OAuthSecretManager authSecretManager = new OAuthSecretManager("routes",
            oAuthPublicKeyProviders,
            config.getOauthValidatorSecretTimeoutHardMillis(),
            config.getOauthValidatorSecretTimeoutSoftMillis(),
            config.getOauthValidatorSecretCacheMissTimeoutMillis());

        return new DefaultOAuthValidator(Executors.newScheduledThreadPool(1),
            config.getOauthValidatorCheckForRemovedSecretsEveryNMillis(),
            authSecretManager,
            config.getOauthValidatorRequestTimestampAgeLimitMillis(),
            false);
    }
}
