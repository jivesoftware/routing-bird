/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.routing.bird.deployable;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jivesoftware.os.mlogger.core.LoggerSummary;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.authentication.AuthRoutingBirdSessionFilter;
import com.jivesoftware.os.routing.bird.authentication.AuthValidationFilter;
import com.jivesoftware.os.routing.bird.authentication.BasicAuthEvaluator;
import com.jivesoftware.os.routing.bird.authentication.NoAuthEvaluator;
import com.jivesoftware.os.routing.bird.deployable.config.extractor.ConfigBinder;
import com.jivesoftware.os.routing.bird.endpoints.configuration.MainProperties;
import com.jivesoftware.os.routing.bird.endpoints.configuration.MainPropertiesEndpoints;
import com.jivesoftware.os.routing.bird.health.HealthCheck;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponseImpl;
import com.jivesoftware.os.routing.bird.health.api.ResettableHealthCheck;
import com.jivesoftware.os.routing.bird.health.api.ScheduledHealthCheck;
import com.jivesoftware.os.routing.bird.health.checkers.PercentileHealthChecker;
import com.jivesoftware.os.routing.bird.http.client.HttpClient;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.routing.bird.http.client.HttpResponse;
import com.jivesoftware.os.routing.bird.http.client.OAuthSigner;
import com.jivesoftware.os.routing.bird.http.client.OAuthSignerProvider;
import com.jivesoftware.os.routing.bird.http.client.TenantRoutingHttpClientInitializer;
import com.jivesoftware.os.routing.bird.http.server.endpoints.TenantRoutingRestEndpoints;
import com.jivesoftware.os.routing.bird.server.InitializeRestfulServer;
import com.jivesoftware.os.routing.bird.server.JerseyEndpoints;
import com.jivesoftware.os.routing.bird.server.RestfulManageServer;
import com.jivesoftware.os.routing.bird.server.RestfulServer;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthEvaluator;
import com.jivesoftware.os.routing.bird.server.oauth.OAuthServiceLocatorShim;
import com.jivesoftware.os.routing.bird.server.oauth.route.RouteOAuthValidatorInitializer;
import com.jivesoftware.os.routing.bird.server.oauth.validator.AuthValidator;
import com.jivesoftware.os.routing.bird.server.session.RouteSessionValidatorInitializer;
import com.jivesoftware.os.routing.bird.server.session.SessionEvaluator;
import com.jivesoftware.os.routing.bird.server.session.SessionValidator;
import com.jivesoftware.os.routing.bird.server.util.Resource;
import com.jivesoftware.os.routing.bird.shared.AuthEvaluator;
import com.jivesoftware.os.routing.bird.shared.BoundedExecutor;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsProvider;
import com.jivesoftware.os.routing.bird.shared.MonitoredExecutorService;
import com.jivesoftware.os.routing.bird.shared.RSAKeyPairGenerator;
import com.jivesoftware.os.routing.bird.shared.TenantRoutingProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.container.ContainerRequestFilter;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.merlin.config.Config;
import org.merlin.config.ConfigProvider;

