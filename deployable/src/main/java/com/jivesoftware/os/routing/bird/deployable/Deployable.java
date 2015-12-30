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
import com.jivesoftware.os.mlogger.core.LoggerSummary;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.deployable.config.extractor.ConfigBinder;
import com.jivesoftware.os.routing.bird.deployable.reporter.service.StatusReportBroadcaster;
import com.jivesoftware.os.routing.bird.deployable.reporter.service.StatusReportBroadcaster.StatusReportCallback;
import com.jivesoftware.os.routing.bird.deployable.reporter.service.StatusReportConfig;
import com.jivesoftware.os.routing.bird.endpoints.configuration.MainProperties;
import com.jivesoftware.os.routing.bird.endpoints.configuration.MainPropertiesEndpoints;
import com.jivesoftware.os.routing.bird.health.HealthCheck;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponse;
import com.jivesoftware.os.routing.bird.health.HealthCheckResponseImpl;
import com.jivesoftware.os.routing.bird.http.server.endpoints.TenantRoutingRestEndpoints;
import com.jivesoftware.os.routing.bird.server.InitializeRestfulServer;
import com.jivesoftware.os.routing.bird.server.JerseyEndpoints;
import com.jivesoftware.os.routing.bird.server.RestfulManageServer;
import com.jivesoftware.os.routing.bird.server.RestfulServer;
import com.jivesoftware.os.routing.bird.server.util.Resource;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsProvider;
import com.jivesoftware.os.routing.bird.shared.TenantRoutingProvider;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.glassfish.hk2.api.ServiceHandle;
import org.merlin.config.Config;

public class Deployable {

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final MainProperties mainProperties;
    private final ConfigBinder configBinder;
    private final InstanceConfig instanceConfig;
    private TenantRoutingProvider tenantRoutingProvider;
    private RestfulManageServer restfulManageServer;
    private final AtomicBoolean manageServerStarted = new AtomicBoolean(false);
    //--------------------------------------------------------------------------
    private InitializeRestfulServer restfulServer;
    private JerseyEndpoints jerseyEndpoints;
    private final AtomicBoolean serverStarted = new AtomicBoolean(false);

    public Deployable(String[] args) throws IOException {
        this.mainProperties = new MainProperties(args);
        this.configBinder = new ConfigBinder(args);
        this.instanceConfig = configBinder.bind(InstanceConfig.class);
        init(null);
    }

    public Deployable(String[] args, ConfigBinder configBinder, InstanceConfig instanceConfig, ConnectionDescriptorsProvider connectionsDescriptorProvider) {
        this.mainProperties = new MainProperties(args);
        this.configBinder = configBinder;
        this.instanceConfig = instanceConfig;
        init(connectionsDescriptorProvider);
    }

    private void init(ConnectionDescriptorsProvider connectionsDescriptorProvider) {

        if (connectionsDescriptorProvider == null) {

            TenantRoutingBirdProviderBuilder tenantRoutingBirdBuilder = new TenantRoutingBirdProviderBuilder(instanceConfig.getRoutesHost(),
                instanceConfig.getRoutesPort(), instanceConfig.getRoutesPath());
            connectionsDescriptorProvider = tenantRoutingBirdBuilder.build();
        }

        tenantRoutingProvider = new TenantRoutingProvider(instanceConfig.getInstanceKey(), connectionsDescriptorProvider);

        String applicationName = "manage " + instanceConfig.getServiceName() + " " + instanceConfig.getClusterName();
        restfulManageServer = new RestfulManageServer(instanceConfig.getManagePort(),
            applicationName,
            instanceConfig.getManageMaxThreads(),
            instanceConfig.getManageMaxQueuedRequests());

        restfulManageServer.addEndpoint(TenantRoutingRestEndpoints.class);
        restfulManageServer.addInjectable(TenantRoutingProvider.class, tenantRoutingProvider);
        restfulManageServer.addEndpoint(MainPropertiesEndpoints.class);
        restfulManageServer.addInjectable(MainProperties.class, mainProperties);

        jerseyEndpoints = new JerseyEndpoints();
        restfulServer = new InitializeRestfulServer(instanceConfig.getMainPort(),
            applicationName,
            instanceConfig.getMainMaxThreads(),
            instanceConfig.getMainMaxQueuedRequests());
    }

    /**
     Needs to be called before buildManageServer().
     @param statusReportCallback
     @return
     */
    public StatusReportBroadcaster buildStatusReporter(StatusReportCallback statusReportCallback) {

        StatusReportConfig config = configBinder.bind(StatusReportConfig.class);

        StatusReportBroadcaster broadcaster = new StatusReportBroadcaster(
            config.getAnnouceEveryNMills(),
            statusReportCallback);

        restfulManageServer.addEndpoint(StatusReportEndpoints.class);
        restfulManageServer.addInjectable(StatusReportBroadcaster.class, broadcaster);
        return broadcaster;
    }

