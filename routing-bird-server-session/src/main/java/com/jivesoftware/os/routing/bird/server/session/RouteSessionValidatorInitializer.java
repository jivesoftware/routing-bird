package com.jivesoftware.os.routing.bird.server.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.routing.bird.http.client.HttpClient;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfiguration;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.routing.bird.http.client.HttpClientSSLConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import java.util.List;
import jersey.repackaged.com.google.common.collect.Lists;
import org.merlin.config.Config;
import org.merlin.config.defaults.BooleanDefault;
import org.merlin.config.defaults.IntDefault;
import org.merlin.config.defaults.LongDefault;

/**
 *
 */
public class RouteSessionValidatorInitializer {

    public interface RouteSessionValidatorConfig extends Config {

        @BooleanDefault(value = true)
        boolean getSessionValidatorIsEnabled();

        @BooleanDefault(value = true)
        boolean getSessionValidatorIsDryRun();

        @IntDefault(value = 60 * 1000)
        int getSessionValidatorCertAuthoritySocketTimeout();

        @LongDefault(value = 10 * 1000)
        long getSessionCacheDurationMillis();

    }

    public SessionValidator initialize(RouteSessionValidatorConfig config,
        String routesHost,
        int routesPort,
        String routesScheme,
        String routesValidatorPath) {

        if (!config.getSessionValidatorIsEnabled()) {
            return NoOpSessionValidator.SINGLETON;
        }

        List<HttpClientConfiguration> configs = Lists.newArrayList();
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder()
            .setSocketTimeoutInMillis(config.getSessionValidatorCertAuthoritySocketTimeout())
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

        SessionValidator sessionValidator = new RouteSessionValidator(client, routesValidatorPath, config.getSessionCacheDurationMillis());

        if (config.getSessionValidatorIsDryRun()) {
            sessionValidator = new DryRunSessionValidator(sessionValidator);
        }

        return sessionValidator;
    }
}