public class Deployable implements ConfigProvider {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final MainProperties mainProperties;
    private final ConfigBinder configBinder;
    private final InstanceConfig instanceConfig;
    private TenantRoutingProvider tenantRoutingProvider;
    private RestfulManageServer restfulManageServer;
    private final AtomicBoolean manageServerStarted = new AtomicBoolean(false);
    private final AtomicReference<String> keyStorePassword = new AtomicReference<>();
    private InitializeRestfulServer restfulServer;
    private JerseyEndpoints jerseyEndpoints;
    private final AtomicBoolean serverStarted = new AtomicBoolean(false);
    private final ScheduledExecutorService connectionRefresh = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat(
        "connectionRefresh-%d").build());

    public Deployable(String[] args,
        ConfigBinder configBinder,
        InstanceConfig instanceConfig,
        ConnectionDescriptorsProvider connectionsDescriptorProvider) throws Exception {

        this.mainProperties = new MainProperties(args);
        this.configBinder = configBinder;
        this.instanceConfig = instanceConfig;
        init(connectionsDescriptorProvider);
    }

    private void init(ConnectionDescriptorsProvider connectionsDescriptorProvider) throws Exception {
        if (connectionsDescriptorProvider == null) {
            TenantRoutingBirdProviderBuilder tenantRoutingBirdBuilder = new TenantRoutingBirdProviderBuilder(instanceConfig.getRoutesHost(),
                instanceConfig.getRoutesPort(), instanceConfig.getRoutesPath());
            connectionsDescriptorProvider = tenantRoutingBirdBuilder.build(null);
        }

        tenantRoutingProvider = new TenantRoutingProvider(connectionRefresh, instanceConfig.getInstanceKey(), connectionsDescriptorProvider);

        String keyStorePassword = null;
        String keyStorePath = "./certs/sslKeystore";

        if (instanceConfig.getMainSslEnabled() || instanceConfig.getManageSslEnabled()) {

            keyStorePassword = keyStorePassword(instanceConfig.getRoutesHost(), instanceConfig.getRoutesPort());
        }

        restfulManageServer = new RestfulManageServer(instanceConfig.getManageLoopback(),
            instanceConfig.getManagePort(),
            "server-manage-" + instanceConfig.getServiceName(),
            instanceConfig.getManageSslEnabled(),
            instanceConfig.getInstanceKey(),
            keyStorePassword,
            keyStorePath,
            instanceConfig.getManageMaxThreads(),
            instanceConfig.getManageMaxQueuedRequests());

        if (instanceConfig.getManageServiceAuthEnabled()) {
            DeployableManageAuthHealthCheckConfig authHealthCheckConfig = configBinder.bind(DeployableManageAuthHealthCheckConfig.class);
            PercentileHealthChecker healthChecker = new PercentileHealthChecker(authHealthCheckConfig);

            AuthValidationFilter authValidationFilter = new AuthValidationFilter(healthChecker);
            authValidationFilter.addEvaluator(new NoAuthEvaluator(), "/manage/health");
            authValidationFilter.addEvaluator(routeOAuth(), "/*");
            authValidationFilter.addEvaluator(sessionAuth(), "/*");
            authValidationFilter.dryRun(instanceConfig.getManageServiceAuthDryRun());
            restfulManageServer.addContainerRequestFilter(authValidationFilter);
            restfulManageServer.addHealthCheck(healthChecker);
        }

        restfulManageServer.addEndpoint(TenantRoutingRestEndpoints.class);
        restfulManageServer.addInjectable(TenantRoutingProvider.class, tenantRoutingProvider);
        restfulManageServer.addEndpoint(MainPropertiesEndpoints.class);
        restfulManageServer.addInjectable(MainProperties.class, mainProperties);

        jerseyEndpoints = new JerseyEndpoints();
        restfulServer = new InitializeRestfulServer(false,
            instanceConfig.getMainPort(),
            "server-" + instanceConfig.getServiceName(),
            instanceConfig.getMainSslEnabled(),
            instanceConfig.getInstanceKey(),
            keyStorePassword,
            keyStorePath,
            instanceConfig.getMainMaxThreads(),
            instanceConfig.getMainMaxQueuedRequests());
    }

    public TenantRoutingProvider getTenantRoutingProvider() {
        return tenantRoutingProvider;
    }

    public TenantRoutingHttpClientInitializer<String> getTenantRoutingHttpClientInitializer() throws Exception {
        return new TenantRoutingHttpClientInitializer<>(new OAuthSignerProvider(this::getServiceOAuthSigner));
    }

    private String keyStorePassword(String routesHost, Integer routesPort) throws Exception {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().build();
        String password = keyStorePassword.get();
        if (password == null) {
            HttpClient httpClient = new HttpClientFactoryProvider()
                .createHttpClientFactory(Collections.singletonList(httpClientConfig), false)
                .createClient(null, routesHost, routesPort);
            HttpResponse response = httpClient.get(instanceConfig.getKeyStorePasswordsPath() + "/" + instanceConfig.getInstanceKey(), null);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                password = new String(response.getResponseBody(), StandardCharsets.UTF_8);
                keyStorePassword.compareAndSet(null, password);
            } else {
                throw new Exception("Failed to access required keystore password. " + response.getStatusCode());
            }
        }
        return password;
    }

    private OAuthSigner getServiceOAuthSigner() throws Exception {
        String password = keyStorePassword(instanceConfig.getRoutesHost(), instanceConfig.getRoutesPort());
        File oauthKeystoreFile = new File("./certs/oauthKeystore");
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        String consumerKey = instanceConfig.getInstanceKey();
        String consumerSecret = generator.getPrivateKey(consumerKey, password, oauthKeystoreFile);

        String token = consumerKey;
        String tokenSecret = consumerSecret;

        return (request) -> {
            CommonsHttpOAuthConsumer oAuthConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
            oAuthConsumer.setMessageSigner(new RsaSha1MessageSigner());
            oAuthConsumer.setTokenWithSecret(token, tokenSecret);
            return oAuthConsumer.sign(request);
        };
    }

    @Override
    public <T extends Config> T config(Class<T> clazz) {
        return configBinder.bind(clazz);
    }

    public void addManageInjectables(Class clazz, Object injectable) {
        if (manageServerStarted.get()) {
            throw new IllegalStateException("Cannot add injectables after the manage server has been started.");
        }
        restfulManageServer.addInjectable(clazz, injectable);
    }

    public void addHealthCheck(HealthCheck... healthCheck) {
        restfulManageServer.addHealthCheck(healthCheck);
    }

    public void removeHealthCheck(HealthCheck... healthCheck) {
        restfulManageServer.removeHealthCheck(healthCheck);
    }

    public RestfulManageServer buildManageServer() throws Exception {
        if (manageServerStarted.compareAndSet(false, true)) {
            long time = System.currentTimeMillis();
            String applicationName = "manage " + instanceConfig.getServiceName() + " " + instanceConfig.getClusterName();
            ComponentHealthCheck healthCheck = new ComponentHealthCheck("'" + applicationName + "'service initialization");
            restfulManageServer.addHealthCheck(healthCheck);
            try {
                RestfulManageServer server = restfulManageServer.initialize();
                healthCheck.setHealthy("'" + applicationName + "' service is initialized.");
                banneredOneLiner("'" + applicationName + "' service started. Elapse:" + (System.currentTimeMillis() - time));
                server.addHealthCheck(() -> {
                    String status = "Service on port:" + instanceConfig.getManagePort() + " has"
                        + " current:" + server.getThreads()
                        + " idle:" + server.getIdleThreads()
                        + " busy:" + server.getBusyThreads()
                        + " max:" + server.getMaxThreads()
                        + " threads";
                    String description = "How many free thread are available to handle http request.";
                    String resolution = "Increase the number or threads or add more services.";
                    return new HealthCheckResponseImpl("manage>http>threadPool",
                        server.isLowOnThreads() ? 0d : 1d,
                        status, description, resolution, System.currentTimeMillis());
                });
                return restfulManageServer;
            } catch (Exception x) {
                healthCheck.setUnhealthy("Failed to initialize service '" + applicationName + "'.", x);
                throw x;
            }
        } else {
            throw new IllegalStateException("Cannot start manage server more than once.");
        }
    }

    public void addEndpoints(Class clazz) {
        if (serverStarted.get()) {
            throw new IllegalStateException("Cannot add endpoints after the server has been started.");
        }
        jerseyEndpoints.addEndpoint(clazz);
    }

    public void addInjectables(Class clazz, Object injectable) {
        if (serverStarted.get()) {
            throw new IllegalStateException("Cannot add injectables after the server has been started.");
        }
        jerseyEndpoints.addInjectable(clazz, injectable);
    }

    public void addResource(Resource resource) {
        restfulServer.addResource(resource);
    }

    private void initializeMemoryExceptionsHandler(OOMHealthCheck oomhc, UncaughtExceptionHealthCheck uehc) {
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
            if (e instanceof OutOfMemoryError) {
                oomhc.oomed.set(true);
            } else {
                LOG.error("UncaughtException", e);
                uehc.unhandled.add(t.getName() + " throwable:" + e + " cause:" + e.getCause());
            }
        });
    }

    private class UncaughtExceptionHealthCheck implements HealthCheck {

        private final List<String> unhandled = new CopyOnWriteArrayList<>();

        @Override
        public HealthCheckResponse checkHealth() throws Exception {
            return new HealthCheckResponse() {

                @Override
                public String getName() {
                    return "uncaught>exception";
                }

                @Override
                public double getHealth() {
                    return (unhandled.size() > 0) ? 0.1d : 1d;
                }

                @Override
                public String getStatus() {
                    StringBuilder sb = new StringBuilder();
                    if (!unhandled.isEmpty()) {
                        sb.append("This service has unhandled exceptions:").append(unhandled);
                    }
                    if (sb.length() == 0) {
                        sb.append("healthy");
                    }
                    return sb.toString();
                }

                @Override
                public String getDescription() {
                    return "Uncaught exception";
                }

                @Override
                public String getResolution() {
                    return "Catch you exceptions please! Restart service";
                }

                @Override
                public long getTimestamp() {
                    return System.currentTimeMillis();
                }
            };
        }

    }

    private class OOMHealthCheck implements HealthCheck {

        private final AtomicBoolean oomed = new AtomicBoolean(false);

        @Override
        public HealthCheckResponse checkHealth() throws Exception {
            return new HealthCheckResponse() {

                @Override
                public String getName() {
                    return "out>of>memory";
                }

                @Override
                public double getHealth() {
                    return oomed.get() ? 0d : 1d;
                }

                @Override
                public String getStatus() {
                    StringBuilder sb = new StringBuilder();
                    if (oomed.get()) {
                        sb.append("These service has experienced an OOM.");
                    }
                    if (sb.length() == 0) {
                        sb.append("healthy");
                    }
                    return sb.toString();
                }

                @Override
                public String getDescription() {
                    return "Service OOM";
                }

                @Override
                public String getResolution() {
                    return "Fix reason for OOM! Restart service";
                }

                @Override
                public long getTimestamp() {
                    return System.currentTimeMillis();
                }
            };
        }

    }

    private class LoggerSummaryHealthCheck implements ScheduledHealthCheck, ResettableHealthCheck {

        private final AtomicLong lastErrorCount = new AtomicLong();
        private final AtomicLong lastCheckTimestamp = new AtomicLong();
        private final AtomicLong lastErrorDelta = new AtomicLong();
        private final LoggerSummary loggerSummary;

        private final String name;
        private final long unhealthyForNMillisEveryError;
        private final double healthWhenErrorsExceeded;
        private long healthClearAfterThisTimestmap;

        public LoggerSummaryHealthCheck(LoggerSummary loggerSummary, String name, long unhealthyForNMillisEveryError, double healthWhenErrorsExceeded) {
            this.name = name;
            this.loggerSummary = loggerSummary;
            this.unhealthyForNMillisEveryError = unhealthyForNMillisEveryError;
            this.healthWhenErrorsExceeded = healthWhenErrorsExceeded;
        }

        @Override
        public void run() {
            long errors = loggerSummary.errors.longValue();
            long delta = errors - lastErrorCount.getAndSet(errors);
            lastErrorDelta.set(delta);
            if (delta > 0) {
                healthClearAfterThisTimestmap = System.currentTimeMillis() + unhealthyForNMillisEveryError;
            } else if (delta < 0) { // means errors were manually reset
                healthClearAfterThisTimestmap = 0;
            }
            lastCheckTimestamp.set(System.currentTimeMillis());
        }

        @Override
        public void reset() {
            healthClearAfterThisTimestmap = 0;
        }

        @Override
        public long getCheckIntervalInMillis() {
            return TimeUnit.SECONDS.toMillis(2);
        }

        @Override
        public HealthCheckResponse checkHealth() throws Exception {
            return new HealthCheckResponse() {

                @Override
                public String getName() {
                    return name + ">logged>errors";
                }

                @Override
                public double getHealth() {
                    long elapse = healthClearAfterThisTimestmap - System.currentTimeMillis();
                    if (elapse <= 0) {
                        return 1d;
                    }
                    return ((1d - ((double) elapse / (double) unhealthyForNMillisEveryError)) * (1d - healthWhenErrorsExceeded)) + healthWhenErrorsExceeded;
                }

                @Override
                public String getStatus() {
                    return "infos:" + loggerSummary.infos
                        + " warns:" + loggerSummary.warns
                        + " errors:" + loggerSummary.errors + " (" + lastErrorDelta.get() + ")";
                }

                @Override
                public String getDescription() {
                    String[] errors = loggerSummary.lastNErrors.get();
                    if (errors != null && errors.length > 0) {
                        for (int i = 0; i < errors.length; i++) {
                            if (errors[i] == null) {
                                errors[i] = "";
                            }
                        }
                    }
                    return "Recent Errors:\n" + Joiner.on("\n").join(Objects.firstNonNull(errors, new String[] { "" }));
                }

                @Override
                public String getResolution() {
                    return "Investigate any errors. Let errors expire or manually reset.";
                }

                @Override
                public long getTimestamp() {
                    return lastCheckTimestamp.get();
                }
            };
        }
    }

    public void addErrorHealthChecks(ErrorHealthCheckConfig config) {
        OOMHealthCheck oomHealthCheck = new OOMHealthCheck();
        UncaughtExceptionHealthCheck uehc = new UncaughtExceptionHealthCheck();
        initializeMemoryExceptionsHandler(oomHealthCheck, uehc);

        restfulManageServer.addHealthCheck(
            new LoggerSummaryHealthCheck(LoggerSummary.INSTANCE,
                "internal",
                config.getInternalUnhealthyForNMillisEveryError(),
                config.getInternalHealthWhenErrorsExceeded()),
            new LoggerSummaryHealthCheck(LoggerSummary.INSTANCE_EXTERNAL_INTERACTIONS,
                "external",
                config.getExternalUnhealthyForNMillisEveryError(),
                config.getExternalHealthWhenErrorsExceeded()),
            oomHealthCheck);
    }

    private final OAuth1Signature verifier = new OAuth1Signature(new OAuthServiceLocatorShim());
    private AuthValidationFilter authValidationFilter;

    synchronized private AuthValidationFilter authValidationFilter() {
        if (authValidationFilter == null) {
            DeployableMainAuthHealthCheckConfig dmahcc = config(DeployableMainAuthHealthCheckConfig.class);
            PercentileHealthChecker authFilterHealthCheck = new PercentileHealthChecker(dmahcc);
            addHealthCheck(authFilterHealthCheck);
            authValidationFilter = new AuthValidationFilter(authFilterHealthCheck)
                .setInstanceKey(instanceConfig.getInstanceKey());
            jerseyEndpoints.addContainerRequestFilter(authValidationFilter);
        }
        return authValidationFilter;
    }

    public Deployable addBasicAuth(Set<String> usernamesAndPasswords, String... paths) throws Exception {
        authValidationFilter().addEvaluator(basicAuth(usernamesAndPasswords), paths);
        return this;
    }

    private AuthEvaluator basicAuth(Set<String> usernamesAndPasswords) {
        return new BasicAuthEvaluator(usernamesAndPasswords);
    }

    public Deployable addRouteOAuth(String... paths) throws Exception {
        authValidationFilter().addEvaluator(routeOAuth(), paths);
        return this;
    }

    private OAuthEvaluator routeOAuth() throws Exception {
        RouteOAuthValidatorInitializer.RouteOAuthValidatorConfig routeOAuthValidatorConfig = config(
            RouteOAuthValidatorInitializer.RouteOAuthValidatorConfig.class);
        AuthValidator<OAuth1Signature, OAuth1Request> routeOAuthValidator = new RouteOAuthValidatorInitializer().initialize(routeOAuthValidatorConfig,
            instanceConfig.getRoutesHost(),
            instanceConfig.getRoutesPort(),
            "http",
            instanceConfig.getOauthValidatorPath());
        routeOAuthValidator.start();
        return new OAuthEvaluator(routeOAuthValidator, verifier);
    }

    private AuthRoutingBirdSessionFilter authRoutingBirdSessionFilter;

    public Deployable addSessionAuth(String... paths) throws Exception {
        authValidationFilter().addEvaluator(sessionAuth(), paths);

        synchronized (this) {
            if (authRoutingBirdSessionFilter == null) {
                authRoutingBirdSessionFilter = new AuthRoutingBirdSessionFilter();
                jerseyEndpoints.addContainerResponseFilter(authRoutingBirdSessionFilter);
            }
        }

        return this;
    }

    public SessionEvaluator sessionAuth() {
        RouteSessionValidatorInitializer.RouteSessionValidatorConfig sessionValidatorConfig = config(
            RouteSessionValidatorInitializer.RouteSessionValidatorConfig.class);
        SessionValidator routeSessionValidator = new RouteSessionValidatorInitializer().initialize(
            sessionValidatorConfig,
            instanceConfig.getInstanceKey(),
            instanceConfig.getRoutesHost(),
            instanceConfig.getRoutesPort(),
            "http",
            instanceConfig.getSessionValidatorPath(),
            instanceConfig.getSessionExchangePath());
        return new SessionEvaluator(routeSessionValidator);
    }

    public Deployable addCustomOAuth(AuthValidator<OAuth1Signature, OAuth1Request> customOAuthValidator, String... paths) {
        customOAuthValidator.start();
        authValidationFilter().addEvaluator(new OAuthEvaluator(customOAuthValidator, verifier), paths);
        return this;
    }

    public Deployable addNoAuth(String... paths) {
        authValidationFilter().addEvaluator(new NoAuthEvaluator(), paths);
        return this;
    }

    public RestfulServer buildServer() throws Exception {
        if (serverStarted.compareAndSet(false, true)) {
            String applicationName = instanceConfig.getServiceName() + " " + instanceConfig.getClusterName();
            ComponentHealthCheck healthCheck = new ComponentHealthCheck("'" + applicationName + "' service initialization");
            restfulManageServer.addHealthCheck(healthCheck);
            try {
                restfulServer.addContextHandler("/", jerseyEndpoints);
                final RestfulServer server = restfulServer.build();
                healthCheck.setHealthy("'" + applicationName + "' service is initialized.");
                startedUpBanner();
                restfulManageServer.addHealthCheck((HealthCheck) () -> {
                    String status = "Service on port:" + instanceConfig.getMainPort() + " has"
                        + " current:" + server.getThreads()
                        + " idle:" + server.getIdleThreads()
                        + " busy:" + server.getBusyThreads()
                        + " max:" + server.getMaxThreads()
                        + " threads";
                    String description = "How many free thread are available to handle http request.";
                    String resolution = "Increase the number or threads or add more services.";
                    return new HealthCheckResponseImpl("main>http>threadPool",
                        server.isLowOnThreads() ? 0d : 1d,
                        status, description, resolution, System.currentTimeMillis());
                });
                return server;
            } catch (Exception x) {
                healthCheck.setUnhealthy("Failed to initialize service '" + applicationName + "'.", x);
                throw x;
            }
        } else {
            throw new IllegalStateException("Cannot start server more than once.");
        }
    }

    public ExecutorService newBoundedExecutor(int maxThreads, String name) {
        MonitoredExecutorService executorService = BoundedExecutor.newBoundedExecutor(maxThreads, name);
        addHealthCheck(new MonitoredExecutorServiceHealthCheck(name, executorService));
        return executorService;
    }

    void banneredOneLiner(String message) {
        LOG.info(pad("-", "", "-", '-', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("|", "      " + message, "|", ' ', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("-", "", "-", '-', 100));
    }

    void startedUpBanner() {
        LOG.info(pad("-", "", "-", '-', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("|", "      Service INSTANCEKEY:" + instanceConfig.getInstanceKey(), "|", ' ', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("|", "      Service CLUSTER:" + instanceConfig.getClusterName(), "|", ' ', 100));
        LOG.info(pad("|", "         Service HOST:" + instanceConfig.getHost(), "|", ' ', 100));
        LOG.info(pad("|", "      Service SERVICE:" + instanceConfig.getServiceName(), "|", ' ', 100));
        LOG.info(pad("|", "     Service INSTANCE:" + instanceConfig.getInstanceName(), "|", ' ', 100));
        LOG.info(pad("|", "         Primary PORT:" + instanceConfig.getMainPort(), "|", ' ', 100));
        LOG.info(pad("|", "          Manage PORT:" + instanceConfig.getManagePort(), "|", ' ', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("|", "        curl " + instanceConfig.getHost() + ":" + instanceConfig.getManagePort() + "/manage/help", "|", ' ', 100));
        LOG.info(pad("|", "", "|", ' ', 100));
        LOG.info(pad("-", "", "-", '-', 100));
    }

    String pad(String prefic, String string, String postFix, char pad, int totalLength) {
        if (string.length() >= totalLength) {
            return string;
        }
        char[] padding = new char[totalLength - string.length()];
        Arrays.fill(padding, pad);
        return prefic + string + new String(padding) + postFix;
    }

}