    public ServiceHandle buildMetricPublisher() {
        return null;
    }

    public ServiceHandle buildLogPublisher() {
        return null;
    }

    public TenantRoutingProvider getTenantRoutingProvider() {
        return tenantRoutingProvider;
    }

    public <T extends Config> T config(Class<T> clazz) {
        return configBinder.bind(clazz);
    }

    public void addManageEndpoints(Class clazz) {
        if (manageServerStarted.get()) {
            throw new IllegalStateException("Cannot add endpoints after the manage server has been started.");
        }
        restfulManageServer.addEndpoint(clazz);
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
                server.addHealthCheck((HealthCheck) () -> {
                    String status = "Currently this service on port:"
                        + instanceConfig.getManagePort() + " has " + server.getIdleThreads() + " idle thread/s.";
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

    //--------------------------------------------------------------------------
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

    private void initializeMemoryExceptionsHandler(OOMHealthCheck oomhc) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof OutOfMemoryError) {
                    oomhc.oomed.set(true);
                } else {
                    LOG.error("UncaughtException", e);
                    oomhc.unhandled.add(t.getName() + " throwable:" + e + " cause:" + e.getCause());
                }
            }
        });
    }

    private class OOMHealthCheck implements HealthCheck {

        private final AtomicBoolean oomed = new AtomicBoolean(false);
        private final List<String> unhandled = new CopyOnWriteArrayList<>();

        @Override
        public HealthCheckResponse checkHealth() throws Exception {
            return new HealthCheckResponse() {

                @Override
                public String getName() {
                    return "out>of>memory";
                }

                @Override
                public double getHealth() {
                    return oomed.get() ? 0d : (unhandled.size() > 0) ? 0.1d : 1d;
                }

                @Override
                public String getStatus() {
                    StringBuilder sb = new StringBuilder();
                    if (oomed.get()) {
                        sb.append("These service has experienced an OOM.");
                    }
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

    private class LoggerSummaryHealthCheck implements HealthCheck {

        private final AtomicLong lastErrorCount = new AtomicLong();
        private final AtomicLong lastCheckTimestamp = new AtomicLong();
        private final AtomicLong lastErrorDelta = new AtomicLong();
        private final LoggerSummary loggerSummary;

        public LoggerSummaryHealthCheck(LoggerSummary loggerSummary) {
            this.loggerSummary = loggerSummary;
        }

        @Override
        public HealthCheckResponse checkHealth() throws Exception {
            return new HealthCheckResponse() {

                @Override
                public String getName() {
                    return "logged>errors";
                }

                @Override
                public double getHealth() {
                    long errors = loggerSummary.errors;
                    long delta = errors - lastErrorCount.getAndSet(errors);
                    lastErrorDelta.set(delta);
                    long now = System.currentTimeMillis();
                    long elapse = now - lastCheckTimestamp.getAndSet(now);
                    if (elapse > 0 && delta > 0) {
                        double errorRatePerMilli = (double) delta / (double) elapse;
                        double errorsRatePerMinute = errorRatePerMilli * (60 * 1000);
                        if (errorsRatePerMinute < 10) {
                            return (1d - Math.log10(errorsRatePerMinute));
                        } else {
                            return Double.MIN_VALUE;
                        }
                    }
                    return 1d;
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
                    return "Recent Errors:\n" + Joiner.on("\n").join(Objects.firstNonNull(errors, new String[]{""}));
                }

                @Override
                public String getResolution() {
                    return "Look confirn errors are benign. Click/Curl/Hit to clear reset. http://" + instanceConfig.getHost() + ":" + instanceConfig
                        .getManagePort() + "/manage/resetErrors";
                }

                @Override
                public long getTimestamp() {
                    return System.currentTimeMillis();
                }
            };
        }

    }

    public void addErrorHealthChecks() {
        OOMHealthCheck oomHealthCheck = new OOMHealthCheck();
        initializeMemoryExceptionsHandler(oomHealthCheck);

        restfulManageServer.addHealthCheck(new LoggerSummaryHealthCheck(LoggerSummary.INSTANCE),
            new LoggerSummaryHealthCheck(LoggerSummary.INSTANCE_EXTERNAL_INTERACTIONS),
            oomHealthCheck);
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
                    String status = "Currently this service on port:"
                        + instanceConfig.getMainPort() + " has " + server.getIdleThreads() + " idle thread/s.";
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
